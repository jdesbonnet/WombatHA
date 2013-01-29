package ie.wombat.ha.ui.server;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import org.apache.log4j.Logger;

import ie.wombat.ha.HANetwork;
import ie.wombat.ha.HibernateUtil;
import ie.wombat.ha.HibernateUtilOld;
import ie.wombat.ha.devices.DeviceDriver;
import ie.wombat.ha.server.Device;
import ie.wombat.ha.ui.client.DeviceInfo;
import ie.wombat.ha.ui.client.GetDevicesService;
import ie.wombat.ha.ui.client.IdName;
import ie.wombat.zigbee.address.Address16;
import ie.wombat.zigbee.address.Address64;

import com.google.gwt.user.server.rpc.RemoteServiceServlet;

/**
 * The server side implementation of the RPC service.
 */
@SuppressWarnings("serial")
public class GetDevicesServiceImpl extends RemoteServiceServlet implements
		GetDevicesService {

	private static Logger log = Logger.getLogger(GetDevicesServiceImpl.class);
	
	public DeviceInfo[] getDevices(Long networkId) throws IllegalArgumentException {
		
		log.info ("getDevices(networkId=" + networkId + ")");
		
		/*
		EntityManager em = HibernateUtil.getEntityManager();
		em.getTransaction().begin();
		List<Device> devices = em
				.createQuery("from Device where network.id=:networkId order by id")
				.setParameter("networkId", networkId)
				.getResultList();
		*/
		HANetwork network = HANetwork.getInstance(networkId);
		List<DeviceDriver> devices = network.getDevices();
		
		DeviceInfo[] ret = new DeviceInfo[devices.size() + 1];
		
		DeviceInfo coord = new DeviceInfo();
		coord.id=new Long(0L);
		coord.networkId = networkId;
		coord.name = "Coordinator";
		coord.addr16 = Address16.COORDINATOR.toString();
		coord.addr64 = Address64.COORDINATOR.toString();
		
		ret[0] = coord;
		
		int i = 1;
		for (DeviceDriver device : devices) {
			//IdName idname = new IdName();
			//idname.id = device.getId().toString();
			//idname.name = device.getName();
			DeviceInfo devInfo = new DeviceInfo();
			devInfo.id = device.getId();
			devInfo.networkId = networkId;
			devInfo.name = device.getName();
			devInfo.addr64 = device.getAddress64().toString();
			if (device.getAddress16() != null) {
				devInfo.addr16 = device.getAddress16().toString();
			}
			devInfo.batteryStatus = device.getBatteryStatus();
			devInfo.batteryPowered = device.isBatteryPowered();
			devInfo.lastRxTime = System.currentTimeMillis() - device.getLastRxTime();
			
			ret[i++] = devInfo;
		}
		//em.getTransaction().commit();
		//em.close();
		//emf.close();
		
	
		return ret;
	}

	public DeviceInfo getDeviceInfo(Long deviceId)
			throws IllegalArgumentException {
		
		if (deviceId == 0) {
			DeviceInfo coord = new DeviceInfo();
			coord.id=new Long(0L);
			coord.name = "Coordinator";
			coord.addr16 = Address16.COORDINATOR.toString();
			coord.addr64 = Address64.COORDINATOR.toString();
			return coord;
		}
		
		//EntityManagerFactory emf = Persistence.createEntityManagerFactory("ie.wombat.ha.server");
		//EntityManager em = emf.createEntityManager();
		EntityManager em = HibernateUtil.getEntityManager();
		em.getTransaction().begin();
		Device device = (Device)em.find(Device.class, deviceId);
		if (device == null) {
			log.error ("Device#" + deviceId + " not found");
		}
		DeviceInfo deviceInfo = new DeviceInfo();
		deviceInfo.id = device.getId();
		deviceInfo.name = device.getName();
		deviceInfo.addr64 = device.getAddress64().toString();
		
		em.getTransaction().commit();
		//em.close();
		//emf.close();
		
		return deviceInfo;
		
	}

	/*
	public String getDeviceAddr16(Long deviceId)
			throws IllegalArgumentException {
		
		if (deviceId == 0) {
			return "0000";
		}
		
		EntityManagerFactory emf = Persistence.createEntityManagerFactory("ie.wombat.ha.server");
		EntityManager em = emf.createEntityManager();
		em.getTransaction().begin();
		Device device = (Device)em.find(Device.class, deviceId);
		String addr64 = device.getAddress64();
		em.getTransaction().commit();
		em.close();
		emf.close();
		
		// TODO: Do ZDO request to get Addr16
		
		return "????";
	}
	*/
	
}
