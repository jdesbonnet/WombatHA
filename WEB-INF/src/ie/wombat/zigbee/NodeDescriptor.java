package ie.wombat.zigbee;

import ie.wombat.zigbee.address.Address16;

public class NodeDescriptor {

	public Address16 addr16;
	public int nodeType;
	public boolean complexDescriptorAvailable;
	public boolean userDescriptorAvailable;
	public int frequencyBand;
	public int macCapability;
	public int manufacturerCode;
	// TODO: more fields
	
	public String toString() {
		return "nodeType=" + nodeType 
		+ " complexDesc=" + complexDescriptorAvailable
		+ " userDesc=" + userDescriptorAvailable
		+ " freq=" + frequencyBand
		+ " macCap=" + macCapability
		+ " manufCode=" + manufacturerCode;
	}
}
