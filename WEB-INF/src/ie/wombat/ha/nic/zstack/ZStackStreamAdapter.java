package ie.wombat.ha.nic.zstack;

import ie.wombat.ha.ByteFormatUtils;
import ie.wombat.ha.nic.APIFrameListener;
import ie.wombat.ha.nic.UARTAdapter;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;

import org.apache.log4j.Logger;

/**
 * A stream IO interface to the NIC UART. Suitable for NIC on local USB bus or 
 * available via ser2net on network.
 * 
 * TODO: this could be a super class for a SIOAdapter and TCPAdapter.
 * 
 * @author joe
 *
 */
public class ZStackStreamAdapter implements UARTAdapter, APIFrameListener {
	
	private static Logger log = Logger.getLogger(ZStackStreamAdapter.class);
	
	private InputStream in;
	private OutputStream out;
	
	/**
	 * A thread listens to the nicIn input stream, triggering callbacks when an API frame arrives.
	 */
	private ZStackReadThread readThread;
	
	private APIFrameListener nicListener;
	
	public ZStackStreamAdapter (InputStream in, OutputStream out) {
		this.in = in;
		this.out = out;
		
		readThread = new ZStackReadThread(in);
		
		// TODO: I would like to add the network ID to the thread name
		readThread.setName("NICRead");
		
		// We only require this thread to exist while the driver is in existence
		readThread.setDaemon(true);
		
		readThread.addListener(this);
		
		readThread.start();
		
		
	}

	public void setName (String name) {
		readThread.setName(name);
	}
	
	public synchronized void txAPIFrame(byte[] apiFrameData, int frameLen) throws IOException {
		log.info("txAPIFrame() Sending escaped API frame len=" + frameLen + " on the 'wire' using OutputStream " + out);
		log.debug ("TX: " + ByteFormatUtils.byteArrayToString(apiFrameData, 0, frameLen));
		out.write(apiFrameData,0,frameLen);
		out.flush();
	}
	
	

	public void setRxAPIFrameListener(APIFrameListener listener) {
		nicListener = listener;
	}
	
	public void handleAPIFrame(byte[] apiFrame, int frameLen) {
		nicListener.handleAPIFrame(apiFrame, frameLen);
		
	}

	public Date getExpiryTime() {
		return null;
	}

	public void setExpiryTime(Date expire) {
		// ignore
	}

	
	public void close () {
		try {
			in.close();
			out.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}


	

}
