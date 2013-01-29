package ie.wombat.ha.nic.xbee;

import ie.wombat.ha.ByteFormatUtils;
import ie.wombat.ha.HANetwork;
import ie.wombat.ha.nic.APIFrameListener;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Date;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;

/**
 * Note 31 July 2011: Might not be needed any more. Connecting NIC directly
 * to repeater service (by instantiating the NIC with a TCP adapter pointing
 * to the repeater service). Seems to work.
 * 
 * 
 * NIC bridge for testing. This is used in conjunction with {@link RepeaterServerThread}
 * to allow a development instance of the server software to test against a real network.
 * As I only have enough hardware to have one real network, and I want to have the real
 * network tested continuously on a server running on a stable version of the server
 * software running on an EC2 instance I need some way
 * for the development server to bridge to the Zigbee network.
 * 
 * The repeater's function is not to relay all packets going to/from the Zigbee NIC. It
 * only has to accept outgoing packets from the development system and queue those to
 * the XBee NIC. It returns traffic transmitted by the XBee UART.
 * 
 * 
 * To avoid loops will only accept remote packets that are
 * emitted by a XBee UART and will not transmit any packets
 * that do not originate from the application layer. Specifically
 * type 0x91 packets will not be transmitted and 0x11 packets
 * will be dropped on reception.
 * 
 * Allows testing of software on computer against a real 
 * network running remotely.
 * 
 * 
 * @author joe
 *
 */
public class TCPRelay extends Thread implements APIFrameListener {

	private static Logger log = Logger.getLogger(TCPRelay.class);
	
	private OutputStream out;
	private InputStream in;
	private String hostname;
	private int port;
	private Long networkId;
	
	
	public TCPRelay (String hostname, int port, Long networkId) {
		this.hostname = hostname;
		this.port = port;
		this.networkId = networkId;
		setDaemon(true);
	}
	
	public void run () {
		
		while (true) {
			try {
				mainLoop();
			} catch (Exception e) {
				log.error(e);
				e.printStackTrace();
			}
			log.warn ("Socket problem. Reopening socket after short delay.");
			
			// Incase there is a problem delay before reopening to avoid
			// swamping server.
			try {
				Thread.sleep(30000);
			} catch (InterruptedException e) {
				// ignore
			}

		}
		
	}

	
	public void mainLoop() throws Exception {
	
		// This is the local 'virtual' NIC. Packets sent to the NIC will be
		// transmitted to the remote server for relay to the real NIC.
		XBeeDriver xbee = (XBeeDriver)HANetwork.getInstance(networkId).getNIC();
		
		log.info ("entering mainLoop()");
		byte[] packet = new byte[512];
		
		// Open socket to remote server
		log.info ("opening socket");
		
		// EC2 server 67.202.42.132
		Socket socket = new Socket(hostname,port);
		in = socket.getInputStream();
		out = socket.getOutputStream();

		
		// Register this object as a listener for API packets. Normally
		// only packets from XBee->Server sent to listeners, but added
		// experimental feature recently that would also send Server->XBee
		// packets. Wondering is this really a good idea.
		xbee.addAPIPacketListener(this);
		
		int packetLen;
		int packetType;
		
		// Loop forever polling server. If packet is received during a sleep
		// period ... oh wait.. crap .. threading issue here. We really should
		// queue packets.
		while (true) {
			// Read packet from socket
			packetLen = XBeeUtil.readAPIFrameFromStream(in, packet);
			packetType = packet[0] & 0xff;
			if (packetType == 0x11) {
				log.info ("Remote->Local: dropping packet type 0x11 as it can only originate from an application");
				continue;
			}
			
			// Now should only have 0x91 (Zigbee receive) and 0x8B (ACK) packets.
			log.info ("Remote->Local: " + ByteFormatUtils.byteArrayToString(packet,0,packetLen));
			// Inject in local virtual NIC
			xbee.handleAPIFrame(packet, packetLen);
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
	 * OR (experimental) when the application sends a packet to the XBee. This
	 * latter case is what we're interested in. We can ignore the 0x91 packets. But
	 * as there is no real XBee on this local development server we should not 
	 * see 0x91 type packets anyway. 
	 * 
	 * TODO: Should the packet be stored and the main loop perform the task instead? 
	 * This seems cleaner. Right now the NIC thread will be performing the HTTP POST
	 * which may take considerable time and lock up the NIC.
	 */
	public void handleAPIFrame(byte[] packet, int packetLen) {
		
		
		int packetType = packet[0] & 0xff;
		
		// This can only be an echo from a remote packet injected by the relay.
		// Don't want it going back out. Drop it.
		if (packetType == 0x91) {
			//log.debug ("Local->Remote: not relaying packet type 0x91 as it can only be source from a real XBee");
			return;
		}
		if (packetType == 0x8B) {
			//log.info ("Local->Remote: dropping packet type 0x8B as it can only originate from a real XBee");
			return;
		}
		log.info("Local->Remote: " + ByteFormatUtils.byteArrayToString(packet,0,packetLen));

		try {
			XBeeUtil.writeAPIFrameToStream(packet, packetLen, out);
			out.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * This is for testing.
	 * 
	 * @param arg
	 */
	public static void main (String[] arg) throws Exception {
		//TCPRelay relay = new TCPRelay (arg[0], Integer.parseInt(arg[1]));
		//relay.start();
		// EC2 server 67.202.42.132
		String hostname = arg[0];
		int port = Integer.parseInt(arg[1]);
		
		Socket socket = new Socket(hostname,port);
		InputStream in = socket.getInputStream();
		OutputStream out = socket.getOutputStream();
		
		String packetStr = "11 09 10 00 00 50 c2 10 00 63 20 40 0a 01 00 00 01 04 00 00 10 1a 00 00 00 01 00 02 00 03 00 04 00 05 00 06 00 07 00";
		byte[] packet = ByteFormatUtils.stringToByteArray(packetStr);
		System.err.println ("packet=" + packet);
		System.err.println ("packetLen=" + packet.length);
		while (true) {
			System.err.println ("Writing " + packetStr);
			XBeeUtil.writeAPIFrameToStream(packet, packet.length, out);
			out.flush();
			
			Thread.sleep(5000);
		}
	}
}
