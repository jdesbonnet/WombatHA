package ie.wombat.zigbee;

import ie.wombat.zigbee.address.Address16;


/**
 * This interface must be implemented by callback objects registered with a 
 * {@link ZigBeeCommand}.
 * 
 * @author joe
 *
 */
public interface ZigBeeCommandResponse {

	// TODO: Address16 is added here because it's useful info that can be obtained
	// from the ACK or ZigBee response. However adding it to the params like this 
	// is not a good idea I think. We need an info object.
	public void handleZigBeeCommandResponse (int status, Address16 addr16, ZigBeeCommand zcmd, byte[] payload);
}
