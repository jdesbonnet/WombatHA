package ie.wombat.ha.devices;

public interface HeatingDevice {
	
	public void setState(int zone, boolean b);
	
	public boolean getState(int zone);
}
