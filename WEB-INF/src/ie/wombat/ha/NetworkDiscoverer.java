package ie.wombat.ha;

import ie.wombat.zigbee.NodeDescriptor;
import ie.wombat.zigbee.RoutingTableEntry;
import ie.wombat.zigbee.address.Address16;
import ie.wombat.zigbee.address.Address64;
import ie.wombat.zigbee.zdo.NeighborTableEntry;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import org.apache.log4j.Logger;

/**
 * Services related to finding devices on the network. Object should be used 
 * just once. Subsequent discovery requests should be done on a new object.
 * 
 * @author joe
 *
 */
public class NetworkDiscoverer implements ZigBeePacketListener {

	
	private static Logger log = Logger.getLogger(NetworkDiscoverer.class);
	
	private ZigBeeNIC driver;
	
	
	private int seqId = 0x66;

	private List<NeighborTableEntry> neighborTable = new ArrayList<NeighborTableEntry>();
	private List<RoutingTableEntry> routingTable = new ArrayList<RoutingTableEntry>();
	private NodeDescriptor nodeDescriptor = null;
	private List<Integer> endPoints;
	
	private NeighborTableResponse neighborTableResponse = null;
	private NeighborTableGetStatus neighborTableGetStatus = null;
	
	private int index = 0;
	
	public NetworkDiscoverer (ZigBeeNIC driver) {
		
		this.driver = driver;
		driver.addZigBeePacketListener(this);
	}
	
	public Collection<RoutingTableEntry> testing () throws IOException {
		
		getRoutingTableFromDevice (Address16.COORDINATOR);
		//getRoutingTableFromDevice (new Address16("d340"));
		return routingTable;
	}
	
	/**
	 * Recursively explore network looking for all devices.
	 * 
	 * @return
	 * @throws IOException
	 */
	public synchronized Collection<NeighborTableEntry> getAllDevices2 () throws IOException {
		neighborTable.clear();
		int frameId = sendNeighborTableRequest16(Address16.BROADCAST_TO_ALL,index);
		//int frameId = sendNeighborTableRequest(Address16.COORDINATOR,index);
		System.err.println ("xBeeAPIframeId=" + frameId);
		try {
			wait();
		} catch (InterruptedException e) {
			// ignore
		}
		System.err.println ("getAllDevices() WAKE!!");
		// Remove duplicates
		HashMap<Address16,NeighborTableEntry>h = new HashMap<Address16,NeighborTableEntry>();
		for (NeighborTableEntry nte : neighborTable) {
			h.put(nte.addr16,nte);
		}
		return h.values();
	}
	
	
	public Collection<NeighborTableEntry> getAllDevices () throws IOException {
	
		HashMap<Address16,NeighborTableEntry> deviceHash = new HashMap<Address16,NeighborTableEntry>();
		HashMap<Address16,NeighborTableEntry> exploredRoutersHash = new HashMap<Address16,NeighborTableEntry>();
		// Dummy NTE to start the recursion
		NeighborTableEntry coordNte = new NeighborTableEntry();
		coordNte.addr64=Address64.COORDINATOR;
		coordNte.addr16=Address16.COORDINATOR;
		getAllDevsRecursive64(coordNte,deviceHash,exploredRoutersHash,0);
		return deviceHash.values();
	}
	
	public synchronized NodeDescriptor getNodeDescriptor (Address16 addr16) throws IOException {
		nodeDescriptor = null;
		sendNodeDescriptorRequest(addr16);
		while (nodeDescriptor == null) {
			try {
				wait();
			} catch (InterruptedException e) {
				// ignore
			}
			System.err.println ("WAKE!!");
		}
		return nodeDescriptor;
	}
	
	private void getAllDevsRecursive64 (NeighborTableEntry router,
					HashMap<Address16,NeighborTableEntry> deviceHash,
					HashMap<Address16,NeighborTableEntry> exploredRoutersHash,
					int level) throws IOException {
		
		log.debug("getAllDevsRecursive64(): level=" + level
				+ " Router addr64=" + router.addr64 + " addr16=" + router.addr16);
		
		if (exploredRoutersHash.containsKey(router.addr16)) {
			log.debug("HEY, ALREADY DONE ROUTER AT " + router.addr16 + ". Returning.");
			return;
		}
		
		// Avoid infinite loops by limiting levels of recursion
		if (level > 4) {
			throw new IOException ("Levels of recursion exceeded");
		}
		
		List<NeighborTableEntry> devices = getNeighborTableFromDevice6416(router.addr64,router.addr16);
		
		// Check off this router as being done
		exploredRoutersHash.put(router.addr16, router);
		
		List<NeighborTableEntry> unexporedRouters = new ArrayList<NeighborTableEntry>();
		for (NeighborTableEntry nte : devices) {
			if (!deviceHash.containsKey(nte.addr16)) {
				// This is new device, add to devices
				deviceHash.put(nte.addr16, nte);
			}
			
			// If router then explore
			if (nte.isRouter() && !exploredRoutersHash.containsKey(nte.addr16)) {
				unexporedRouters.add(nte);
			} 
		}
		
		int i = 0;
		StringBuffer buf = new StringBuffer();
		for (NeighborTableEntry nte : unexporedRouters) {
			buf.append (" " + nte.addr16);
		}
		log.debug("level=" + level + ". Found " + unexporedRouters.size() + " unexplored routers: " + buf.toString());
		for (NeighborTableEntry nte : unexporedRouters) {
			log.debug ("level=" + level + " next unexplored router at this level: " + nte.addr16);
			log.debug("calling getAllDevsRecursive64() iteration #" + i++);
			getAllDevsRecursive64(nte, deviceHash, exploredRoutersHash, level+1);
		}
		log.debug ("level=" + level + ". Finished recursing at this level.");
	}
	
	public synchronized List<NeighborTableEntry> getNeighborTableFromDevice6416(Address64 addr64, Address16 addr16) throws IOException {
		log.debug ("getNeighborTableFromDevice6416(): addr=" + addr64 + " addr16=" + addr16);
		
		// Rely on the handleZigBeePacket() call back handler to populate
		// the table. Clear the neighborTable before polling. 
		neighborTable.clear();
		neighborTableGetStatus = null;
		index = 0;
		
		while (neighborTableGetStatus == null || index < neighborTableGetStatus.nEntriesTotal ) {
			log.debug ("getNeighborTableFromDevice6416(): requesting neighbor table starting at index=" + index);
			neighborTableResponse = null;
			sendNeighborTableRequest6416(addr64,addr16,index);
			while (neighborTableResponse == null) {
				log.debug ("getNeighborTableFromDevice6416(): SLEEP index="+index );
				try {
					wait();
				} catch (InterruptedException e) {
				}
				log.debug ("getNeighborTableFromDevice6416(): WAKE index="+index);
			}
			// Will assume for the moment that the sequenceId is correct
			log.debug ("getNeighborTableFromDevice6416(): got a response! seq=0x" 
					+ Integer.toHexString(neighborTableResponse.sequenceId));
		}
		
		log.debug ("getNeighborTableFromDevice6416(): Complete for addr=" + addr64 
				+ ". Found " + neighborTable.size() + " devices.");
		for (NeighborTableEntry nte : neighborTable) {
			log.debug ("    " + nte.toString());
		}
		log.debug ("getNeighborTableFromDevice6416(): RETURN.");
		return neighborTable;
	}
	
	public synchronized List<RoutingTableEntry> getRoutingTableFromDevice(Address16 addr16) throws IOException {
		// Rely on the handleZigBeePacket() call back handler to populate
		// the table. Clear the neighborTable before polling. 
		routingTable.clear();
		/*
		while (index < nDevices) {
			sendRoutingTableRequest(addr16,index);
			try {
				wait();
			} catch (InterruptedException e) {
				// ignore
			}
		}
		*/
		return routingTable;
	}
	
	public synchronized List<Integer> getActiveEndPointsFromDevice(Address16 addr16) throws IOException {
		log.debug ("getActiveEndPointsFromDevice() addr16=" + addr16);
		endPoints = null;
		sendActiveEndPointRequest(addr16);
		while (endPoints == null) {
			try {
				wait();
			} catch (InterruptedException e) {
			}
		}
		return endPoints;
	}

	private void sendNodeDescriptorRequest (Address16 addr16) throws IOException {
		byte[] command = new byte[1];
		//command[0] = 0x66; // Sequence number
		command[0] = (byte)getSeqId();
		driver.sendZigBeeCommand(
				addr16,	// The address to which to send the query 
				0x0002,  // Node descriptor request 
				0x0000,  // ZDP profile 
				0x00, // source ep 
				0x00, // dest ep (ZDO) 
				command);
	}
	
	
	private int sendNeighborTableRequest16 (Address16 addr16, int index) throws IOException {
		byte[] command = new byte[2];
		int seqId = getSeqId();
		log.debug("sendNeighborTableRequest16(): addr16=" + addr16 + " index=" + index 
				+ " seq=0x" + Integer.toHexString(seqId));
		//command[0] = 0x66; // Sequence number
		command[0] = (byte)seqId;
		command[1] = (byte)index; // start index
		return driver.sendZigBeeCommand(
				addr16,	// The address to which to send the query 
				0x0031,  // LQI request 
				0x0000,  // ZDP profile 
				0x00, // source ep 
				0x00, // dest ep (ZDO) 
				command);
	}
	private int sendNeighborTableRequest64 (Address64 addr64, int index) throws IOException {
		byte[] command = new byte[2];
		int seqId = getSeqId();
		log.debug("sendNeighborTableRequest64(): addr64=" + addr64 + " index=" + index 
				+ " seq=0x" + Integer.toHexString(seqId)
				);
		command[0] = (byte)seqId;
		command[1] = (byte)index; // start index
		return driver.sendZigBeeCommand(
				addr64,	// The address to which to send the query 
				0x0031,  // LQI request 
				0x0000,  // ZDP profile 
				0x00, // source ep 
				0x00, // dest ep (ZDO) 
				command);
	}
	private int sendNeighborTableRequest6416 (Address64 addr64, Address16 addr16, int index) throws IOException {
		byte[] command = new byte[2];
		int seqId = getSeqId();
		log.debug("sendNeighborTableRequest6416(): addr64=" + addr64 
				+ " addr16=" + addr16
				+ " index=" + index 
				+ " seq=0x" + Integer.toHexString(seqId));
		//command[0] = 0x66; // Sequence number
		command[0] = (byte)seqId;
		command[1] = (byte)index; // start index
		return driver.sendZigBeeCommand(
				addr64,	// The address to which to send the query 
				addr16,
				0x0031,  // LQI request 
				0x0000,  // ZDP profile 
				0x00, // source ep 
				0x00, // dest ep (ZDO) 
				command);
	}
	private void sendRoutingTableRequest (Address16 addr16, int index) throws IOException {
		byte[] command = new byte[2];
		command[0] = (byte)getSeqId();
		command[1] = (byte)index; // start index
		driver.sendZigBeeCommand(
				addr16,	// The address to which to send the query 
				0x0032,  // Neighbor table request 
				0x0000,  // ZDP profile 
				0x00, // source ep 
				0x00, // dest ep (ZDO) 
				command);
	}
	
	private void sendActiveEndPointRequest (Address16 addr16) throws IOException {
		byte[] command = new byte[3];
		command[0] = (byte)getSeqId();
		// The address of interest
		command[1] = addr16.addr[1];
		command[2] = addr16.addr[0];
		driver.sendZigBeeCommand(
				Address16.COORDINATOR,	// The address to which to send the query 
				0x0005,  // Active end point request
				0x0000,  // ZDP profile 
				0x00, // source ep 
				0x00, // dest ep (ZDO) 
				command);
	}
	

	public synchronized void handleAcknowledgement(int frameId, int status) {
		log.debug (this + " ACK received, frameId=" + frameId + " status=" + status + ". Sending notifyAll().");
		notifyAll();
	}
	
	public synchronized void handleZigBeePacket(ZigBeePacket zbpacket) {
		
		log.debug ("handleZigBeePacket()");
		log.debug ("  addr64=" + zbpacket.getSourceAddress64().toString());
		log.debug ("  addr16=" + zbpacket.getSourceAddress16().toString());
		log.debug ("  srcEp=0x" + Integer.toHexString(zbpacket.getSourceEndPoint()));
		log.debug ("  dstEp=0x" + Integer.toHexString(zbpacket.getDestinationEndPoint()));
		log.debug ("  profileId=0x" + Integer.toHexString(zbpacket.getProfileId()));
		log.debug ("  clusterId=0x" + Integer.toHexString(zbpacket.getClusterId()));

		byte[] packet = zbpacket.getPayload();

		// Status codes:
		/*
		  SUCCESS = 0x00
		    INV_REQUESTTYPE = 0x80
		    DEVICE_NOT_FOUND = 0x81
		    INVALID_EP = 0x82
		    NOT_ACTIVE = 0x83
		    NOT_SUPPORTED = 0x84
		    TIMEOUT = 0x85
		    NO_MATCH = 0x86
		    NO_ENTRY = 0x88
		    NO_DESCRIPTOR = 0x89
		    INSUFFICIENT_SPACE = 0x8a
		    NOT_PERMITTED = 0x8b
		    TABLE_FULL = 0x8c
		    NOT_AUTHORIZED = 0x8d
		  */
		    
		// Node descriptor response
		if (zbpacket.getClusterId() == 0x8002) {
			System.err.println ("seq=0x" + Integer.toHexString(packet[0]));

			System.err.println ("status=" + packet[1]);
			Address16 addr16 = new Address16 (packet,2,true);

			System.err.println ("addr16=" + addr16);
			//nodeDescriptor.nodeType
			nodeDescriptor = new NodeDescriptor();
			nodeDescriptor.nodeType=packet[4] & 0x07;
			nodeDescriptor.complexDescriptorAvailable = ((packet[4] & 0x08) !=0);
		}
		if (zbpacket.getClusterId() == 0x8005) {
			System.err.println ("Active End Point Response");
			System.err.println ("seq=0x" + Integer.toHexString(packet[0]));

			int status = packet[1] & 0xff;
			System.err.println ("status=0x" + Integer.toHexString(status));

			endPoints = new ArrayList<Integer>();
			if (status == 0) {
				Address16 addr16 = new Address16 (packet,2,true);
				//System.err.println ("addr16=" + addr16);
			
				int nEp = packet[4];
				int ep;
				//System.err.println ("nEp=" + nEp);
				for (int i = 0; i < nEp; i++) {
					ep = packet[5+i] & 0xff;
					//System.err.println ("  ep=" + ep);
					endPoints.add(ep);
				}
			}
		}
		
		// Neighbor table response
		if (zbpacket.getClusterId() == 0x8031) {
			log.debug ("handleZigBeePacket(): clusterId=0x8031 (Neighbor Table Response)");
			log.debug ("handleZigBeePacket(): seq=0x" + Integer.toHexString(packet[0]));
			int start = 1;
			
			
			neighborTableResponse = new NeighborTableResponse();
			neighborTableResponse.sequenceId = (int)packet[0] & 0xff;
			neighborTableResponse.sourceAddr16 = zbpacket.getSourceAddress16();
			neighborTableResponse.sourceAddr64 = zbpacket.getSourceAddress64();
			neighborTableResponse.nEntriesTotal = (int)packet[start+1] & 0xff;
			
			if (neighborTableGetStatus == null) {
				neighborTableGetStatus = new NeighborTableGetStatus();
				neighborTableGetStatus.nEntriesTotal = neighborTableResponse.nEntriesTotal;
			}
			
			
			log.debug ("  ntr.nDevicesTotal=" + neighborTableResponse.nEntriesTotal);
			
			neighborTableResponse.entryTableIndex = (int)packet[start+2] & 0xff;
			log.debug ("  offset=" + neighborTableResponse.entryTableIndex);
			
			neighborTableResponse.nEntriesThisPacket = (int)packet[start+3] & 0xff;
			log.debug ("  entriesThisPacket=" + neighborTableResponse.nEntriesThisPacket);

			int i,nteOffset;
			for (i = 0; i < neighborTableResponse.nEntriesThisPacket; i++) {
				
				// Entry offset into packet
				nteOffset = start+4+i*22;
				
				// Addresses in ZigBee packets as LSB first
				boolean lsbFirst = true;
				
				NeighborTableEntry nte = new NeighborTableEntry();
				nte.panId = new Address64(packet,nteOffset,lsbFirst);
				nte.addr64 = new Address64(packet,nteOffset+8,lsbFirst);
				nte.addr16 = new Address16(packet,nteOffset+16,lsbFirst);
				nte.deviceType = (int)packet[nteOffset+18] & 0x03;
				nte.rxOnWhenIdle = ((int)packet[nteOffset+18] >> 2) & 0x03;

				nte.relationshipType = ((int)packet[nteOffset+18] >> 4) & 0x07;
				nte.joinPermitted = ((int)packet[nteOffset+19]) & 0x03;
				nte.depth = (int)packet[nteOffset+20] & 0xff;
				nte.lqi=(int)packet[nteOffset+21] & 0xff;
				neighborTable.add(nte);
				//networkDiscoveryDeviceFound(rtr);
			}
			index = neighborTableResponse.entryTableIndex + neighborTableResponse.nEntriesThisPacket;
		
		}
		
		// Routing table response
		if (zbpacket.getClusterId() == 0x8032) {
		
			int start = 1;
			
			// TODO
			int nDevices = packet[start+1];
			
			int offset = packet[start+2];
			int entriesThisPacket = packet[start+3];
			
			int i,rteOffset;
			for (i = 0; i < entriesThisPacket; i++) {
				
				// Entry offset into packet
				rteOffset = start+4+i*5;
				
				// Addresses in ZigBee packets as LSB first
				boolean lsbFirst = true;
				RoutingTableEntry nte = new RoutingTableEntry();
				nte.addr16 = new Address16(packet,rteOffset+0,lsbFirst);
				nte.status = (int)packet[rteOffset+2] & 0x07;
				nte.memoryConstrained = ((int)packet[rteOffset+2] & 0x08) != 0;
				nte.manyToOne = ((int)packet[rteOffset+2] & 0x10) != 0;
				nte.routeRecordRequired = ((int)packet[rteOffset+2] & 0x20) != 0;

				
				routingTable.add(nte);
				//networkDiscoveryDeviceFound(rtr);
			}
			index = offset + i;
			
			log.debug ("  index=" + index);
		}
		
		log.debug ("handleZigBeePacket(): notifyAll()");
		notifyAll();
		
		log.debug ("handleZigBeePacket(): return");
		
	}
	
	private synchronized int getSeqId () {
		return ++seqId;
	}
	
	/**
	 * Structure to hold the response packet to a Neighbor Table Request
	 * 
	 * @author joe
	 *
	 */
	private static class NeighborTableResponse {
		public int sequenceId;
		public Address64 sourceAddr64;
		public Address16 sourceAddr16;
		public int nEntriesTotal;
		public int nEntriesThisPacket;
		public int entryTableIndex;
	}
	
	/**
	 * Track progress in Neighbor Table Request
	 * 
	 * @author joe
	 *
	 */
	private static class NeighborTableGetStatus {
		public int nEntriesTotal;
	}

	public Date getExpiryTime() {
		// TODO Auto-generated method stub
		return null;
	}

	public void setExpiryTime(Date expire) {
		// TODO Auto-generated method stub
		
	}
}
