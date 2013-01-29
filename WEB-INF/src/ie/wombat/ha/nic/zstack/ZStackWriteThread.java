package ie.wombat.ha.nic.zstack;

import java.io.OutputStream;


import org.apache.log4j.Logger;

/**
 * TODO: experimental
 * 
 * This thread pops API packets ready for transmission to the NIC UART off
 * a queue and transmits it to the NIC UART. This is experimental. Up to now
 * I have been sending API packets directly to the UART without any intermediate
 * queue. 
 * The reason for the introduction of this thread is that we are experiencing
 * problems with the UBee: it crashes if there are many ZDO requests issued
 * at approx the same time.  The hope is that if we can control the rate at 
 * which packets are sent to the UBee we can avoid this crash.
 *  
 * TODO: much in common with {@link XBeeReadThread} (except for the readAPIFrameFromStream() method).
 * Can we refactor to make this common code?
 * 
 * @author joe
 */
public class ZStackWriteThread extends Thread  {

	private static Logger log = Logger.getLogger(ZStackWriteThread.class);
	
	private OutputStream nicOut;
	
	/**
	 * 
	 * @param in The input stream corresponding to the XBee UART out for which to listen 
	 * for XBee API packets.
	 * 
	 */
	public ZStackWriteThread (OutputStream out) {
		super();
		this.nicOut = out;
	}
	
	public void run() {
		while (true) {
			// Pop last API frame from queue
			// Transmit to UART
			// Check tx rate and pause if necessary
			// Block if there are no more frames
		}
	}

}
