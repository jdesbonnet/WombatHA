package ie.wombat.ha.nic;


import ie.wombat.ha.ZigBeeNIC;

import java.io.IOException;
import java.util.LinkedList;
import java.util.Queue;

import org.apache.log4j.Logger;

/**
 * Link the NIC UART to the NIC driver by means of a HTTP bridge.
 * 
 * TODO: should the queue be an integral part of the NIC instead?
 * 
 * @author joe
 *
 */
public class ServletAdapter implements UARTAdapter {

	private static Logger log = Logger.getLogger(ServletAdapter.class);
	
	private static final long PACKET_EXPIRY_TIME = 120000L;
	
	private Queue<APIPacket> queue = new LinkedList<APIPacket>();
	private APIFrameListener listener;
	
	public ServletAdapter () {
		
	}
	
	public void setName (String name) {
		
	}
	
	/**
	 * {@link ZigBeeNIC} implementation will call this to queue a NIC API packet
	 * for transmission to the ZigBee Network via a remote NIC module on the other side
	 * of a HTTP link.
	 */
	public void txAPIFrame(byte[] apiFrame, int frameLen)
			throws IOException {
		
		log.info("Recevied API packet for transmission to NIC hardware. Storing in queue.");
		
		// Annoyingly the spec calls for the unencoded packet payload to be
		// transmitted over the HTTP link. So we need to decode this again.
		
		// TODO: need to fix this for XBee
		//byte[] payload = XBeeUtil.decodeAPIFrame(apiPacketData, packetLen);
		byte[] payload = new byte[frameLen-3];
		System.arraycopy(apiFrame, 2, payload, 0, payload.length);
	
		// TODO: this should be by exception
		//if (payload == null) {
			//log.warn("sendAPIFrame(): decodeAPIPacket() returned null payload, ignoring");
			//return;
		//}
		
		APIPacket packet = new APIPacket();
		packet.payload = payload;
		packet.status=APIPacket.STATUS_INQUEUE;
		APIPacketLog.addPacket(packet);
		
		synchronized (queue) {
			queue.add(packet);
		}
		
		// Support for experimental long poll. Notify and wake any threads waiting on 
		// this object.
		synchronized (this) {
			notifyAll();
		}
		
	}

	
	public void setRxAPIFrameListener(APIFrameListener listener) {
		// listener needs to be communicated to the Servlet somehow
		this.listener = listener;
	}
	public APIFrameListener getServerSideListener() {
		return listener;
	}

	public APIPacket getNextAPIPacket() {
		APIPacket packet;
		
		// Pop packet off queue. Discard any expired packets. Return
		// null if no packets left.
		while (true) {
			synchronized (queue) {
				packet = queue.poll();
			}
			if (packet == null) {
				//log.debug("getNextXBeeAPIPacket(): no packets");
				return null;
			}
			if (packet.timestamp < (System.currentTimeMillis() - PACKET_EXPIRY_TIME) ) {
				log.warn ("Dropping APIPacket because it's too old (queued "  
					+ ((System.currentTimeMillis()-packet.timestamp)/1000L) 
					+ " seconds ago)");
				packet.status=APIPacket.STATUS_TIMEOUT;
			} else {
				log.debug("getNextAPIPacket(): returning " + packet.toString());
				return packet;
			}
		}
		
	}
	
	public void close() {
		// TODO
	}
}
