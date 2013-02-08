package ie.wombat.ha;

import ie.wombat.ha.nic.NICErrorListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;


public class NetworkMonitor extends Thread implements NICErrorListener {

	private static final Logger log = Logger.getLogger(NetworkMonitor.class);
	
	public List<HANetwork> networks = Collections.synchronizedList(new ArrayList<HANetwork>());
			
	public synchronized void addNetwork (HANetwork network) {
		networks.add(network);
		
		network.getNIC().addListener(this);
	}
	
	@Override
	public void run() {
		
			
			System.err.println ("NetworkMonitor is waiting...");
			try {
				wait();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		
	}

	public static void notifyThreadDeath (Thread t) {
		log.warn("Thread death: " + t);
	}

	@Override
	public Date getExpiryTime() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setExpiryTime(Date expire) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void handleNICError(ZigBeeNIC nic, int errorCode) {
		log.error ("NIC " + nic + " reported error " + errorCode);
		
	}


}
