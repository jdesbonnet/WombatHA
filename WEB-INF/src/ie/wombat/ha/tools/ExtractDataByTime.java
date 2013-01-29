package ie.wombat.ha.tools;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Tool to extract data from a log file in format 
 * unix-timestamp (space) data...
 * 
 * Two args required: start and end date/time in format yyyyMMddHHmmss
 * eg 20110122113000
 * 
 * @author joe
 * 
 */
public class ExtractDataByTime {

	public static final SimpleDateFormat df = new SimpleDateFormat("yyyyMMddHHmmss");
	
	public static void main(String[] arg) throws Exception {

		InputStreamReader r = new InputStreamReader(System.in);
		LineNumberReader lnr = new LineNumberReader(r);

		int start = (int)(df.parse(arg[0]).getTime()/1000);
		
		int end;
		if ( "now".equals(arg[1])) {
			end = (int)(System.currentTimeMillis()/1000);
		} else {
			end = (int)(df.parse(arg[1]).getTime()/1000);
		}
		
		String line;
		int t;
		while ((line = lnr.readLine()) != null) {
			String[] p = line.split("\\s+");
			t = Integer.parseInt(p[0]);
			if (t >= start && t < end) {
				System.out.println (line);
			}
		}
	}

}
