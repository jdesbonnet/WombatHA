package ie.wombat.ha.devices;

import java.io.IOException;

import ie.wombat.ha.ZigBeeNIC;
import ie.wombat.ha.ZigBeePacket;
import ie.wombat.zigbee.ZigBeeException;
import ie.wombat.zigbee.address.Address16;
import ie.wombat.zigbee.address.Address64;
import ie.wombat.zigbee.zcl.Cluster;
import ie.wombat.zigbee.zcl.Profile;
import ie.wombat.zigbee.zcl.ZCLException;

public class CleodeZMove extends DeviceDriver {


	public CleodeZMove(Address64 address64, Address16 address16, ZigBeeNIC nic) {
		super(address64, address16, nic);
	}
	

	public boolean isBatteryPowered () {
		return true;
	}
	
	/**
	 * Get temperature in degrees celsius. 
	 * @return
	 * @throws IOException 
	 * @throws ZCLException 
	 */
	public boolean getOccupancy() throws ZigBeeException, IOException {
		int c = getIntegerAttribute(Profile.HOME_AUTOMATION,
				Cluster.OCCUPANCY, 
				0x0a,  // Source EP 10
				0x01,  // Destination EP 1
				0x0000 // Attribute ID 0 (occupancy)
				);
		return (c==1);
	}
	@Override
	public void handleZigBeePacket(ZigBeePacket packet) {
		super.handleZigBeePacket(packet);		
	}
	
}
