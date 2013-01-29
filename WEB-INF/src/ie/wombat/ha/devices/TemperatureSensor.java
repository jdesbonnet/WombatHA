package ie.wombat.ha.devices;

import java.io.IOException;

import ie.wombat.zigbee.ZigBeeException;

public interface TemperatureSensor {

	public float getTemperature() throws ZigBeeException, IOException;
	//public void setReportingInterval(int interval);
}
