package ie.wombat.ha.tools;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Tool to bin data
 * 
 * Three args required: binSize (seconds), start and end date/time in format yyyyMMddHHmmss
 * eg 20110122113000
 * 
 * @author joe
 * 
 */
public class BinByTime {

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
		
		int binSize = Integer.parseInt(arg[2]);
		
		String line;
		int i,t,bin,currentBin=0,ncol=0;
		double[] bins = new double[16];
		int[] nrec = new int[16];
		
		while ((line = lnr.readLine()) != null) {
			String[] p = line.split("\\s+");
			t = Integer.parseInt(p[0]);
			if (t < start || t >= end) {
				continue;
			}
			
			if (p.length-1 > ncol) {
				ncol = p.length-1;
			}
			
			bin = t / binSize;
			if (bin != currentBin) {
				if (currentBin > 0) {
					System.out.print (currentBin*binSize + " ");
					for (i = 0; i < ncol; i++) {
						if (nrec[i] == 0) {
							System.out.print("0 ");
						} else {
							System.out.print (bins[i]/nrec[i] + " ");
						}
						bins[i] = 0;
						nrec[i] = 0;
					}
					System.out.println ("");
				}
				
				currentBin = bin;
			} else {
				for (i = 1; i < p.length; i++) {
					bins[i-1] += Double.parseDouble(p[i]);
					nrec[i-1]++;
				}
			}
		}
	}

}
