package ie.wombat.zigbee;

import java.util.List;

import ie.wombat.zigbee.address.Address16;
import ie.wombat.zigbee.zcl.AttributeValue;

public interface WriteAttributesResponse {
	
	public void handleWriteAttributesResponse (int status, Address16 addr16, WriteAttributesCommand zcmd, List<AttributeValue> attributes);
	
}
