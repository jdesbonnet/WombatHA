package ie.wombat.zigbee;

import ie.wombat.zigbee.address.Address16;

public class RoutingTableEntry {

	public Address16 addr16;
	public int status;
	public boolean memoryConstrained;
	public boolean manyToOne;
	public boolean routeRecordRequired;
	public Address16 nextNopAddr16;
	
	public String toString() {
		return " addr16=" + addr16
		 + " memoryConstrained=" + memoryConstrained
		 + " manyToOne=" + manyToOne
		 + " routeRecordRequired=" + routeRecordRequired
		 + " nextHop=" +  nextNopAddr16;
	}
	
}
