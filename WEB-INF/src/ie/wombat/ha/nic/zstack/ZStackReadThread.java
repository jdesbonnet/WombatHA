package ie.wombat.ha.nic.zstack;


import ie.wombat.ha.ByteFormatUtils;
import ie.wombat.ha.nic.APIFrameListener;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

/**
 * This thread listens to the NIC UART for an incoming API frame and 
 * invokes any listening {@link APIFrameListener} objects.
 * 
 * TODO: much in common with {@link XBeeReadThread} (except for the readAPIFrameFromStream() method).
 * Can we refactor to make this common code?
 * 
 * @author joe
 */
public class ZStackReadThread extends Thread  {

	private static Logger log = Logger.getLogger(ZStackReadThread.class);

	private static final int START_OF_FRAME=0xFE;
	
	private InputStream nicIn;
	private byte[] frame = new byte[256];
	private List<APIFrameListener> listeners = new ArrayList<APIFrameListener>();
	
	/**
	 * 
	 * @param in The input stream corresponding to the XBee UART out for which to listen 
	 * for XBee API packets.
	 * 
	 */
	public ZStackReadThread (InputStream in) {
		super();
		this.nicIn = in;
	}
	
	public void run() {
		int frameLen;
		while (true) {
			try {
				
				frameLen = readAPIFrameFromStream(nicIn, frame);
				
				log.debug("RX: SOF LEN " + ByteFormatUtils.byteArrayToString(frame,0,frameLen) + " FCS");
				
				if (listeners.size()==0) {
					log.warn("No listeners registered.");
				}
				for (APIFrameListener l : listeners) {
					l.handleAPIFrame(frame, frameLen);
				}
			} catch (IOException e) {
				log.error(e.toString());
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
			}
		}
	}
	public synchronized void addListener (APIFrameListener o) {
		if (!listeners.contains(o)) {
			listeners.add(o);
		}
	}
	public synchronized void removeListener (APIFrameListener o) {
		listeners.remove(o);
	}

	public static int readAPIFrameFromStream (InputStream nicIn, byte[] frame) throws IOException {
		int b, i;
		
		i = 255;
		while ( (b = nicIn.read()) != START_OF_FRAME ) {
			log.debug("Expecting START_OF_FRAME, skipping " + ByteFormatUtils.formatHexByte(b));
			if (--i == 0) {
				throw new IOException ("Start of frame delimiter not found");
			}
		}
	
		int dataLen = nicIn.read() & 0xff;
		log.trace ("dataLen=" + dataLen);
		
		// Checksum starts with dataLen
		int cs = dataLen;
		
		// TODO: can make this more efficient by doing block reads
		for (i = 0; i < dataLen+2; i++){
			b = nicIn.read() & 0xff;
			frame[i] = (byte)b;
			cs ^= b;
		}
		
		// Read FCS
		b = nicIn.read();
		cs ^= b;
		
		if ( (cs&0xff) != 0x00 ) {
			throw new IOException ("API frame checksum fail: csCalc=0x"
					+ Integer.toHexString(cs)
					+ " (should be 0x00) data="
					+ ByteFormatUtils.byteArrayToString(frame,0,dataLen+2)
					);
		} else {
			log.trace ("Checksum OK");
		}
		
		return dataLen+2;
		
	}
}
