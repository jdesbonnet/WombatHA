package ie.wombat.ha.netvox.old;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.net.Socket;

/**
 * Script extract current and voltage from Z800 power outlet. Harded coded to 
 * Antoin's setup (network address DB8E embedded).
 * Compile with: javac NetvoxText2.java
 * Run with: java NetvoxText antoin.dyndns-web.com 5000
 * Paramaters are: host, port
 *
 * Example for full packet (emphasis by means of square brackets -- brackets not in original packet data):
 * 020D0112[DB8E][0A]070202 E000 00 21[0045] E0010021[00E8]  EA Z
 *          addr                    69mA            242V    CS
 * 
 */
public class NetvoxTest2 {

	// This identifies packet with current and voltage data from power outlet
	private static final String startOfPacket = "020D0112DB8E0A070202E0000021";

	public static void main(String[] arg) throws Exception {
		String hostname = arg[0];
		Integer port = new Integer(arg[1]);

		// Open TCP connection to host, port.
		Socket sock = new Socket(hostname, port);
		InputStream in = sock.getInputStream();

		StringBuffer buf = new StringBuffer();
		String s;
		char c;
		int mA, V;

		while (true) {
			buf.setLength(0);

			// Read from socket until 'Z' encountered. This is end-of-packet delimiter.
			//while (  ( c = (char) in.read() ) != 'Z' ) { 
				//buf.append (c);
			//}
			while (   true ) { 
				c = (char) in.read() ;
				buf.append (c);
				if (c == 'Z') {
					break;
				}
			}
			s = buf.toString();

			if (s.startsWith(startOfPacket)) {
				// Extract current (mA), voltage and convert from 4 digit hex (16bit)
				mA = Integer.parseInt(buf.substring(28,32),16);
				V = Integer.parseInt(buf.substring(40,44),16);
				System.out.println ("V=" + V + " mA=" + mA);
			}
		}

	}
}
