package ie.wombat.ha.sensord;


import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Thread that listens for incoming connections, creates thread to handle the
 * connection. TODO: handle connection disconnect.
 * 
 * @author joe
 *
 */
public class BroadcastServerListener implements Runnable {
	
	private List<BroadcastServerThread> clients = Collections.synchronizedList(new ArrayList<BroadcastServerThread>());
	
	public BroadcastServerListener() {
		System.err.println ("Starting socket listener " + this);
	}

	ServerSocket serverSocket;
	
	private String[] packets = new String[8];
	int lastPacketIndex = 0;
	

	public void run() {
		
		int i;
		
		// Create listener socket
		try {
			serverSocket = new ServerSocket(SensorDaemon.LISTEN_PORT);
		} catch (IOException e) {
			System.err.println("Could not listen on port: " + SensorDaemon.LISTEN_PORT);
			return;
		}
		
		// Listen for connections, create thread to handle each connection
		while (true) {
			try {
				Socket clientSocket = serverSocket.accept();
				BroadcastServerThread ss = new BroadcastServerThread(clientSocket);
				clients.add(ss);
				
				Thread t = new Thread(ss);
				t.start();
				
				// send last few packets received
				for (i = 0; i < packets.length; i++) {
					String p = packets[ (lastPacketIndex+i) % packets.length];
					if (p != null) {
						ss.queuePacket(p);
					}
				}
				
				
				
			} catch (IOException e) {
				System.err.println("Accept failed: 4444");
			}
		}

	}
	
	/**
	 * Send packet to all connections. Check for closed connections and 
	 * remove them also.
	 * @param s
	 */
	public void broadcastPacket (String s) {
		
		//System.err.println ("broadcasting " + s);
		
		
		this.packets[lastPacketIndex] = s;
		lastPacketIndex++;
		if (lastPacketIndex == packets.length) {
			lastPacketIndex = 0;
		}
		
	
		synchronized (clients) {
			int n = clients.size();
			for (int i = 0; i < n; i++) {
				BroadcastServerThread ss = clients.get(i);
				if (ss.isClosed()) {
					System.err.println ("removing " + ss);
					clients.remove(ss);
				} else {
					ss.queuePacket(s);
				}
			}
		}
		
		//System.err.println ("done");
		
	}
}