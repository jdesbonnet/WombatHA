package ie.wombat.ha.devices;

import java.io.IOException;

public interface HeatingDevice {
	
	public void setState(int zone, boolean b) throws IOException;
	
	public boolean getState(int zone) throws IOException;
}
