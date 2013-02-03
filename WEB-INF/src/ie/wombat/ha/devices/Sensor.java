package ie.wombat.ha.devices;

import ie.wombat.zigbee.address.Address16;
import ie.wombat.zigbee.address.Address64;

public interface Sensor {

	public Address64 getAddress64();
	public Address16 getAddress16();
	
}
