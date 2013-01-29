package ie.wombat.ha;

import ie.wombat.ha.server.Device;
import ie.wombat.ha.ui.server.AddressServiceImpl;

import java.util.Date;
import java.util.HashMap;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import org.apache.log4j.Logger;

public class NetworkEnumerator {

	private static final Logger log = Logger.getLogger(NetworkEnumerator.class);

	public static void enumerate(HANetwork network) {

		ZigBeeNIC nic = network.getNIC();
		
		EntityManagerFactory emf = Persistence
				.createEntityManagerFactory("ie.wombat.ha.server");
		EntityManager em = emf.createEntityManager();
		em.getTransaction().begin();

		List<Device> devices = em.createQuery("from Device where address16=null order by id")
				.getResultList();

		log.info("Enumerating network");
		HashMap<String, String> addr64to16 = new HashMap<String, String>();
		AddressServiceImpl addrService = new AddressServiceImpl();
		// for (String addr64 : devicesAddr64) {
		for (Device device : devices) {
			log.info("requesting addr16 for " + device.getName());
			String addr16str = addrService.getDeviceAddr16(network.getNetworkId(),device
					.getAddress64());
			if (addr16str.startsWith("ZDP_")) {
				// error condition
				if (device.getLastContactTime() != null
						&& (System.currentTimeMillis()
								- device.getLastContactTime().getTime() > 3600000L)) {
					device.setAddress16(null);
				}
			} else {
				device.setAddress16(addr16str.toUpperCase());
				device.setLastContactTime(new Date());
			}

			addr64to16.put(device.getAddress64(), addr16str);
		}
		log.info("***************************** Enumeration done.");
		for (String addr64 : addr64to16.keySet()) {
			log.info("    " + addr64 + "=" + addr64to16.get(addr64));
		}

		em.getTransaction().commit();
		em.close();
		emf.close();
	}
}
