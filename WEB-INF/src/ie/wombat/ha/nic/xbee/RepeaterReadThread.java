package ie.wombat.ha.nic.xbee;

import ie.wombat.ha.ByteFormatUtils;

import java.io.IOException;
import java.io.InputStream;

import org.apache.log4j.Logger;

/**
 * Read packets from repeater thread and send to NIC for transmission to 
 * Zigbee network.
 * 
 * @author joe
 *
 */
public class RepeaterReadThread extends Thread  {
	
	private static Logger log = Logger.getLogger (RepeaterReadThread.class);

	private XBeeDriver nic;
	private InputStream in;
	
	public RepeaterReadThread (XBeeDriver nic, InputStream in) {
		this.nic = nic;
		this.in = in;
		
		setDaemon(true);
		setName("XBeeRepeaterRead");
		
		log.info (this + " created");

	}
	
	public void run ()  {
		try {
			mainLoop();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		// No cleanup needed
		log.info("thread has ended");

	}
	
	private void mainLoop() throws IOException {
	
		byte[] packet = new byte[256];
		int packetLen;
		while (true) {
			packetLen = XBeeUtil.readAPIFrameFromStream(in, packet);
			log.debug ("Sending packet to NIC: " + ByteFormatUtils.byteArrayToString(packet,0,packetLen));

			// TODO: Complication. The API packet ID supplied
			// by the remote client may clash with the sequence used on the local server
			// so will need to translate to local sequence and back again for reliable operation.
			nic.sendAPIFrame(packet, packetLen);
		}
		
	}
	
}
