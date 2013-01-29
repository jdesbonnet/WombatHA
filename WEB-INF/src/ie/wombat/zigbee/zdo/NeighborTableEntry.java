package ie.wombat.zigbee.zdo;

import com.google.gwt.user.client.rpc.IsSerializable;

import ie.wombat.zigbee.address.Address16;
import ie.wombat.zigbee.address.Address64;

public class NeighborTableEntry implements IsSerializable {

	public Address64 panId;
	public Address64 addr64;
	public Address16 addr16;
	public int deviceType;
	public int relationshipType;
	public int rxOnWhenIdle = 0;
	public int joinPermitted = 0;
	public int depth;
	public int lqi;
	
	public NeighborTableEntry () {
		
	}
	public NeighborTableEntry (byte[] bytes, int offset) {
		boolean lsbFirst = true;
		panId = new Address64(bytes,offset+0,lsbFirst);
		addr64 = new Address64(bytes,offset+8,lsbFirst);
		addr16 = new Address16(bytes,offset+16,lsbFirst);
		
		deviceType = bytes[offset+18] & 0x03;
		rxOnWhenIdle = bytes[offset+18] & 0x0a;
		relationshipType = bytes[offset+18] >> 4;
		
		joinPermitted = bytes[offset+19];
		depth = bytes[offset+20] & 0xff;
		lqi = bytes[offset+21] & 0xff;
	}
	
	public String toString() {
		return "pan64=" + panId + " addr64=" + addr64 + " addr16=" + addr16
		 + " devType=" + getDeviceTypeName()
		 + " rxOnWhenIdle=" + rxOnWhenIdle
		 + " devRel=" + getDeviceRelationshipName()
		 + " joinPermitted=" + joinPermitted
		+ " depth=" + depth + " lqi=" + lqi;
	}
	public String getDeviceTypeName () {
		switch (deviceType) {
		case 0:
			return "Coordinator";
		case 1:
			return "Router";
		case 2: 
			return "EndDevice";
		default:
			return "Unknown";
		}
	}
	
	public String getDeviceRelationshipName() {
		switch (relationshipType) {
		case 0:
			return "Parent";
		case 1:
			return "Child";
		case 2:
			return "Sibling";
		case 3:
			return "UnknownRelation";
		case 4:
			return "PrevChild";
		default:
			return "Unknown";
		}
	}
	public boolean isRouter () {
		return (deviceType == 1);
	}
}
