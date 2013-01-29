package ie.wombat.ha.netvox.old;


import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.text.SimpleDateFormat;


/**
 * Send packet to Netvox Z-202. "02" prefix and checksum added automatically.
 * 
 * Run with: java NetvoxSendPacket 192.168.165 5000 packet-data
 * Paramaters are: host, port, packet-data
 *
 * 
 */
public class NetvoxSendPacket {

	private static SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssz");
	

	
	public static void main(String[] arg) throws Exception {
		String hostname = arg[0];
		Integer port = new Integer(arg[1]);

		StringBuffer buf = new StringBuffer();
		for (int i = 2; i < arg.length; i++) {
			buf.append(arg[i]);
		}
		
		System.err.println ("Sending " + buf.toString());
		
		// Open TCP connection to host, port.
		Socket sock = new Socket(hostname, port);
		sock.setSoTimeout(3000);
		OutputStream out = sock.getOutputStream();
		
		NetvoxUtil.sendPacket (buf.toString(),out);
		
		InputStream sin = sock.getInputStream();
		String packet = NetvoxUtil.receivePacket(sin);
		
		System.out.println (packet);
		
		
		out.close();
	}
	
}
