package ie.wombat.ha.tools;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;

/**
 * Convert on/off duty cycle from PowerMonitor log into a mean power consumption
 * 
 * @author joe
 * 
 */
public class ConvertDutyCycle {

	private static final int A = 7370;

	public static void main(String[] arg) throws IOException {

		InputStreamReader r = new InputStreamReader(System.in);
		LineNumberReader lnr = new LineNumberReader(r);

		String line;
		int t = 0, cycleStart, cycleEnd, heaterOff;
		float meanP;

		while ((line = lnr.readLine()) != null) {
			String[] p = line.split("\\s+");
			if (p.length != 4) {
				continue;
			}
			t = Integer.parseInt(p[0]);
			if ("1".equals(p[1])) {
				break;
			}
		}

		while (true) {

			cycleStart = t;

			while ((line = lnr.readLine()) != null) {
				String[] p = line.split("\\s+");
				if (p.length != 4) {
					continue;
				}
				t = Integer.parseInt(p[0]);
				if ("0".equals(p[1])) {
					break;
				}
			}

			heaterOff = t;

			while ((line = lnr.readLine()) != null) {
				String[] p = line.split("\\s+");
				if (p.length != 4) {
					continue;
				}
				t = Integer.parseInt(p[0]);
				if ("1".equals(p[1])) {
					break;
				}
			}

			cycleEnd = t;

			meanP = (float)(A * 230 * (heaterOff - cycleStart) / (cycleEnd - cycleStart))/1000;
			meanP /= 100f;

			System.out.println(
					//(cycleStart + (cycleEnd - cycleStart) / 2)
					cycleStart
					+ " "
					+ meanP);
		}

	}

}
