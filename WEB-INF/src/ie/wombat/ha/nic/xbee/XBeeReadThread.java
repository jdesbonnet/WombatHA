package ie.wombat.ha.nic.xbee;


import ie.wombat.ha.ByteFormatUtils;
import ie.wombat.ha.Listener;
import ie.wombat.ha.NetworkMonitor;
import ie.wombat.ha.ZigBeeNIC;
import ie.wombat.ha.nic.APIFrameListener;
import ie.wombat.ha.nic.NICErrorListener;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

/**
 * This thread listens to the XBee UART for an incoming API packet and 
 * invokes any listening {@link APIFrameListener} objects.
 * 
 * TODO: does this correctly reset if START_OF_PACKET received unexpectedly?
 * 
 * TODO: much in common with {@link ZStackReadThread} (except for the readAPIFrameFromStream() method).
 * Can we refactor to make this common code?
 * @author joe
 *
 */
public class XBeeReadThread extends Thread implements XBeeConstants {

	private static Logger log = Logger.getLogger(XBeeReadThread.class);

	private ZigBeeNIC nic;
	private InputStream xbeeIn;
	private byte[] packet = new byte[256];
	private List<APIFrameListener> listeners = new ArrayList<APIFrameListener>();
	private List<NICErrorListener> errorListeners = new ArrayList<NICErrorListener>();

	/**
	 * 
	 * @param in The input stream corresponding to the XBee UART out for which to listen 
	 * for XBee API packets.
	 * 
	 */
	public XBeeReadThread (ZigBeeNIC nic, InputStream in) {
		super();
		this.nic = nic;
		this.xbeeIn = in;
	}
	
	public void run() {
		
		try {
			runLoop();
		} catch (Throwable e) {
			e.printStackTrace();
		}
		
		// Experimental: signal to listeners that NIC read thread is now dead
		// by sending a 0 byte packet.
		for (NICErrorListener l : errorListeners) {
			l.handleNICError(nic, 500);
		}
	}
	
	private void runLoop () throws IOException {
		int packetLen, packetType;

		while (true) {
			
				packetLen = XBeeUtil.readAPIFrameFromStream(xbeeIn, packet);
				
				log.debug("RX: " + ByteFormatUtils.byteArrayToString(packet,0,packetLen));
				
				packetType = packet[0] & 0xff;
				
				// Display frame ID if appropriate
				if (packetType == 0x88 || packetType == 0x8B || packetType == 0x97) {
					//int xBeeFrameId = packet[1] & 0xff;
					//log.debug ("RX frameId=0x"  + Integer.toHexString(xBeeFrameId) + ": " 
					//	+ DebugUtils.formatXBeeAPIPacket(packet, 0, packetLen)
					//	);
				} else {
					//log.debug ("RX: " + DebugUtils.formatXBeeAPIPacket(packet, 0, packetLen));
				}
				
				if (listeners.size()==0) {
					log.warn("No listeners registered.");
				}
				for (APIFrameListener l : listeners) {
					l.handleAPIFrame(packet, packetLen);
				}
		
		}
	}
	
	public synchronized void addListener (Listener o) {
		if (o instanceof APIFrameListener && !listeners.contains(o)) {
			listeners.add((APIFrameListener)o);
		}
	}
	public synchronized void removeListener (Listener o) {
		listeners.remove(o);
		errorListeners.remove(o);
	}

}
