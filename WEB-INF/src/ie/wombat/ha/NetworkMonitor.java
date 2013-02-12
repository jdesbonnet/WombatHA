package ie.wombat.ha;

import ie.wombat.ha.nic.NICErrorListener;
import ie.wombat.ha.server.Network;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import javax.persistence.EntityManager;

import org.apache.log4j.Logger;

/**
 * All very experimental... and will change a lot.
 * @author joe
 *
 */
public class NetworkMonitor extends Thread implements NICErrorListener {

	private static final Logger log = Logger.getLogger(NetworkMonitor.class);
	
	public List<HANetwork> networks = Collections.synchronizedList(new ArrayList<HANetwork>());
	
	private Long lock = 0L;
	
	// TODO: this is wrong
	private HANetwork faultInNetwork;
	
	public NetworkMonitor () {
		// Set thread name
		setName ("NetworkMonitor");
	}
	
	public synchronized void addNetwork (HANetwork network) {
		networks.add(network);
		
		network.getNIC().addListener(this);
	}
	
	@Override
	public void run() {
		
		while (true) {
			
			System.err.println ("NetworkMonitor is sleeping... ");
			try {
				synchronized (lock) {
					lock.wait();
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			System.err.println ("NetworkMonitor AWAKE!!");
			
			networks.remove(faultInNetwork);
			
			System.err.println ("Need to restart HANetwork#" + faultInNetwork.getNetworkId());
			
			
			// Small delay first
			System.err.println ("Delay to allow things to settle");
			try {
				Thread.sleep(5000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			
			//HAStarter.startNetwork(network);
			EntityManager em = HibernateUtil.getEntityManager();
			em.getTransaction().begin();
			Network networkRecord = em.find(Network.class, faultInNetwork.getNetworkId());
			//addNetwork(HANetwork.createNetwork(networkRecord));
			addNetwork(HAStarter.startNetwork(networkRecord));
			em.getTransaction().commit();
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
		// Which network?
		Collection<HANetwork> allNetworks = HANetwork.getAllNetworks();
		faultInNetwork = null;
		for (HANetwork network : allNetworks) {
			if (network.getNIC() == nic) {
				log.info("Error in HANetwork#" + network.getNetworkId());
				faultInNetwork = network;
			}
		}
		log.error ("NIC " + nic + " reported error " + errorCode + ". Attempting to wake monitor thread.");
		synchronized (lock) {
			lock.notifyAll();
		}
	}


}
