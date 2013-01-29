package ie.wombat.ha.netvox.old;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.text.SimpleDateFormat;


/**
 * Connect to Netvox [what device exactly?] and echo network traffic
 * to stdout prefixed with timestamp. One line per packet. Time in
 * unix epoch time (milliseconds) and also in ISO8601 time format (second col)
 * 
 * Run with: java NetvoxLogger em.wombat.ie 5000
 * Paramaters are: host, port
 *
 * 
 */
public class NetvoxLogger {

	private static SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssz");
	

	public static void main(String[] arg) throws Exception {
		String hostname = arg[0];
		Integer port = new Integer(arg[1]);
		// Open TCP connection to host, port.
		Socket socket = new Socket(hostname, port);
		logger(socket);
	}
	
	public static void logger (Socket sock) throws IOException {

		

		InputStream in = sock.getInputStream();
		
		// ISO8601 date format
		
		int c,i,len,nerr=0;
		int cs;

		while ( nerr < 10 ) {
			
			cs = 0;
			StringBuffer buf = new StringBuffer();

			// Read from socket until 'Z' encountered. This is end-of-packet delimiter.
			
			while ( true ) { 
				c = in.read() & 0xff;
				
				// For some reason after ZigButler disconnect after 8 minutes
				// lots of 0xFF chars gets spewed down the TCP connection. Ignore these.
				if (c == 0xff) {
					System.err.println (getTimestamp() + " E buf=" + buf.toString());
					nerr++;
					break;
				}
				
				if (c == 'Z') {
					break;
				}
				
				buf.append ((char)c);
			}
	
			System.out.print (getTimestamp() + " ");
			
			len = buf.length()/2;
			
			System.out.print (buf.substring(0,2));
			System.out.print (" ");
			System.out.print (buf.substring(2,6));
			System.out.print (" ");
			System.out.print (buf.substring(6,8));
			System.out.print (" ");
			System.out.print (buf.substring(8,buf.length()-2));
			System.out.print (" ");
			System.out.print (buf.substring(buf.length()-2));
			
			cs = 0;
			for (i = 1; i < len; i++) {
				cs ^= Integer.parseInt(buf.substring(i*2,i*2+2),16);
			}
			
			System.out.print ( cs == 0 ? " [OK]" : " [ERR]");
			System.out.println ("");
		}

	}
	
	private static String getTimestamp () {
		String ts = df.format(System.currentTimeMillis());
		ts = ts.substring(0, 19) + ts.substring(22, ts.length()); // Strip trailing "GMT"
		//long now = System.currentTimeMillis();
		//String ts = (now/1000) + "." + (now%1000);
		return ts;
	}
}
