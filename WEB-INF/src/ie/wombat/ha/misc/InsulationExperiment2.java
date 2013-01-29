package ie.wombat.ha.misc;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;

/**
 * Use Netvox Z-800 with electric heater and temperature sensor to maintain 
 * room at constant temperature. Duty cycle and outside temperature should
 * allow an approximate calculation of the aggregate R-value of the room.
 * 
 * @author joe
 *
 */
public class InsulationExperiment2 {

	private static final float eps = 0.5f;
	
	private static final String onCmd =  "020C010602DB8E0A000650";
	private static final String offCmd = "020C000602DB8E0A000651";
	private static final String HOST = "192.168.1.165";
	private static final int PORT = 5000;
	
	
	public static void main (String[] arg) throws IOException {
		
		float targetTemperature = Float.parseFloat(arg[0]);
		
		InputStreamReader r = new InputStreamReader(System.in);
		LineNumberReader lnr = new LineNumberReader (r);
		
		String line;
		while (  (line = lnr.readLine()) != null) {
			String[] p = line.split("\\s+");
			if (p.length != 3) {
				continue;
			}
			float t = Float.parseFloat(p[2]);
System.err.println ("t=" + t);

			try {
				if (t < targetTemperature - eps) {
					setHeater(true);
				}
				if (t > targetTemperature + eps) {
					setHeater(false);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	public static void setHeater (boolean state) throws IOException {
		System.err.println ("setting heater "  + (state ? "on":"off"));
		java.net.Socket sock = new java.net.Socket(HOST, PORT);
		java.io.OutputStream sout = sock.getOutputStream();
		if (state) {
			sout.write(onCmd.getBytes());
		} else {
			sout.write(offCmd.getBytes());
		}
		sout.write('\n');
		//sout.close();
		sock.close();
	}
	
}
