package ie.wombat.ha.devices;

import ie.wombat.ha.ByteFormatUtils;
import ie.wombat.ha.DebugUtils;
import ie.wombat.ha.ZigBeeNIC;
import ie.wombat.ha.ZigBeePacket;
import ie.wombat.ha.ZigBeePacketListener;
import ie.wombat.ha.nic.APIFrameListener;
import ie.wombat.ha.nic.xbee.XBeeConstants;
import ie.wombat.ha.nic.xbee.XBeeDriver;
import ie.wombat.zigbee.address.Address16;
import ie.wombat.zigbee.address.Address64;
import ie.wombat.zigbee.zcl.AttributeResponseDecode;
import ie.wombat.zigbee.zcl.AttributeValue;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import org.apache.log4j.Logger;

/**
 * Base class for ZigBee device proxy. Implements common functionality.
 * 
 * @author joe
 *
 */
public class ZigBeeDeviceProxy implements ZigBeePacketListener {

	private static Logger log = Logger.getLogger(ZigBeeDeviceProxy.class);
	
	public static final int SUCCESS = 0;
	
	private Address64 addr64 = null;
	private Address16 addr16 = null;
	
	private ZigBeeNIC driver;
	private int frameId = 2;
	
	private byte[] packet = new byte[256];
	private int packetLen;
	private int wakeReason;
	private static final int ACK = 1;
	private static final int PACKET = 2;
	
	// TODO: declare it for sync access
	private HashMap<Integer,Integer> acknowledgements = new HashMap<Integer,Integer>();
	

	public ZigBeeDeviceProxy(Address64 address, ZigBeeNIC driver) {
		this.addr64 = address;
		this.driver = driver;

		// TODO: is it ok to use 'this' in a constructor?
		driver.addZigBeePacketListener(this);
	}
	
	public ZigBeeDeviceProxy(Address16 addr16, ZigBeeNIC driver) {
		this.addr16 = addr16;
		this.driver = driver;

		// TODO: is it ok to use 'this' in a constructor?
		driver.addZigBeePacketListener(this);
	}

	public Address64 getAddress64() {
		return addr64;
	}

	public void setAddress64(Address64 address) {
		this.addr64 = address;
	}

	public void getDeviceInfo () throws IOException {
		{
			int[] attrIds = {
				//0x0000, // ZCL version
				//0x0001, // Application version
				//0x0002, // Stack version
				//0x0003, // HW version
				0x0004, // Manufacturer name
				0x0005, // Model ID
				//0x0006, // Date code
				//0x0007 // Power source
			};
			//queryAttributes(0x0000, 0x0104, 0x0A, 0x0A, attrIds); // works for Z800
			queryAttributes(0x0000, 0x0104, 0x0A, 0x01, attrIds); // works for ?
			//queryAttributes(0x0000, 0x0104, 0x01, 0x01, attrIds);
		}
		
	}
	
	
	public synchronized List<AttributeValue> queryAttributes (int clusterId, int profileId,
			int srcEp, int dstEp, int[] attrIds) throws IOException {
		log.debug ("queryAttributes():"
				+ " clusterId=0x" + Integer.toHexString(clusterId)
				+ " profileId=0x" + Integer.toHexString(profileId)
				+ " srcEp=0x" + Integer.toHexString(srcEp)
				+ " dstEp=0x" + Integer.toHexString(dstEp)
				+ " attrIds=" + DebugUtils.arrayJoin(attrIds, ",")
				);
		/*
		log.info ("queryAttributes():");
		for (int i = 0; i < attrIds.length; i++) {
			log.info("  attrId: 0x" + Integer.toHexString(attrIds[i]));
		}
		*/
		/*
		byte[] header = {
				0x14, // ZCL FC (bit3: FromServer=0, bit2: ManufSpecific=1)
				(byte)0x9f, 0x10, // ?? Manufacturer specific
				(byte)0x88, // Sequence Number 
				0x00, // Command Read Attr
		};
		*/
		
		// The Cleode ZRC doesn't like the manufacturer specific stuff
		byte[] header = {
				0x10, // ZCL FC (bit3: FromServer=0, bit2: ManufSpecific=1)
				//(byte)0x9f, 0x10, // ?? Manufacturer specific
				(byte)0x88, // Sequence Number 
				0x00, // Command Read Attr
		};
		byte[] command = new byte[header.length + attrIds.length*2];
		System.arraycopy(header, 0, command, 0, header.length);
		for (int i = 0; i < attrIds.length; i++) {
			command[header.length+i*2] = (byte)(attrIds[i] & 0xff);
			command[header.length+i*2+1] = (byte)(attrIds[i] >> 8);
		}
		return execQuery(clusterId, profileId, srcEp, dstEp, command);
	}
	
	public synchronized List<AttributeValue> execQuery (int clusterId, int profileId,
			int srcEp, int dstEp, byte[] command) throws IOException {
		
		log.debug ("execQuery():"
				+ " clusterId=0x" + Integer.toHexString(clusterId)
				+ " profileId=0x" + Integer.toHexString(profileId)
				+ " srcEp=0x" + Integer.toHexString(srcEp)
				+ " dstEp=0x" + Integer.toHexString(dstEp)
				);
		
		long startTime = System.currentTimeMillis();
		
		List<AttributeValue> list = null;
		
		int frameId;
		
		// Prefer use of 16bit address if available (more network efficient)
		if (addr16 != null) {
			log.debug ("sending query command to " + addr16);
			frameId = driver.sendZigBeeCommand(
				addr16,
				clusterId, profileId, srcEp, dstEp, command
				);
		} else {
			log.debug ("sending query command to " + addr64);
			frameId = driver.sendZigBeeCommand(
					addr64,
					clusterId, profileId, srcEp, dstEp, command
					);
		}

		
		// Expecting an ACK for the query command
		log.debug ("execQuery: expecting ACK for command with frameId=" + frameId);
		
		while (! acknowledgements.containsKey(frameId)) {
			log.debug("execQuery(): SLEEP for ACK...");
			try {
				wait(10000);
			} catch (InterruptedException e) {
			}
			log.debug("execQuery(): WAKE");
			
			// TODO: is the best way of doing timeouts?
			if (System.currentTimeMillis() - startTime > 10000) {
				throw new IOException ("Timeout");
			}
			
			
		}
		
		int ackStatus = acknowledgements.get(frameId);
		
		// Expecting ACK for frameId now
		log.debug ("execQuery: Got ACK for frameId=" + frameId 
				+ ", status=" + ackStatus);
		
		if (ackStatus != 0x00) {
			throw new IOException (DebugUtils.getDeliveryStatusDescription(ackStatus)
					+ " frameId=" + frameId);
		}
		
		
		//log.debug ("execQuery: Waiting for query response");
		
		// Wait for both ack and query response packet to arrive
		while (true) {
			log.debug("execQuery(): waiting for query response");
			try {
				wait(10000);
			} catch (InterruptedException e) {
			}
			
			// TODO: is the best way of doing timeouts?
			if (System.currentTimeMillis() - startTime > 20000) {
				throw new IOException ("Timeout");
			}
			
			// Why did I wake? Ignore ACKs.
			if (wakeReason == ACK) {
				continue;
			}
			
			// OK we got a packet then
			System.err.print(">>>");
			for (int i = 0; i < packetLen; i++ ) {
				System.err.print (ByteFormatUtils.formatHexByte(packet[i]) + " ");
			}
			
			
			int ptr = 0;
				
			int fc = packet[ptr++];
				// ---- --xx = Frame Type. 
				//			0b00 = Command acts across the entire profile
				//			0b01 = Command specific to cluster
				// ---- -x-- = Manufacturer Code Field. 1 = 2 extra octets after FC
	            // ---- x--- = Direction: 0 = client to server; 1 = server to client
	            
			System.err.print ("fc=0x" + ByteFormatUtils.formatHexByte(fc));
			System.err.print (" 0b" + ByteFormatUtils.formatBinaryByte(fc));
			
			// Check for manufacturer specific flag. Skip two bytes if present
			if ( (fc & 0x04) != 0) {
				ptr += 2;
			} 
			
			int seq = packet[ptr++];
			System.err.println (" seq=0x" + ByteFormatUtils.formatHexByte(seq));
			int cmd = packet[ptr++];
			System.err.println (" cmd=0x"+ByteFormatUtils.formatHexByte(cmd));
			if (cmd != 0x01) {
				System.err.println ("WARN: expecting command=0x01 (read attribute response) but got 0x"
						+ ByteFormatUtils.formatHexByte(cmd));
			}
			
			list = AttributeResponseDecode.decode(packet, ptr,packetLen);
			
			log.debug ("execQuery(): returning with list of " + list.size() + " attributes");
			return list;
				
		}
		
	}
	
	/**
	 * 
	 * Send a ZigBee command and wait for response.
	 * 
	 * @param clusterId
	 * @param profileId
	 * @param srcEp
	 * @param dstEp
	 * @param command
	 * @return Status. 0 = SUCCESS.
	 * @throws IOException
	 */
	public synchronized int execCommand(int clusterId, int profileId,
			int srcEp, int dstEp, byte[] command) throws IOException {

		System.err.println (this + " sending command. frameId=" + frameId);

		
		int frameId;
		
		// Prefer use of 16bit address if available
		if (addr16 != null) {
			frameId = driver.sendZigBeeCommand(addr16,clusterId, profileId, srcEp, dstEp, command);
		} else {
			frameId = driver.sendZigBeeCommand(addr64,clusterId, profileId, srcEp, dstEp, command);
		}

		// Wait for packet to arrive
		while (true) {
			System.err.println("Waiting for ACK packet for frameId=" + frameId);
			try {
				wait();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		
			
			System.err.println ("Wait over");
			
			if (acknowledgements.containsKey(frameId)) {
				System.err.println ("ACK has been received for frameId=" + frameId);
				System.err.println ("  status=" + acknowledgements.get(frameId));
				// Finished with this ack so remove from acknowledgements hash
				int status = acknowledgements.get(frameId);
				acknowledgements.remove(frameId);
				return status;
			} else {
				System.err.println ("No ACK received for frameId=" + frameId + " yet.");
				System.err.println ("Continue to listen.");
			}
		}
		

	}


	public synchronized void handleZigBeePacket(ZigBeePacket zbpacket) {
		log.debug ("handleZigBeePacket(): making copy of packet data -> this.packet[]");
		packetLen = zbpacket.getPayload().length;
		System.arraycopy(zbpacket.getPayload(), 0, packet, 0, packetLen);
		wakeReason = PACKET;
		notifyAll();
	}

	public synchronized void handleAcknowledgement(int frameId, int status) {
		log.debug("handleAcknowledgement() frameId=" + frameId + " status=" + status);
		acknowledgements.put(frameId, status);
		wakeReason = ACK;
		notifyAll();
	}

	public Date getExpiryTime() {
		// TODO Auto-generated method stub
		return null;
	}

	public void setExpiryTime(Date expire) {
		// TODO Auto-generated method stub
		
	}
	
}
