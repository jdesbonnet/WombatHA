package ie.wombat.ha.devices;

import java.io.IOException;

import ie.wombat.zigbee.ZigBeeException;

public interface LightSensor extends Sensor {

	/**
	 * Get light illuminance in Lux units.
	 * 
	 * @return
	 * @throws ZigBeeException
	 * @throws IOException
	 */
	public float getLux() throws ZigBeeException, IOException;
}
