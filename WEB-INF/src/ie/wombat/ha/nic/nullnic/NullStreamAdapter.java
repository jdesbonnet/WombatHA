package ie.wombat.ha.nic.nullnic;

import ie.wombat.ha.nic.APIFrameListener;
import ie.wombat.ha.nic.UARTAdapter;

import java.io.IOException;

import java.util.Date;

import org.apache.log4j.Logger;

/**
 * A null stream adapter
 * 
 * @author joe
 *
 */
public class NullStreamAdapter implements UARTAdapter, APIFrameListener {
	
	private static Logger log = Logger.getLogger(NullStreamAdapter.class);
	
	//private APIFrameListener nicListener;
	
	
	public void setName (String name) {
		//this.name = name;
	}
	
	public synchronized void txAPIFrame(byte[] apiFrameData, int frameLen) throws IOException {
		log.info("sendAPIFrame() to null");
	}
	
	

	public void setRxAPIFrameListener(APIFrameListener listener) {
		//nicListener = listener;
	}
	
	public void handleAPIFrame(byte[] apiFrame, int frameLen) {
		//nicListener.handleAPIFrame(apiFrame, frameLen);
		
	}

	public Date getExpiryTime() {
		return null;
	}

	public void setExpiryTime(Date expire) {
		// ignore
	}

	
	public void close () {
	
	}


	

}
