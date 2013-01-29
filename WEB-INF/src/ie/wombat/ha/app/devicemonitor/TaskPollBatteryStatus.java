package ie.wombat.ha.app.devicemonitor;

import java.util.List;

import ie.wombat.ha.app.AppBase;
import ie.wombat.ha.devices.DeviceDriver;


import org.apache.log4j.Logger;

/**
 * Get temperature. A scheduled task that will be run periodically.
 * @author Joe Desbonnet, jdesbonnet@gmail.com
 *
 */
public class TaskPollBatteryStatus implements Runnable {

	private static Logger log = Logger.getLogger(TaskPollBatteryStatus.class);
	private AppBase app;
	
	private int deviceIndex = 0;
	
	public TaskPollBatteryStatus(AppBase app) {
		this.app = app;
	}
	
	public void run() {
		
		String logPrefix = "Network#" + app.getNetwork().getNetworkId() + ": ";
	
		List<DeviceDriver> devices = app.getNetwork().getDevices();
		
		// Get next device on round robin
		DeviceDriver device = devices.get(deviceIndex++ % devices.size());
		
		if ( ! device.isBatteryPowered()) {
			log.debug ("skipping device " + device + " because it is not battery powered");
			return;
		}
		
		log.debug("Polling battery status of " + device);
		
		int batteryStatus = device.queryBatteryStatus();
		
		log.info (logPrefix + device + " " + device.getAddress64() + " battery status " + batteryStatus + " %%%%%%%%%%%%%%");
		
		
	}

}
