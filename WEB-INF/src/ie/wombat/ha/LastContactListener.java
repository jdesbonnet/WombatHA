package ie.wombat.ha;


import java.util.Date;
import java.util.HashMap;
import java.util.List;


import javax.persistence.EntityManager;
import org.apache.log4j.Logger;
import ie.wombat.ha.server.Device;
import ie.wombat.ha.server.Network;
import ie.wombat.zigbee.address.Address16;

/**
 * Update last contact timestamp for each device in the database
 * 
 * TODO: is this really necessary?
 * 
 * @author joe
 *
 */
public class LastContactListener implements ZigBeePacketListener  {

	private static Logger log = Logger.getLogger(LastContactListener.class);
	private Long networkId;
	
	
	private HashMap<Address16,Date> lastContactTime = new HashMap<Address16,Date>();

	
	public LastContactListener (Long networkId) {
		this.networkId = networkId;
	}

	public Date getExpiryTime() {
		// TODO Auto-generated method stub
		return null;
	}

	public void setExpiryTime(Date expire) {
		// TODO Auto-generated method stub
		
	}

	public void handleZigBeePacket(ZigBeePacket packet) {
		Address16 addr16 = packet.getSourceAddress16();
		String addr16str = addr16.toString().toUpperCase();
		log.debug("Updating lastContactTime of " + addr16);
		lastContactTime.put(addr16,new Date());
		
		EntityManager em = HibernateUtil.getEntityManager();
		em.getTransaction().begin();
		
		List<Device> deviceRecords = em
			.createQuery("from Device where address16=:addr16 and network.id=:networkId")
			.setParameter("addr16", addr16str)
			.setParameter("networkId", networkId)
			.getResultList();
		
		log.debug ("Found " + deviceRecords.size() + " matching " + addr16str + " on networkId=" + networkId);
		if (deviceRecords.size() != 1) {
			log.warn ("Could not find device for addr16 " + addr16);
		} else {
			log.debug ("Setting new time for " + addr16);
			Device device = deviceRecords.get(0);
			device.setLastContactTime(new Date());
		}
		
		em.getTransaction().commit();
		
	}
	
}
