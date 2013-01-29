package ie.wombat.ha.devices;



import org.apache.log4j.Logger;

import ie.wombat.ha.ZigBeeNIC;
import ie.wombat.ha.ZigBeePacket;

import ie.wombat.zigbee.address.Address16;
import ie.wombat.zigbee.address.Address64;


public class NetvoxZ501B extends DeviceDriver  {

	private Logger log = Logger.getLogger (NetvoxZ501B.class);
	
	public NetvoxZ501B(Address64 address64, Address16 address16, ZigBeeNIC nic) {
		super(address64, address16, nic);
	}

	public boolean isBatteryPowered () {
		return true;
	}

	@Override
	public void handleZigBeePacket(ZigBeePacket packet) {
		super.handleZigBeePacket(packet);

	}
	
}
