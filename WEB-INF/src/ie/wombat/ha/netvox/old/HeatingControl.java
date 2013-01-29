package ie.wombat.ha.netvox.old;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;

/**
 * Control Netvox Z-802 (connected to heating boiler)
 * 
 * @author joe
 *
 */
public class HeatingControl {

	private static final int SOCK_TIMEOUT = 3000;
	private static final String HOST = "192.168.1.165";
	private static final int PORT = 5000;
	
	public static final int I_FRONTDOOR = 0;
	public static final int I_HALL = 1;
	public static final int I_KITCHEN = 3;
	public static final int I_SITTINGROOM = 4;
	public static final int I_OUTDOORS = 6;
	
	// Z-802 16 bit address on my network (16 bit, 4 hex digits)
	//private static String NETADDR16 = "A1F1";  
	private static String NETADDR16 = "FE94";  
	
	/**
	 * Command line tool to turn heating on off. Eg
	 * java -cp ./WEB-INF/classes ie.wombat.ha.HeatingControl 1 on
	 * (turn zone 1 on).
	 * 
	 * @param arg
	 * @throws Exception
	 */
	public static void main (String[] arg) throws Exception {
	
		
		int zone = Integer.parseInt(arg[0]);
		
		if (zone < 1 || zone > 2) {
			throw new Exception ("Invalid zone. Only 1 or 2 allowed.");
		}
		
		boolean state;
		if ("on".equals(arg[1])) {
			state = true;
		} else if ("off".equals(arg[1])) {
			state = false;
		} else {
			throw new Exception ("Invalid state. Only 'on' or 'off' allowed.");
		}
		
		setHeatingState(zone, state);
		
	}
	
	public static void reset () throws IOException {
		Socket sock = new Socket(HOST, PORT);
		sock.setSoTimeout(3000);
		OutputStream sout = sock.getOutputStream();
		NetvoxUtil.reset(sout);
		sock.close();
	}
	public static void setHeatingState (int zone, boolean state) throws IOException {
		Socket sock = new Socket(HOST, PORT);
		sock.setSoTimeout(3000);
		OutputStream sout = sock.getOutputStream();
			
		// End points are 01 and 02
		String endPoint = "0" + zone;
		NetvoxUtil.sendPacket("0C0" + (state ? "1":"0")  // )C01 = on, 0C00 = off
				+ " 06 " // payload length
				+ " 02 " // ?
				+ NETADDR16 + endPoint 
				+ "0006", // On/Off cluster ID 
				sout);
		
		sock.close();
	}
	
	public static float[] getTemperatures () throws IOException {
		Socket sock = new Socket(HOST, 4444);
		sock.setSoTimeout(3000);
		InputStream sin = sock.getInputStream();
		BufferedReader r = new BufferedReader(new InputStreamReader (sin));
		String line;
		
		int i, h,l,v;
		
		float[] ret = new float[8];
		
		while ( (line = r.readLine()) != null) {

			String[] p = line.split("\\s+");

			if (!"53".equals(p[1])) {
				continue;
			}

			System.out.print (p[0]);
			for (i = 0; i < 7; i++) {
				h = Integer.parseInt(p[5+i*2],16);
				l = Integer.parseInt(p[6+i*2],16);
				v = (h*256+l);
				if (v > 0x7fff) {
					v -= 0x10000;
				}	
				ret[i] = (float)v/16f;
			}

			// Battery
			h = Integer.parseInt(p[19],16);
			l = Integer.parseInt(p[20],16);
			v = (h*256+l)>>6;
			
			
		}
	
		sock.close();
		
		return ret;
	}
	
	/**
	 * Get heating state
	 * 
	 * @param zone
	 * @return
	 * @throws IOException
	 */
	public static boolean getState (int zone) throws IOException {
		Socket sock = new java.net.Socket(HOST, PORT);
		sock.setSoTimeout(SOCK_TIMEOUT);
		
		OutputStream sout = sock.getOutputStream();
		InputStream sin = sock.getInputStream();
		
		// End points are 01 and 02
		String endPoint = "0" + zone;
		
		NetvoxUtil.sendPacket("0D00" // query command (?)
				+ "0B" // payload length
				+ "02" // ?
				+ NETADDR16 + endPoint 
				+ "0006" // On/Off cluster ID
				+ "01" // 1 attribute to query
				+ "0000" // Attribute ID 0 = state
				, sout);
		
		String packet = NetvoxUtil.receivePacket(sin);
		sock.close();
		
		
		// Expect 020D010BDB8E0A00060100000010bbCC where
		// bb = state = 00 | 01 and CC is checksum
		if (! packet.startsWith("020D010B" + NETADDR16 + endPoint + "0006")) { 
			throw new IOException ("unexpected packet " + packet);
		}

		// Assume it's just 0 or 1 and convert to boolean return value
		int state = Integer.parseInt(packet.substring(29, 30));
		return (state == 1);
	}
	
	/**
	 * Get heating state for all zones
	 * 
	 * @param zone
	 * @return
	 * @throws IOException
	 */
	public static Boolean[] getState () throws IOException {
		Socket sock = new java.net.Socket(HOST, PORT);
		sock.setSoTimeout(SOCK_TIMEOUT);
		
		OutputStream sout = sock.getOutputStream();
		InputStream sin = sock.getInputStream();
		
		Boolean[] ret = new Boolean[3];
		
		for (int zone = 1; zone < 3; zone++) {
		
			// try 3 times
			for (int i = 0; i < 3; i++) {
				String endPoint = "0" + zone;
		
				NetvoxUtil.sendPacket("0D00" // query command (?)
						+ "0B" // payload length
						+ "02" // ?
						+ NETADDR16 + endPoint 
						+ "0006" // On/Off cluster ID
						+ "01" // 1 attribute to query
						+ "0000" // Attribute ID 0 = state
						, sout);
		
				String packet = NetvoxUtil.receivePacket(sin);
		
				// Expect 020D010BDB8E0A00060100000010bbCC where
				// bb = state = 00 | 01 and CC is checksum
				if ( packet.startsWith("020D010B" + NETADDR16 + endPoint + "0006")) { 
					ret[zone] = (Integer.parseInt(packet.substring(29, 30)) == 1);
				}
			}
		}

		return ret;
	}
	
}
