package ie.wombat.ha.netvox.old;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketTimeoutException;

/**
 * Script to replicate polling problem observed with Z-202. After 
 * 20 minutes to 8 hours of polling a ZigBee device status through the
 * Z-202 , the Z-202 stops responding. The middle green LED is seen
 * to flash (2Hz?). A power cycle is required to restore functionality.
 * 
 * Running this script you (should?) see the following error:
 * "Iteration 980: java.net.NoRouteToHostException: No route to host"
 * (980 is just the iteration number -- that will vary).
 * At this point it will not be possible to connect to the Z-202 
 * on port 5000.
 * 
 * Joe Desbonnet, jdesbonnet@gmail.com
 */
public class CrashZ202 {

	
	/** The 16 bit address can be real or just made up. Crashes in both cases */
	private static String NETADDR16 = "A1F3";
	
	private static String ENDPOINT = "01";
	
	private static final int SOCK_TIMEOUT = 200;
	
	private String hostname;
	private int port;
	
	public CrashZ202 (String hostname, int port) {
		this.hostname = hostname;
		this.port = port;
	}
	
	/**
	 * Entry point from command line. Supplied two arguments:
	 * hostname of Z-202
	 * TCP port (usually 5000)
	 * @param arg
	 * @throws Exception
	 */
	public static void main (String[] arg) throws Exception {
		String hostname = arg[0];
		int port = new Integer(arg[1]);		
		CrashZ202 z202 = new CrashZ202(hostname,port);
		z202.loop();
	}
	
	
	public void loop () throws Exception {
		
		
		int i = 0;
		
		while (true) {
			
			Thread.sleep(20);
			
			
			// This does cause a crash, but it can take many hours (12+).
			
			System.out.print ("Iteration " + (i++) + ": ");
			try {
				queryZ202();
			} catch (SocketTimeoutException e) {
				// ignore
			} catch (Exception e) {
				System.out.print(e.toString());
			}
			
			System.out.println("");
		}

	}
	
	
	/**
	 * Send packet to the Z-202 that causes it to send a query to a ZigBee
	 * device.
	 * 
	 * @param zone
	 * @return
	 * @throws IOException
	 */
	public void queryZ202 () throws IOException {
		Socket sock = new java.net.Socket(this.hostname, this.port);
		sock.setSoTimeout(SOCK_TIMEOUT);
		
		OutputStream sout = sock.getOutputStream();
		InputStream sin = sock.getInputStream();
		
		// End points are 01 and 02
		
		
		sendPacket("0D00" // query command (?)
				+ "0B" // payload length
				+ "02" // ?
				+ NETADDR16 + ENDPOINT
				+ "0006" // On/Off cluster ID
				+ "01" // 1 attribute to query
				+ "0000" // Attribute ID 0 = state
				, sout);
		
		String packet = receivePacket(sin);
		
		sock.close();
	}
	
	/**
	 * Send a packet to Z-202. This method will automatically prefix with the packet
	 * with "02" and add checksum.
	 * 
	 * @param s Packet as hex digits. Spaces between digits are allowed.
	 * @param out Socket output stream.
	 * @throws IOException
	 */
	public static void sendPacket (String s, OutputStream out) throws IOException {
		
		// Remove spaces
		s = s.replaceAll("\\s", "");
		
		// Calculate checksum
		int b,checksum=0;
		for (int i = 0; i < s.length(); i+=2) {
			b = Integer.parseInt(s.substring(i,i+2), 16);
			checksum ^= b;
		}
		checksum &= 0xff;
		String checksumHex = (checksum < 16 ? "0" : "" ) + Integer.toHexString(checksum);
		String fullPacket = "02" + s + checksumHex;
		out.write(fullPacket.getBytes());
		out.flush();
	}
	
 	public static String receivePacket (InputStream in) throws IOException {
		int c;
		StringBuffer buf = new StringBuffer(128);
		while ( true ) { 
			c = in.read() & 0xff;
			
			// For some reason after ZigButler disconnect after 8 minutes
			// lots of 0xFF chars gets spewed down the TCP connection. Ignore these.
			if (c == 0xff) {
				return buf.toString();
			}
			
			// 'Z' signals end of packet
			if (c == 'Z') {
				return buf.toString();
			}
			
			buf.append ((char)c);
		}

	}
 	
	
}
