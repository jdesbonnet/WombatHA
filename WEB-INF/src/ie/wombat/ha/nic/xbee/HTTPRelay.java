package ie.wombat.ha.nic.xbee;

import ie.wombat.ha.ByteFormatUtils;
import ie.wombat.ha.nic.APIFrameListener;
import ie.wombat.ha.nic.FoneraServlet;

import java.io.IOException;
import java.util.Date;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;

/**
 * This is a standalone program that will emulate the operation of the
 * the XBee HTTP relay software that runs in the Gateway (Fonera). Used
 * for testing {@link FoneraServlet}
 * 
 * Command line parameters: 
 * Serial IO Device or hostname
 * Baudrate (if SIO device) or port number
 * Service URL
 * 
 * 
 * @author joe
 *
 */
public class HTTPRelay implements APIFrameListener {

	private static Logger log = Logger.getLogger(HTTPRelay.class);
	
	private XBeeDriver nic;
	private String serviceURL;
	private HttpClient client;
	
	private long lastPollTime = 0;
	
	public static void main (String[] arg) throws IOException {
		
		// Configure log4j logging
		BasicConfigurator.configure();
		
		XBeeDriver nic = new XBeeDriver();
		
		if (arg[0].startsWith("/")) {
			log.info ("Opening connection to XBee on local serial port " + arg[0] + " speed " + arg[1]);
			XBeeDriverFactory.setSIOPort(nic,arg[0], Integer.parseInt(arg[1]));
		} else {
			log.info ("Opening connection to XBee on TCP socket on host " + arg[0] + " port " + arg[1]);
			XBeeDriverFactory.setTCPPort(nic,arg[0], Integer.parseInt(arg[1]));
		}
		
		HTTPRelay me = new HTTPRelay(nic,arg[2]);
		me.run();
		
		log.info("Done.");
	}
	
	public HTTPRelay (XBeeDriver driver, String serviceURL) {
		this.nic = driver;
		this.serviceURL = serviceURL;
	}
	
	public synchronized void run () {
		
		while (true) {
			try {
				mainLoop();
			} catch (Exception e) {
				e.printStackTrace();
				try {
					Thread.sleep(5000);
				} catch (InterruptedException e1) {
				
				}
			}
		}
		
	}
	
	private void mainLoop () throws Exception {
		client = new HttpClient();
		
		// Register this class as a listener for packets being transmitted
		// but the XBee module's UART. handleXBeeAPIPacket() will be called.
		nic.addAPIPacketListener(this);
		
		// Loop forever polling server. If packet is received during a sleep
		// period ... oh wait.. crap .. threading issue here. We really should
		// queue packets.
		while (true) {
			
			if ( (System.currentTimeMillis() - lastPollTime) > 5000) {
				log.info ("Polling with empty payload");
				handleAPIFrame(null, 0);
			}
			
			try {
				wait(2000);
			} catch (InterruptedException e) {
			}
			
		}
	}

	public Date getExpiryTime() {
		return null;
	}

	public void setExpiryTime(Date expire) {
		// ignore
	}

	/**
	 * This will be invoked by the XBee NIC (which we registered as a listener) 
	 * whenever a packet is transmitted by the XBee's UART.
	 * 
	 * TODO: Should the packet be stored and the main loop perform the task instead? 
	 * This seems cleaner. Right now the NIC thread will be performing the HTTP POST
	 * which may take considerable time and lock up the NIC.
	 */
	public void handleAPIFrame(byte[] packet, int packetLen) {
		
		if (packet == null) {
			log.debug("ZigBee->Server: no packets. Poll for Server->Zigbee");
		} else {
			log.debug("ZigBee->Server: " + ByteFormatUtils.byteArrayToString(packet,0,packetLen));
		}
		
		// Construct packet payload
		StringBuffer buf = new StringBuffer();		
		if (packetLen > 0) {
			for (int i = 0; i < packetLen; i++) {
				buf.append(ByteFormatUtils.formatHexByte(packet[i]));
			}
			buf.append("\r\n");
		}
		
		
		// Send packets to server by HTTP POST
		
		PostMethod post = new PostMethod(serviceURL);
		
		post.setRequestBody(buf.toString());
				
		try {
			int status = client.executeMethod(post);
			log.info ("HTTP status=" + status);
			
			// Anything not 2xx is an error.
			if (status > 299) {
				log.error ("Server error, HTTP status code " + status);
				return;
			}
		} catch (HttpException e) {
			e.printStackTrace();
			log.error (e);
		} catch (IOException e) {
			e.printStackTrace();
			log.error (e);
		}
		
		// Parse response from server and send any packets to XBee UART
		try {
			String response = post.getResponseBodyAsString();
			log.debug ("Server->Zigbee: " + response + " (len=" + response.length() +")");
			String[] lines = response.split("\r\n");
			//log.debug ("response contains " + lines.length + " lines");
			
			for (int i = 0; i < lines.length; i++) {
				String line = lines[i].replaceAll(" ", "");
				if (line.length()==0) {
					continue;
				}
				byte[] bytes = new byte[line.length()/2];
				for (int j = 0; j < bytes.length; j++) {
					bytes[j] = (byte)Integer.parseInt( line.substring(j*2,j*2+2) ,16);
				}
				nic.sendAPIFrame(bytes, bytes.length);
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		post.releaseConnection();
		
		lastPollTime = System.currentTimeMillis();
		
		
	}
}
