package ie.wombat.ha;

import ie.wombat.ha.server.Device;
import ie.wombat.ha.server.Observe;
import ie.wombat.ha.server.ObserveData;
import ie.wombat.zigbee.ZigBeeCommand;
import ie.wombat.zigbee.address.Address16;
import ie.wombat.zigbee.address.Address64;
import ie.wombat.zigbee.zcl.AttributeResponseDecode;
import ie.wombat.zigbee.zcl.AttributeValue;
import ie.wombat.zigbee.zcl.Cluster;
import ie.wombat.zigbee.zcl.Profile;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import org.apache.log4j.Logger;

/**
 * Listens for attributes being sent by devices configured to periodically
 * report.
 * 
 * @author joe
 *
 */
public class ReportAttributesListener implements ZigBeePacketListener {
	
	private static Logger log = Logger.getLogger(ReportAttributesListener.class);
	private static final boolean EN_THROTTLING = false;
	private static final boolean EN_HEATING_APP = false;

	private Map<Address16, Date> deviceLastReport = new HashMap<Address16,Date>();
	private Map<Address16, String> deviceLastValue = new HashMap<Address16, String>();
	/**
	 * {@inheritDoc}
	 */
	public Date getExpiryTime() {
		return null; // no expiry
	}

	public void setExpiryTime(Date expire) {
		// ignore
	}

	public void handleZigBeePacket(ZigBeePacket packet) {
		
		log.debug ("received ZigBee packet for evaluation: payload=" 
		+ ByteFormatUtils.byteArrayToString(packet.getPayload()));
		
		byte[] payload = packet.getPayload();
		
		if (payload.length < 3) {
			log.warn ("payload too short. Ignoring.");
			return;
		}
		
		if ( (payload[0] == 0x00 || payload[0] == 0x10) && payload[2] == 0x0a) {
			
			
			log.debug("Got AttributeReport (0x0a) packet");
			List<AttributeValue> list = AttributeResponseDecode.decode(payload, 3, payload.length, false);
			log.debug("Found " + list.size() + " attributes");
			
			
			if (EN_THROTTLING) {
				Date lastReportTime = deviceLastReport.get(packet.getSourceAddress16());
				if (lastReportTime != null && (System.currentTimeMillis() - lastReportTime.getTime()) < 120000L) {
					// Got a recent report
					// TODO: check if value has changed
					log.debug("Ignoring report as one from this device has already been recorded recently.");
					return;
				}
			}
			
			
			deviceLastReport.put(packet.getSourceAddress16(),new Date());
			
			// TODO: should I be creating the entity manager on every request?
			//EntityManagerFactory emf = Persistence.createEntityManagerFactory("ie.wombat.ha.server");
			//EntityManager em = emf.createEntityManager();
			EntityManager em = HibernateUtil.getEntityManager();
			em.getTransaction().begin();
		
			

			log.debug ("Looking for device record:");
			
			Device device;
			
			if (Address64.UNKNOWN.equals(packet.getSourceAddress64())) {
				log.debug("Unknown addr64. Trying to match device on addr16 (" + packet.getSourceAddress16() + ")");
				List<Device> devs = em.createQuery("from Device where address16=:addr16")
						.setParameter("addr16", 
						packet.getSourceAddress16().toString().toUpperCase()
						).getResultList();
				
				if (devs.size() == 0) {
					log.debug("Device " + packet.getSourceAddress16() + " not found");
					em.getTransaction().commit();
					//em.close();
					//emf.close();
					return;
				}
				device = devs.get(0);
				log.debug("Found device with addr16: " + device.getName());
			} else {
				log.debug("Retrieving device with addr64=" + packet.getSourceAddress64() );
				List<Device> devs = em.createQuery("from Device where address64=:addr64")
					.setParameter("addr64", 
					packet.getSourceAddress64().toString().toUpperCase()
					).getResultList();
			
				if (devs.size() == 0) {
					log.debug("Device " + packet.getSourceAddress64() + " not found");
					em.getTransaction().commit();
					//em.close();
					//emf.close();
					return;
				}
				device = devs.get(0);
				log.debug("Found device with addr64: " + device.getName());
			}
			
			log.debug ("deviceId=" + device.getId() + " device=" + device);
			
			// Update last contact time to now
			device.setLastContactTime(new Date());
			
			List<Observe> obs = em.createQuery("from Observe where device.id=:devId")
					.setParameter("devId", device.getId()).getResultList();
			if (obs.size() == 0) {
				log.debug("Observe record for deviceId=" + device.getId() + " not found for " + device);
				em.getTransaction().commit();
				//em.close();
				//emf.close();
				return;
			}
			Observe ob = obs.get(0);
			
			ObserveData obdata = new ObserveData();
			obdata.setObserve(ob);
			obdata.setData(list.get(0).getStringValue());
			
			// TODO: Hack heating app in for the moment
			if (EN_HEATING_APP && (ob.getId().longValue() == 2L) ) {
				try {
					ZigBeeNIC nic = HANetwork.getInstance(1L).getNIC();
					ZigBeeCommand zcmd = new ZigBeeCommand(nic);
					zcmd.setAddress16(new Address16("0B52"));
					zcmd.setAddress64(new Address64("10:00:00:50:C2:70:00:BD"));
					zcmd.setProfileId (Profile.HOME_AUTOMATION);
					zcmd.setClusterId (Cluster.ON_OFF);
					zcmd.setSourceEndpoint(10);
					zcmd.setDestinationEndpoint(1);
					zcmd.setSequenceId(ZigBeeCommand.AUTO_SEQUENCE);
					zcmd.setClusterSpecific(true);
					byte[] command = new byte[1];
					float t = new Float(obdata.getData()) / 100;
					System.err.println ("IN HEATING APP T=" +t);

					if (t < 22) {
						command[0] = 0x01;
						System.err.println ("HEATING ON");
						zcmd.setCommand(command);
						zcmd.exec();
					} else if (t > 24.5){
						command[0] = 0x00;
						System.err.println ("HEATING OFF");
						zcmd.setCommand(command);
						zcmd.exec();
					}
					
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
							
			em.persist(obdata);
			em.getTransaction().commit();
			//em.close();
			//emf.close();
			
			
			
		} else {
			log.debug ("Packet rejected");
		}
		
	}

}
