package ie.wombat.ha.ui.server;

import org.apache.log4j.Logger;

import ie.wombat.ha.ZigBeePacket;
import ie.wombat.ha.ZigBeePacketFilter;
import ie.wombat.zigbee.address.Address16;
import ie.wombat.zigbee.address.Address64;


public class ZDPFilter implements ZigBeePacketFilter {

	private static Logger log = Logger.getLogger(ZDPFilter.class);
	
	private int requestClusterId;
	
	public ZDPFilter () {
	}
	
	public void setClusterId (int requestClusterId) {
		this.requestClusterId = requestClusterId;
	}
	
	public boolean allow(ZigBeePacket packet) {
		
		// Reject any packets not from EP0
		if (packet.getSourceEndPoint() != 0) {
			log.trace(this + " rejecting packet: expecting EP 0, received " + packet.getSourceEndPoint());
			return false;
		}
		
		// Reject any packets not clusterId 0x8xxx
		if (packet.getClusterId() != (requestClusterId | 0x8000) ) {
			log.trace(this + " rejecting packet: expecting cluster 0x8xxx, received 0x"
					+ Integer.toHexString(packet.getClusterId()));
			return false;
		}
		
		// TODO: is it safe to reject a packet just because the
		// source does not match the address of interest. Source
		// of response may be from another device.
		/*
		if (! packet.getSourceAddress64().equals(Address64.UNKNOWN)) {
			return (packet.getSourceAddress64().equals(addr64));
		}
		if (! packet.getSourceAddress16().equals(Address16.UNKNOWN)) {
			return (packet.getSourceAddress16().equals(addr16));
		}
		*/
		return true;
	}

}
