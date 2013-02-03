package ie.wombat.ha.devices;

import java.io.IOException;

import ie.wombat.zigbee.ZigBeeException;

public interface HumiditySensor extends Sensor {

	/**
	 * Get relative humidity in percent. 
	 * 
	 * @return
	 * @throws ZigBeeException
	 * @throws IOException
	 */
	public float getRelativeHumidity() throws ZigBeeException, IOException;
}
