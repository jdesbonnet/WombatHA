package ie.wombat.ha.app.devicemonitor;

import java.io.IOException;
import java.util.List;

import javax.persistence.EntityManager;

import ie.wombat.ha.HibernateUtil;
import ie.wombat.ha.app.AppBase;
import ie.wombat.ha.devices.DeviceDriver;
import ie.wombat.ha.server.Device;
import ie.wombat.ha.ui.server.AddressServiceImpl;
import ie.wombat.zigbee.address.Address16;


import org.apache.log4j.Logger;

/**
 * Retrieve current network address (addr16). Update system if it has changed since the last poll. 
 * 
 * @author Joe Desbonnet, jdesbonnet@gmail.com
 *
 */
public class TaskPollAddress16 implements Runnable {

	private static final boolean EN_RESET_BEFORE_QUERY = false;
	
	private static Logger log = Logger.getLogger(TaskPollAddress16.class);
	private AppBase app;
	
	private int deviceIndex = 0;
	private AddressServiceImpl addrService = new AddressServiceImpl();

	public TaskPollAddress16(AppBase app) {
		this.app = app;
	}
	
	public void run() {
		Long networkId = app.getNetwork().getNetworkId();
		String logPrefix = "Network#" + networkId + ": ";
	
		List<DeviceDriver> devices = app.getNetwork().getDevices();
		
		// Get next device on round robin
		DeviceDriver device = devices.get(deviceIndex++ % devices.size());
		
		if (EN_RESET_BEFORE_QUERY) {
			log.debug("Resetting NIC before issuing ZDO request. Having trouble with crashing UBee!");
			try {
				app.getNetwork().getNIC().reset();
			} catch (IOException e) {
				log.error("Error resetting NIC: " + e);
			}

			try {
				Thread.sleep(2000);
			} catch (InterruptedException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}
		
		
		log.debug(logPrefix + "Attempting to resolve addr16 for " + device.getAddress64());
		String addr16 = addrService.getDeviceAddr16(networkId, device.getAddress64().toString());
		log.debug(logPrefix + "Response to resolving " + device.getAddress64() + " is " + addr16);
		if (addr16.length() != 4) {
			log.error (logPrefix + "expected addr16 to be 4 digits in length");
			return;
		}
		try {
			Integer.parseInt(addr16,16);
		} catch (NumberFormatException e) {
			log.error (logPrefix + "addr16 fails to parse: " + addr16 + " not hex");
			return;
		}
		
		Address16 newAddr16 = new Address16(addr16);
		
		if (! newAddr16.equals(device.getAddress16())) {
			log.warn (logPrefix + "detected a new addr16 for device " + device.getAddress64() 
					+ " oldAddr16=" + device.getAddress16() + " newAddr16=" + newAddr16
					+ ". Updating."
					);
			device.setAddress16(newAddr16);
			
			// Update device addr16 in database
			EntityManager em = HibernateUtil.getEntityManager();
			em.getTransaction().begin();
			List<Device> list = em.createQuery("from Device where network.id=:networkId and address64=:addr64)")
					.setParameter("networkId",networkId)
					.setParameter("addr64", device.getAddress64().toString())
					.getResultList();
			if (list.size() == 1) {
				list.get(0).setAddress16(newAddr16.toString());
				em.getTransaction().commit();
			} else {
				log.error (logPrefix + "Expecting exactly one record to match networkId=" + networkId + " addr64=" 
						+ device.getAddress64().toString() + " but found " + list.size());				
			}
			em.getTransaction().commit();
			
		} else {
			log.debug ("addr16 of device " + device.getAddress64() + " remains " + device.getAddress16());
		}
		
	}

}
