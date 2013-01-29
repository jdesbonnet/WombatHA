package ie.wombat.ha.app.xbee;

import java.io.IOException;

import ie.wombat.ha.devices.XBeeSeries2;

import org.apache.log4j.Logger;

/**
 * Get temperature. A scheduled task that will be run periodically.
 * @author Joe Desbonnet, jdesbonnet@gmail.com
 *
 */
public class TaskPollXBee implements Runnable {

	private static Logger log = Logger.getLogger(TaskPollXBee.class);
	private XBeeIOApp app;
	private XBeeSeries2 xbee;
	private int pollCount = 0;
	
	public TaskPollXBee(XBeeIOApp app, XBeeSeries2 xbee) {
		this.app = app;
		this.xbee = xbee;
	}
	
	public void run() {
		
System.err.println ("******************** " + TaskPollXBee.class + " running");
		
		String logPrefix = "Network#" + app.getNetwork().getNetworkId() + ": ";
	
		pollCount++;
		
		byte[] data = new byte[1];
				
		try {

			// Randomly perform one of the following commands
			switch (pollCount % 4) {
			case 0:
				data[0] = 'T';
				xbee.sendData(data);
				break;
			case 1:
				data[0] = 'H';
				xbee.sendData(data);
				break;
			case 2:
				data[0] = 'B';
				xbee.sendData(data);
				break;
			case 3:
				xbee.execATQuery("%V");
			}

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
	}

}
