package ie.wombat.ha.devices;

import java.io.IOException;

import ie.wombat.zigbee.ZigBeeException;

public interface HumiditySensor extends Sensor {

	public float getRelativeHumidity() throws ZigBeeException, IOException;
}
