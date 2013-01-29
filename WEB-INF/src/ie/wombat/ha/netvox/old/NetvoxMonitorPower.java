package ie.wombat.ha.netvox.old;


import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;

/**
 * 
 * 
 * Run with: java NetvoxLogger em.wombat.ie 5000 Paramaters are: host, port
 * 
 * 
 */
public class NetvoxMonitorPower {

	private static SimpleDateFormat df = new SimpleDateFormat(
			"yyyy-MM-dd'T'HH:mm:ssz");
	private static String NETADDR16 = "DB8E";
	private static String ENDPOINT = "0A";
	//private static String NETADDR16 = "A1F1";
	//private static String ENDPOINT = "02";

	public static void main(String[] arg) throws Exception {
		String hostname = arg[0];
		Integer port = new Integer(arg[1]);

		StringBuffer buf2 = new StringBuffer();
		for (int i = 2; i < arg.length; i++) {
			buf2.append(arg[i]);
		}

		int success=0;
		while (true) {

			Thread.sleep(1000);
			System.err.print("(" + success +") ");
			
			try {
				System.out.println(pollDevice(hostname,port));
				success++;
				System.err.print("*");
			} catch (SocketTimeoutException e) {
				System.err.print("T");
			} catch (Exception e) {
				e.printStackTrace();
			}
			
			System.err.flush();
		}
	}

	/**
	 * Poll Z-800 outlet device (hard coded 16bit address 0xDB8E)
	 * for state, voltage and current (mA). As state is on a different
	 * cluster to voltage and current two separate requests are required.
	 * 
	 * Open a new socket for each request and close when finished.
	 * 
	 * @param hostname
	 * @param port
	 * @return
	 * @throws UnknownHostException
	 * @throws IOException
	 */
	private static String pollDevice(String hostname, int port)
			throws UnknownHostException, IOException {
		// Open TCP connection to host, port.
		Socket sock = new Socket(hostname, port);
		sock.setSoTimeout(3000);
		OutputStream out = sock.getOutputStream();
		InputStream in = sock.getInputStream();

		String packet = null;
		int mA=0, V=0;

		packet = "0D00 0B 02 " + NETADDR16 + ENDPOINT + "0702 02 E000 E001";
System.err.println ("packet sent=" + packet);
		
		NetvoxUtil.sendPacket(packet, out);
		packet = NetvoxUtil.receivePacket(in);
System.err.println ("packet received=" + packet);
		mA = Integer.parseInt(packet.substring(28, 32), 16);
		V = Integer.parseInt(packet.substring(40, 44), 16);
		
		
		NetvoxUtil.sendPacket("0D00 0B 02 " + NETADDR16 + ENDPOINT + " 0006 01 0000", out);
		packet = NetvoxUtil.receivePacket(in);

		// Expect 020D010BDB8E0A00060100000010bbCC where
		// bb = 00 | 01 and CC is checksum
		if (! packet.startsWith("020D010B" + NETADDR16 + ENDPOINT + "0006")) { 
			throw new IOException ("unexpected packet " + packet);
		}

		int state = Integer.parseInt(packet.substring(29, 30));

		sock.close();

		return (System.currentTimeMillis() / 1000) + "\t" + state + " " + V + " "
				+ mA;
	}

}
