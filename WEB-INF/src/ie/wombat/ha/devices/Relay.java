package ie.wombat.ha.devices;

import java.io.IOException;

import ie.wombat.zigbee.ZigBeeException;

public interface Relay {

	public void setState(boolean b);
	public boolean getState ();
}
