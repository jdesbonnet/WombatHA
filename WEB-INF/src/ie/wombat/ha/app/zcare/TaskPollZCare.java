package ie.wombat.ha.app.zcare;

import java.io.IOException;

import ie.wombat.ha.devices.CleodeZCare;
import ie.wombat.ha.devices.XBeeSeries2;
import ie.wombat.zigbee.ZigBeeException;

import org.apache.log4j.Logger;

/**
 * Poll a Cleode ZCare for pulse counter and pulse average.
 * 
 * @author Joe Desbonnet, jdesbonnet@gmail.com
 *
 */
public class TaskPollZCare implements Runnable {

	private static Logger log = Logger.getLogger(TaskPollZCare.class);
	private ZCareApp app;
	private CleodeZCare zcare;
	
	public TaskPollZCare(ZCareApp app, CleodeZCare zcare) {
		this.app = app;
		this.zcare = zcare;
	}
	
	public void run() {
				
		try {

			int pulseCounter = zcare.getPulseCounter();
			app.logEvent("PULSE",""+pulseCounter);

			//int pulseAverage = zcare.getPulseAverage();
			//app.logEvent("PULSE_AVERAGE",""+pulseAverage);

		} catch (IOException e) {
			e.printStackTrace();
		} catch (ZigBeeException e) {
			e.printStackTrace();
		}
		
		
	}

}
