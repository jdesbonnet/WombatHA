package ie.wombat.ha.tools;

import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Tool to convert log file with first field as unix epoc time to
 * ISO8601 time format
 * 
 * @author joe
 * 
 */
public class UnixTimeToISO8601 {

	private static SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssz");
	
	public static void main(String[] arg) throws Exception {

		InputStreamReader r = new InputStreamReader(System.in);
		LineNumberReader lnr = new LineNumberReader(r);

		
		String line;
		int i;
		long tsl;
		while ((line = lnr.readLine()) != null) {
			String[] p = line.split("\\s+");
			tsl = (long)(Double.parseDouble(p[0]) * 1000);
			System.out.print(getTimestamp(tsl));
			for (i = 1; i < p.length; i++) {
				System.out.print(" " + p[i]);
			}
			System.out.println("");
		}
	}
	
	private static String getTimestamp (long tsl) {
		String ts = df.format(new Date(tsl));
		ts = ts.substring(0, 19) + ts.substring(22, ts.length()); // Strip trailing "GMT"
		ts += "." + tsl%1000;
		//long now = System.currentTimeMillis();
		//String ts = (now/1000) + "." + (now%1000);
		return ts;
	}
	

}
