package ie.wombat.ha.devices;

import java.io.IOException;

import ie.wombat.zigbee.ZigBeeException;

public interface LightSensor extends Sensor {

	public float getLux() throws ZigBeeException, IOException;
}
