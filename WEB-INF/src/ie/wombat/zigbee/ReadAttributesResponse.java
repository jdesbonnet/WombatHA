package ie.wombat.zigbee;

import java.util.List;

import ie.wombat.zigbee.address.Address16;
import ie.wombat.zigbee.zcl.AttributeValue;

public interface ReadAttributesResponse {
	
	// TODO: Address16 is added here because it's useful info that can be obtained
	// from the ACK or ZigBee response. However adding it to the params like this 
	// is not a good idea I think. We need an info object.
	public void handleReadAttributesResponse (int status, Address16 addr16, ReadAttributesCommand zcmd, List<AttributeValue> attributes);
	
}
