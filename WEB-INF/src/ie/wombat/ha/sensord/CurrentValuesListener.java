package ie.wombat.ha.sensord;


import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
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
public class CurrentValuesListener implements Runnable {
	
	public CurrentValuesListener() {
		System.err.println ("Starting socket listener2 " + this);
	}

	ServerSocket serverSocket;
	

	public void run() {
		
		
		// Create listener socket
		try {
			serverSocket = new ServerSocket(SensorDaemon.LISTEN_PORT2);
		} catch (IOException e) {
			System.err.println("Could not listen on port: " + SensorDaemon.LISTEN_PORT2);
			return;
		}
		
		// Listen for connections, create thread to handle each connection
		while (true) {
			try {
				Socket clientSocket = serverSocket.accept();
				//SocketServer ss = new SocketServer(clientSocket);
				
				OutputStreamWriter w = new OutputStreamWriter(clientSocket.getOutputStream());
				
				for (String key : SensorDaemon.lastReadingMap.keySet()) {
					//w.write("0.1 ");
					w.write(SensorDaemon.lastReadingMap.get(key).toString());
					w.write("\n");
				}
				
				w.flush();
				w.close();
				clientSocket.close();
				
			} catch (IOException e) {
				System.err.println("Accept failed: " + SensorDaemon.LISTEN_PORT2);
			}
		}

	}
	
	public synchronized void addSensorReading(SensorReading reading) {
		SensorDaemon.lastReadingMap.put(reading.sensorId, reading);
	}

}