package ie.wombat.ha.ui.server;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import javax.persistence.EntityManager;

import org.apache.log4j.Logger;

import ie.wombat.ha.ByteFormatUtils;
import ie.wombat.ha.HANetwork;
import ie.wombat.ha.HibernateUtil;
import ie.wombat.ha.ZigBeeNIC;
import ie.wombat.ha.ZigBeePacket;
import ie.wombat.ha.ZigBeePacketFilter;
import ie.wombat.ha.nic.xbee.XBeeDriver;
import ie.wombat.ha.nic.xbee.XBeeUtil;
import ie.wombat.ha.nic.zstack.ZStackDriver;

import ie.wombat.ha.server.Device;
import ie.wombat.ha.server.LogRecord;
import ie.wombat.ha.ui.client.AddressService;


import ie.wombat.zigbee.ZDPRequest;
import ie.wombat.zigbee.ZDPResponseListener;

import ie.wombat.zigbee.address.Address16;
import ie.wombat.zigbee.address.Address64;
import ie.wombat.zigbee.zcl.Cluster;

import ie.wombat.zigbee.zdo.Addr16Response;
import ie.wombat.zigbee.zdo.Addr64Response;

import ie.wombat.zigbee.zdo.ZDOResponse;
import ie.wombat.zigbee.zdo.ZDPStatus;
import ie.wombat.zigbee.zdo.ZDPUtil;

import com.google.gwt.user.server.rpc.RemoteServiceServlet;

/**
 * The server side implementation of {@link AddressService}.
 * 
 * This poses some challenges. ZDO (it seems) does not use sequence 
 * numbers. And many ZDO requests are sent to coordinator on EP 0.
 * So can't use the usual information in packet headers to match
 * responses to requests. Therefore need to deep-inspect packets
 * to match responses to requests.
 * 
 * @author Joe Desbonnet, jdesbonnet@gmail.com
 */
@SuppressWarnings("serial")
public class AddressServiceImpl extends RemoteServiceServlet implements
		AddressService,  ZDPResponseListener {

	private static final Logger log = Logger.getLogger(AddressServiceImpl.class);
	
	private static final int TIMEOUT = 25000;
	
	// TODO: make thread safe
	private HashMap<Address64,Addr16Response> addr64ToAddr16ResponseHash = new HashMap<Address64,Addr16Response>();
	private HashMap<Address16,Addr64Response> addr16ToAddr64ResponseHash = new HashMap<Address16,Addr64Response>();
	private HashMap<Integer,ZDOResponse> seqHash = new HashMap<Integer,ZDOResponse>();
	
	public AddressServiceImpl () {
		log.info("Creating instance of RPC servlet " + this);
	}

	/**
	 * Experimental method to resolve ADDR16 to ADDR64
	 * 
	 */
	@Deprecated
	public String resolveAddr16ToAddr64 (Long networkId, String addr16str) {
		log.info ("resolveAddr16ToAddr64(" + addr16str + ")");
		
		// 0000 is always the coordinator
		if ("0000".equals(addr16str)) {
			return Address64.COORDINATOR.toString();
		}
		
		
		ZigBeeNIC nic = HANetwork.getInstance(networkId).getNIC();
		
		if ( ! ( nic instanceof ZStackDriver)) {
			log.error("resolveAddr16ToAddr64 not supported for NIC type " + nic.getClass());
			return null;
		}
		
		ZStackDriver zstack = (ZStackDriver)nic;

		final Address16 addr16 = new Address16(addr16str);
		final byte[] addr16bytes = addr16.getBytesLSBF();
		
		// Experiment for ZStack NIC. Send UTIL_ADDRMGR_NWK_ADDR_LOOKUP
		byte[] frame = new byte[7];
		frame[0] = (byte)0xfe; //SOF
		frame[1] = 2; // length
		frame[2] = 0x27; // CMD0
		frame[3] = 0x41; // CMD1
		frame[4] = addr16bytes[0];
		frame[5] = addr16bytes[1];
		frame[6] = (byte)(frame[1] ^ frame[2] ^ frame[3] ^ frame[4] ^ frame[5]);
		try {
			nic.sendAPIFrame(frame, frame.length);
		} catch (IOException e1) {
			e1.printStackTrace();
		}

		// wait
		
		return null;
		
	}
	
	/**
	 * Experimental method to resolve ADDR64 to ADDR16
	 * 
	 */
	@Deprecated
	public String resolveAddr64ToAddr16 (Long networkId, String addr64str) {
		log.info ("resolveAddr16ToAddr64(" + addr64str + ")");
		
		// 00:...:00 is always the coordinator
		if (addr64str.equals(Address64.COORDINATOR.toString())) {
			return Address16.COORDINATOR.toString();
		}
		
		ZigBeeNIC nic = HANetwork.getInstance(networkId).getNIC();
		
		if ( ! ( nic instanceof ZStackDriver)) {
			log.error("resolveAddr16ToAddr64 not supported for NIC type " + nic.getClass());
			return null;
		}
		
		//ZStackDriver zstack = (ZStackDriver)nic;

		final Address64 addr64 = new Address64(addr64str);
		final byte[] addr64bytes = addr64.getBytesLSBF();
		
		int i;
		
		// Experiment for ZStack NIC. Send UTIL_ADDRMGR_NWK_ADDR_LOOKUP
		byte[] frame = new byte[15];
		frame[0] = (byte)0xfe; //SOF
		frame[1] = 2; // length
		frame[2] = 0x27; // CMD0
		frame[3] = 0x40; // CMD1
		for (i = 4; i < 12; i++) {
			frame[i] = addr64bytes[i-4];
		}
		frame[12] = 0x00;
		frame[13] = 0x00;
		frame[14] = (byte)ZStackDriver.calculateCheckum(frame, 1, 13);
		
		try {
			nic.sendAPIFrame(frame, frame.length);
		} catch (IOException e1) {
			e1.printStackTrace();
		}

		// wait
		
		return null;
		
	}
	
	
	

	/**
	 * Get 64 bit IEEE address from 16 bit address. Uses ZDO Cluster ID 0x0001
	 * Ref ZigBee Specification, 2.4.3.1.2 IEEE_addr_req, page 101.
	 * 
	 */
	
	public String getDeviceAddr64(Long networkId, String addr16str) {
		log.info ("getDeviceAddr64(" + addr16str + ")");
		
	
		ZigBeeNIC nic = HANetwork.getInstance(networkId).getNIC();

		byte[] command = new byte[4];
		
		// First two octets is 16 bit network address (LSB first)
		final Address16 addr16 = new Address16(addr16str);
		final byte[] addr16bytes = addr16.getBytesLSBF();
		System.arraycopy(addr16bytes, 0, command, 0, 2);
			
		// Cannot get the usual ZDP/ZDO cluster 0x0001 thing working
		// reliably. Augment with TI function if using zstack.
		if (nic instanceof ZStackDriver) {
			ZStackDriver zstack = (ZStackDriver)nic;
			try {
				zstack.UTIL_ADDRMGR_NWK_ADDR_LOOKUP (addr16);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		
/*
		// Experiment for ZStack NIC. Send UTIL_ADDRMGR_NWK_ADDR_LOOKUP
		byte[] zstackcmd = new byte[7];
		zstackcmd[0] = (byte)0xfe; //SOF
		zstackcmd[1] = 2; // length
		zstackcmd[2] = 0x27; // CMD0
		zstackcmd[3] = 0x41; // CMD1
		zstackcmd[4] = addr16bytes[0];
		zstackcmd[5] = addr16bytes[1];
		zstackcmd[6] = (byte)(zstackcmd[1] ^ zstackcmd[2] ^ zstackcmd[3] ^ zstackcmd[4] ^ zstackcmd[5]);
		try {
			nic.sendAPIPacket(zstackcmd, zstackcmd.length);
		} catch (IOException e1) {
			e1.printStackTrace();
		}
*/
		// Thrid octet is RequestType (0x00 = single device response,
		// 0x01 = extended response)
		command[2] = 0x00; // Single device response
		
		// Start index (n/a if using single device response)
		command[3] = 0x00; // Start index
		
		final Addr64Response response = new Addr64Response(addr16);
		log.debug ("created response object: " + response);
		
		// TODO: is this hash necessary now?
		addr16ToAddr64ResponseHash.put(addr16, response);  // TODO: thread safety
		
		ZDPRequest zcmd = new ZDPRequest(nic);
		seqHash.put(zcmd.getSequenceId(), response);

		
		//zcmd.setAddress64(Address64.COORDINATOR);
		//zcmd.setAddress16(Address16.COORDINATOR);
		
		zcmd.setAddress64(Address64.BROADCAST);
		zcmd.setAddress16(Address16.BROADCAST_TO_ALL);
		
		zcmd.setSourceEndpoint(0x0);
		zcmd.setClusterId(0x0001);
		zcmd.setCommand(command);
		
		
		zcmd.setCallback(this, new ZigBeePacketFilter() {
			public boolean allow(ZigBeePacket packet) {
				return (packet.getClusterId() == 0x8001);			
			}
		});
		
		// Why us keepalive set here?
		zcmd.setKeepAlive(true);
		
		log.debug ("ZDPRequest=" + zcmd);
		
		try {
			zcmd.exec();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		log.debug ("" + response + " is sleeping");
		synchronized (response) {
			try {
				response.wait(TIMEOUT);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		log.debug ("" + response + " is awake");

		
		//Address16 addr16 = addr64ToAddr16Map.get(addr64);
		if (response.getStatus() != 0x00) {
			log.error("Error translating " +addr16 + " to addr64, status=" + response.getStatus());
			return "ERR"+Integer.toHexString(response.getStatus());
		}
		
		//String responseStr = response.getAddress16().toString();
		addr16ToAddr64ResponseHash.remove(addr16); // TODO: thread safety
		
		zcmd.discard();		
		Address64 addr64 = response.getAddress64();
		
		return addr64.toString();
		
	}
	/**
	 * Get 16 bit network address from a 64 bit IEEE address.
	 * 
	 * Ref ZigBee specification (document 053474r17) 2.4.3.1.1 NWK_addr_req, page 99. This ZDP command
	 * can be transmitted unicast or broadcast to devices with macRxOnWhenIdle = true
	 * 
	 * @param addr64str 64 bit IEEE address (colon separated hex octets)
	 * @return 16 bit network address (4 hex digits)
	 */
	public String getDeviceAddr16 (Long networkId, String addr64str) throws IllegalArgumentException {
		
		log.info ("getDeviceAddr16(networkId=" + networkId + " addr64=" + addr64str + ")");
		
		// Send a broadcast command to ZDO Cluster 0x0000 (ADDR16_REQUEST).
		// Command is 10 bytes long. 8 bytes addr64, 1 byte request type, 1 byte start index
		// See ZigBee spec §2.4.3.1.1 NWK_addr_req, page. 
		// Expect a response from Cluster 0x8000. see ZigBee spec, §2.4.4.1.1, NWK_addr_rsp, (page 151)
		
		byte[] command = new byte[10];
		
		// Copy addr64 (IEEE address) to bytes 0-7 of the command
		Address64 addr64 = new Address64(addr64str);
		System.arraycopy(addr64.getBytesLSBF(), 0, command, 0, 8);
		
		// RequestType (byte 8) can be 0x00 (single device response
		// or 0x01 extended response)
		command[8] = 0x00;
		
		// Start index (only relevant if RequestType == 0x01)
		command[9] = 0x00;
		
		Addr16Response response = new Addr16Response(addr64);
		addr64ToAddr16ResponseHash.put(addr64, response);  // TODO: thread safety
	
		
		HANetwork network = HANetwork.getInstance(networkId);
		ZigBeeNIC nic = network.getNIC();
		
		
		ZDPRequest zcmd = new ZDPRequest(nic);
		seqHash.put(zcmd.getSequenceId(), response);

		zcmd.setSourceEndpoint(0x0); // has to be 0x0 ??
		zcmd.setDestinationEndpoint(0x00); // ZDO
		zcmd.setClusterId(Cluster.ZDO_ADDR16_REQUEST);
		zcmd.setCommand(command);
		zcmd.setCallback(this, new ZigBeePacketFilter() {
			public boolean allow(ZigBeePacket packet) {
				return (packet.getClusterId() == 0x8000);			
			}
		});
		zcmd.setKeepAlive(true);
		
		
		try {
			
			//zcmd.setAddress64(new Address64("10:00:00:50:C2:70:00:A7"));
			//zcmd.setAddress16(new Address16("E190"));
			//zcmd.exec();
			
			// Send again to broadcast to see what sticks
			// (Only routers will respond).
			zcmd.setAddress64(Address64.BROADCAST);
			zcmd.setAddress16(Address16.BROADCAST_TO_ALL);
			zcmd.exec();

			
			// Send to coordinator
			// This causes UBee to crash reliably. But it's seems to be necessary
			// if end devices are associated with the UBee. Ie UBee fails to respond
			// to broadcast requests!
			if (nic instanceof XBeeDriver) {
				log.info ("also sending attr64 to addr16 query to COORDINATOR");
				zcmd.setAddress64(Address64.COORDINATOR);
				zcmd.setAddress16(Address16.COORDINATOR);
				zcmd.exec();
			}
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		log.debug ("" + response + " is sleeping");
		synchronized (response) {
			try {
				response.wait(TIMEOUT);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		log.debug ("" + response + " is awake");

		
		//Address16 addr16 = addr64ToAddr16Map.get(addr64);
		if (response.getStatus() != ZDPStatus.SUCCESS) {
			log.error("Error translating " +addr64 + " to addr16, status=" + response.getStatus());
			return ZDPUtil.getStatusName(response.getStatus());
		}
		
		//String responseStr = response.getAddress16().toString();
		log.trace ("removing " + addr64 + " from addr64ToAddr16ResponseHash");
		addr64ToAddr16ResponseHash.remove(addr64); // TODO: thread safety
		
		zcmd.discard();
		
		// TODO: this does not belong here really.  Compare addr16 in database
		/*
		EntityManager em = HibernateUtil.getEntityManager();
		em.getTransaction().begin();
		List<Device> list = em.createQuery("from Device where network.id=:networkId and address64=:addr64")
				.setParameter("networkId", networkId)
				.setParameter("addr64", addr64.toString())
				.getResultList();
		if (list.size()==1) {
			Device deviceRecord = list.get(0);
			String addr16str = response.getAddress16().toString();
			if (! deviceRecord.getAddress16().equals(addr16str)) {
				
				// 16 bit network address has changed. Update DB and 
				log.warn ("Device " + deviceRecord.getAddress64() + " addr16 is out of date. In database "
					+ deviceRecord.getAddress16().toString() + " but should be " + addr16str
					+ ". Updating database.");
				deviceRecord.setAddress16(addr16str);
				
				// TODO: update in-memory copy 
				network.getDevice(addr64).setAddress16(response.getAddress16());

				deviceRecord.logEvent(LogRecord.INFO, "addr16_change", "New addr16 " + addr16str);
			}
		}
		em.getTransaction().commit();
		*/
		
		// Take this opportunity to update the system if addr16 has changed
		network.updateAddress16(addr64, response.getAddress16());
	
		return response.getAddress16().toString();
	}
	
	
	/**
	 * Handle ZDO callbacks for address resolution requests (cluster 0x8000, 0x8001)
	 * which are in response to 0x0000 and 0x0001 respectively.
	 * Cluster 0x8000 is documented here: ZigBee spec, §2.4.4.1.1, NWK_addr_rsp, (page 151)
	 * Cluster 0x8001 is documented here:
	 */
	public void handleZDPResponse(int deliveryStatus, Address16 addr16,
			ZDPRequest zcmd, byte[] payload) {
		
		log.info("Got ZDO response to " + zcmd + " response deliveryStatus=" 
				+ XBeeUtil.getTxStatusDescription(deliveryStatus) 
				);		

		// Note: this is currently always set to 0x00 (SUCCESS) for ZStack driver
		if (deliveryStatus != 0x00) {
			log.error ("ZDO command " + zcmd + " delivery error, status=" + ByteFormatUtils.formatHexByte(deliveryStatus));
			return;
		}
		
		// We got a response to the command. But the response could still be an error.
		log.debug("ZDO payload: " + ByteFormatUtils.byteArrayToString(payload));		
			
		int status = payload[1] & 0xff;
		log.debug("ZDO command status=0x" + Integer.toHexString(status) 
				+  " (" + ZDPUtil.getStatusName(status) + ")");
		
		// ZStack quirk. TODO: document.
		if (status != 0x00) {
			int seqId = payload[0] & 0xff;
			if (seqHash.containsKey(seqId)) {
				log.trace("found response in seqHash");
				ZDOResponse response = seqHash.get(seqId);
				log.trace("response=" + response);
				response.setStatus(status);
				seqHash.remove(response);
				synchronized (response) {
					response.notifyAll();
				}
				return;
			}
		}
		
		// Both ZDO response 0x8000 and 0x8001 both have the same response
		// format (bytes 1 - 8 are addr64, bytes 9,10 are addr16)
		final boolean lsbFirst = true;
		Address64 responseAddr64 = new Address64(payload, 2, lsbFirst);
		Address16 responseAddr16 = new Address16(payload, 10, lsbFirst);
	
		log.debug("response addr16=" + responseAddr16.toString());
		log.debug("response addr64=" + responseAddr64.toString());
		
		// Need to identify the request that generated this response. 
		// Unfortunately it seems that the usual packet headers are no
		// good ZDO does not use transaction sequence (?).
		
		// TODO:
		// Note: zcmd holds the request cluster... not response cluster
		ZDOResponse response = null;
		if (zcmd.getClusterId() == Cluster.ZDO_ADDR16_REQUEST) {
			// 64 to 16
			//response = addr64ToAddr16ResponseHash.get(responseAddr64);
			log.trace ("iterating over " + addr64ToAddr16ResponseHash.size() + " pending Addr16Response responses");
			// TODO: got ConcurrentModificationException here!
			// Make a copy to avoid ConcurrentMod
			// Still getting CME. Let's try iterating using index var
			// TODO: probably error in declaration of addr64ToAddr16ResponseHash (should be declared as thread safe)
			List<Addr16Response> responses = new ArrayList<Addr16Response>(addr64ToAddr16ResponseHash.values());
			int n = responses.size();
			for (int i = 0; i < n; i++) {
				Addr16Response r = responses.get(i);
				if (r.getAddress64().equals(responseAddr64)) {
					response = r;
					log.trace (r.getAddress64() + " == " + responseAddr64);

					break;
				} else {
					log.trace (r.getAddress64() + " != " + responseAddr64);
				}
			}
		} else if (zcmd.getClusterId() == Cluster.ZDO_ADDR64_REQUEST) {
			// 16 to 64 
			//response = addr16ToAddr64ResponseHash.get(responseAddr16);
			log.trace ("iterating over " + addr16ToAddr64ResponseHash.size() + " pending Addr64Response responses");

			for (Addr64Response r : addr16ToAddr64ResponseHash.values()) {
				if (r.getAddress16().equals(responseAddr16)) {
					response = r;
					log.trace (r.getAddress16() + " == " + responseAddr16);
					break;
				} else {
					log.trace (r.getAddress16() + " != " + responseAddr16);
				}
			}
		} else {
			log.trace ("?? clusterId=" + Integer.toHexString(zcmd.getClusterId()));
		}
		
		if (response == null) {
			log.warn ("Received unexpected address resolution response, ignoring");
			log.trace("Outstanding addr64 to addr16 requests are:");
			
			for (Addr16Response r : addr64ToAddr16ResponseHash.values()) {
				log.trace ("  " +  r + " "  + r.getAddress64());
			}
			log.trace("Outstanding addr16 to addr64 requests are:");
			for (Addr64Response r : addr16ToAddr64ResponseHash.values()) {
				log.trace ("  " +  r + " " + r.getAddress16());
			}
			
			return;
		}
		
		log.debug("Found a pending request matching this response: " + response);
		
		response.setStatus(status);
		
		if (status != ZDPStatus.SUCCESS) {
			synchronized (response) {
				response.notifyAll();
			}
			return;
		}
		
		// Populate response. Skip ZCL header in payload (offset=1)
		log.debug ("populating response " + response);
		response.addPacket(payload, 1);
		
		// Send notify to the waiting request which causes it to 
		// wake up and return the response 
		log.debug("Sending notify to " + response);
		synchronized (response) {
			response.notifyAll();
		}
		
		log.debug ("ZDO address resolution (0x8000,0x8001) handler done.");
	}

}
