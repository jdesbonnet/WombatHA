package ie.wombat.ha.devices;



import org.apache.log4j.Logger;

import ie.wombat.ha.ZigBeeNIC;
import ie.wombat.ha.ZigBeePacket;

import ie.wombat.zigbee.address.Address16;
import ie.wombat.zigbee.address.Address64;


/**
 * The new (April 2011) version of the Cleode ZRC.
 * 
 * @author joe
 *
 */
public class CleodeZDoor extends DeviceDriver  {

	private Logger log = Logger.getLogger (CleodeZDoor.class);
	
	
	
	public CleodeZDoor(Address64 address64, Address16 address16, ZigBeeNIC nic) {
		super(address64, address16, nic);
	}


	public boolean isBatteryPowered () {
		return true;
	}

	public void setState (boolean b) {
		
	}
	public boolean getState() {
		return false;
	}


	@Override
	public void handleZigBeePacket(ZigBeePacket packet) {	
		super.handleZigBeePacket(packet);
	}
	

}
