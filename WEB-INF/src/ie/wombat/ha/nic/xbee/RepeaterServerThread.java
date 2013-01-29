package ie.wombat.ha.nic.xbee;


import ie.wombat.ha.HANetwork;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;


import org.apache.log4j.Logger;

/**
 * Listens for incoming connections to repeater service. The repeater service relays incoming and outgoing XBee API packets
 * to the connected client. The primary motivation for this service is development and debugging. It means that I can have a 
 * version of this software running in a production setting and still be able to have a development system interact with the
 * production system. 
 * 
 * The socket acts the exact same way as the Fonera XBee serial bridge. 
 * 
 * @author joe
 *
 */
public class RepeaterServerThread extends Thread {
	
	private static Logger log = Logger.getLogger (RepeaterServerThread.class);
	
	private Long networkId;
	
	public RepeaterServerThread(Long networkId) {
	
		this.networkId = networkId;
		
		setDaemon(true);
		setName("RepeaterServer");
		
		log.info (this + " created");

	}

	private ServerSocket serverSocket;

	public void run() {
		
		XBeeDriver nic = (XBeeDriver)HANetwork.getInstance(networkId).getNIC();
		
		// Create listener socket
		try {
			serverSocket = new ServerSocket(2002);
		} catch (IOException e) {
			System.err.println("Could not listen on port: " + 2002);
			return;
		}
		
		// Listen for connections, create thread to handle each connection
		while (true) {
			try {
				log.info("Received incoming connection. Creating R/W threads.");
				Socket clientSocket = serverSocket.accept();
				InputStream in = clientSocket.getInputStream();
				OutputStream out = clientSocket.getOutputStream();
				RepeaterWriteThread rwt = new RepeaterWriteThread(nic,out);
				RepeaterReadThread rrt = new RepeaterReadThread(nic,in);
				rwt.start();
				rrt.start();
			} catch (IOException e) {
				System.err.println("Accept failed: 2002");
			}
		}

	}
	
	
}