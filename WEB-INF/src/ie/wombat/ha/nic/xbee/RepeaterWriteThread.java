package ie.wombat.ha.nic.xbee;

import ie.wombat.ha.ByteFormatUtils;
import ie.wombat.ha.nic.APIFrameListener;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Date;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;

import org.apache.log4j.Logger;

/**
 * This thread is responsible for listening to the local NIC traffic and writing XBee
 * API packets to the remote client.
 * 
 * @author joe
 *
 */
public class RepeaterWriteThread extends Thread implements APIFrameListener {

	private static Logger log = Logger.getLogger (RepeaterWriteThread.class);
	
	private static final int QUEUE_SIZE = 8;
	
	// All packets sent or received on NIC to be echoed to socket output stream
	private Queue<byte[]> outQueue = new ArrayBlockingQueue<byte[]>(QUEUE_SIZE);
	
	
	private XBeeDriver nic;
	private OutputStream out;
	
	public RepeaterWriteThread ( XBeeDriver nic, OutputStream out) {
		this.nic = nic;
		this.out = out;
		
		setDaemon(true);
		setName("XBeeRepeaterWrite");
		
		log.info (this + " created");
	}
	
	public void run ()  {
		try {
		
			mainLoop();
			
		} catch (IOException e) {
			e.printStackTrace();
		}

		// Remove as NIC listener before dying
		nic.removeAPIPacketListener(this);
		log.info("thread has ended");
	}
	
	private void mainLoop() throws IOException {
	
		nic.addAPIPacketListener(this);
		
		byte[] packet;
		
		while (true) {
			
			synchronized (outQueue) {
				packet = outQueue.poll();
			}
			
			if (packet == null) {
				// Wait for packet
				synchronized (this) {
					try {
						wait();
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
				continue;
			}
			
			log.info (this + " writing packet " + ByteFormatUtils.byteArrayToString(packet));
			
			out.write (XBeeUtil.encodeAPIFrame(packet, packet.length));
			out.flush();
			
		}
		
		
	}

	public Date getExpiryTime() {
		return null;
	}

	public void setExpiryTime(Date expire) {		
	}

	public void handleAPIFrame(byte[] packet, int packetLen) {
		// Expect this to be called by the NIC thread. Don't want 
		// to risk having this thread blocked by writing directly
		// to the socket. Instead write to queue and have the
		// RepeaterWriteThread write the data to the socket.
		
		log.info (this + " queuing packet " + ByteFormatUtils.byteArrayToString(packet, 0, packetLen));
		
		byte[] packetCopy = new byte[packetLen];
		System.arraycopy(packet, 0, packetCopy, 0, packetLen);
		synchronized (outQueue) {
			outQueue.add(packetCopy);
		}
		synchronized (this) {
			notifyAll();
		}
	}
	
}
