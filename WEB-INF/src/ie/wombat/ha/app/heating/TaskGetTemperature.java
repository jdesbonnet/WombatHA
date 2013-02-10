package ie.wombat.ha.app.heating;

import ie.wombat.ha.devices.TemperatureSensor;
import ie.wombat.zigbee.ZigBeeException;

import java.io.IOException;

import org.apache.log4j.Logger;

/**
 * Get temperature. A scheduled task that will be run periodically.
 * @author Joe Desbonnet, jdesbonnet@gmail.com
 *
 */
public class TaskGetTemperature implements Runnable {

	private static Logger log = Logger.getLogger(TaskGetTemperature.class);
	private HeatingApp app;
	private int zone;
	
	public TaskGetTemperature(HeatingApp app, int zone) {
		this.app = app;
		this.zone = zone;
	}
	
	public void run() {
		
		String logPrefix = "Network#" + app.getNetwork().getNetworkId() + ": ";
		
		long now = System.currentTimeMillis();
		if ((now - app.getTemperatureTime(zone)) < 120000L) {
			log.debug (logPrefix + "No need for temperature poll, as temperature was reported recently.");
			return;
		}
		
		TemperatureSensor ts = app.getSensorDevice(zone);
		
		try {
			log.debug(logPrefix + "Requesting temperature device=" + ts);
			float t = ts.getTemperature();
			log.debug(logPrefix + "Temperature sensor response t=" + t);
			app.notifyCurrentTemperature(zone,t);
		} catch (ZigBeeException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (Throwable e) {
			e.printStackTrace();
		}
		
		
	}

}
