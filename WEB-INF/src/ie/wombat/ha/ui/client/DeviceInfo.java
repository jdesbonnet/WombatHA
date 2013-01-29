package ie.wombat.ha.ui.client;

import com.google.gwt.user.client.rpc.IsSerializable;

/**
 * A lightweight device information object.
 * 
 * @author joe
 *
 */
public class DeviceInfo implements IsSerializable {
	public Long id;
	public Long networkId;
	public String name;
	public boolean status;
	public String addr64;
	public String addr16;
	public boolean batteryPowered;
	public int batteryStatus;
	public long lastRxTime;
	
	public Long getId() {
		return id;
	}
	public void setId(Long id) {
		this.id = id;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getAddr64() {
		return addr64;
	}
	public void setAddr64(String addr64) {
		this.addr64 = addr64;
	}
	public boolean isBatteryPowered() {
		return batteryPowered;
	}
	public void setBatteryPowered(boolean batteryPowered) {
		this.batteryPowered = batteryPowered;
	}
	public int getBatteryStatus() {
		return batteryStatus;
	}
	public void setBatteryStatus(int batteryStatus) {
		this.batteryStatus = batteryStatus;
	}
	
	
}
