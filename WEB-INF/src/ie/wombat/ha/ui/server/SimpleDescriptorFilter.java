package ie.wombat.ha.ui.server;

import org.apache.log4j.Logger;

import ie.wombat.ha.ZigBeePacket;
import ie.wombat.zigbee.zcl.Cluster;

public class SimpleDescriptorFilter extends ZDPFilter {

	private static Logger log = Logger.getLogger(SimpleDescriptorFilter.class);
	private int endPoint;
	
	public SimpleDescriptorFilter () {
		setClusterId(Cluster.ZDO_SIMPLE_DESCRIPTOR_REQUEST);
	}
	public void setEndPoint (int endPoint) {
		this.endPoint = endPoint;
	}
	
	@Override
	public boolean allow(ZigBeePacket packet) {
		if ( ! super.allow (packet) ) {
			return false;
		}
		
		log.trace ("applying endpoint test:");
		// Check end point in response to requested end point
		
		byte[] payload = packet.getPayload();
		int responseEndPoint = payload[5];
		log.trace ("response endpoint = " + responseEndPoint);
		
		if (this.endPoint != responseEndPoint) {
			log.trace ("rejecting packet " + packet + " because expected ep " 
					+ this.endPoint + " does not match the ep in the packet " 
					+ responseEndPoint);

			return false;
		}
		
		log.trace ("accepting packet " + packet + " as SimpleDescriptor response");
		return true;
	}
}
