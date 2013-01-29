package ie.wombat.ha.sensord;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;

class BroadcastServerThread implements Runnable {
	
	private static final int QUEUE_SIZE = 8;
	
	private Socket socket;
	
	Queue<String> packetQueue = new ArrayBlockingQueue<String>(QUEUE_SIZE);
	
	volatile boolean active = false;
	
	public BroadcastServerThread(Socket socket) {
		this.socket = socket;
	}

	public void run() {
		active = true;
		try {
			loop();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		active = false;
	}
	
	private synchronized void loop () throws IOException {
	
		OutputStreamWriter w = new OutputStreamWriter(socket.getOutputStream());
		
		String p;
		
		while (true) {
			
			// wait for new data
			try {
				wait();
			} catch (InterruptedException e) {
				// ignore
			}
			
			// Empty what ever is in queue to socket
			while ( (p = popPacket()) != null ) {
				w.write(p);
				w.write("\n");
			}
			w.flush();
		}
	}
	
	public synchronized void queuePacket (String s) {
		synchronized (packetQueue) {
			packetQueue.offer(s);
		}
		notifyAll();
	}
	public String popPacket () {
		synchronized (packetQueue) {
			return packetQueue.poll();
		}
	}
	public boolean isClosed () {
		return  ! active;
	}
}