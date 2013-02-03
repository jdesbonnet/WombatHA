package ie.wombat.ha.devices;

import java.io.IOException;

import ie.wombat.zigbee.ZigBeeException;

public interface TemperatureSensor extends Sensor {

	public float getTemperature() throws ZigBeeException, IOException;
	//public void setReportingInterval(int interval);
}
