package ie.wombat.zigbee;

import ie.wombat.zigbee.address.Address16;


/**
 * This interface must be implemented by callback objects registered with a 
 * {@link ZDPRequest}.
 * 
 * @author Joe Desbonnet, jdesbonnet@gmail.com
 *
 */
public interface ZDPResponseListener {

	// TODO: Address16 is added here because it's useful info that can be obtained
	// from the ACK or ZigBee response. However adding it to the params like this 
	// is not a good idea I think. We need an info object.
	public void handleZDPResponse (int status, Address16 addr16, ZDPRequest zcmd, byte[] payload);
}
