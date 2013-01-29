package ie.wombat.ha.netvox.old;

import java.io.OutputStream;
import java.net.Socket;

/**
 * Script to turn Z800 power outlet on off. Harded coded to 
 * Antoin's setup (network address DB8E embedded).
 * Compile with: javac NetvoxText.java
 * Run with: java NetvoxText antoin.dyndns-web.com 5000 1
 * Paramaters are: host, port and state (0 = off, 1 = on)
 */
public class NetvoxTest {

	// Switch is DB8E endpoint 0A.
	
	// 02 0C01 06 02 DB8E 0A 0006  50
	// Observation: 0x0006 = On/Off cluster in ZCL
	// 2 = toggle?
	private static final String onCmd =  "020C010602DB8E0A000650";
	private static final String offCmd = "020C000602DB8E0A000651";
	
	public static void main(String[] arg) throws Exception {
		String hostname = arg[0];
		Integer port = new Integer(arg[1]);
		Integer state = new Integer(arg[2]);

		// Open TCP connection to host, port. Write command. Close socket.
		Socket sock = new Socket(hostname, port);
		OutputStream out = sock.getOutputStream();
		if (state == 1) {
			out.write(onCmd.getBytes());
		} else {
			out.write(offCmd.getBytes());
		}
		out.write('\n');
		out.close();
	}
}
