package ie.wombat.ha.netvox.old;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class NetvoxUtil {

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
		sendString(fullPacket,out);
		out.flush();
	}
	
	/**
	 * Send reset command to Z-202. 
	 * 
	 * @param out
	 * @throws IOException
	 */
	public static void reset (OutputStream out) throws IOException {		
		sendPacket("0005 01 00",out);  // I believe this is reset. Not sure.
		out.flush();
	}
	
	/**
	 * Convert a string to sequence of bytes before sending to output stream. 
	 * Assuming simple one character to one byte encoding (ASCII / ISO-8859-1). 
	 * 
	 * @param s
	 * @param out
	 * @throws IOException
	 */
	private static void sendString (String s, OutputStream out) throws IOException {
		out.write(s.getBytes("ISO-8859-1"));
	}
	
	/**
	 * Listen for packet on socket input stream. If timeout is required need to
	 * set that on the socket before calling.
	 * 
	 * @param in
	 * @return
	 * @throws IOException
	 */
	
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
