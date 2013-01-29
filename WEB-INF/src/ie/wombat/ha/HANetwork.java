package ie.wombat.ha;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ScheduledThreadPoolExecutor;

import javax.persistence.EntityManager;

import org.apache.log4j.Logger;



import gnu.io.PortInUseException;
import gnu.io.SerialPort;
import ie.wombat.ha.app.AppBase;
import ie.wombat.ha.devices.DeviceDriver;
import ie.wombat.ha.devices.DeviceFactory;

import ie.wombat.ha.nic.ServletAdapter;
import ie.wombat.ha.nic.UARTAdapter;

import ie.wombat.ha.nic.nullnic.NullDriver;
import ie.wombat.ha.nic.xbee.XBeeDriver;

import ie.wombat.ha.nic.xbee.XBeeStreamAdapter;
import ie.wombat.ha.nic.zstack.ZStackStreamAdapter;
import ie.wombat.ha.nic.zstack.ZStackDriver;
import ie.wombat.ha.server.Application;
import ie.wombat.ha.server.Device;
import ie.wombat.ha.server.LogRecord;
import ie.wombat.ha.server.Network;
import ie.wombat.ha.sio.SIOUtil;
import ie.wombat.ha.ui.server.AddressServiceImpl;

import ie.wombat.zigbee.ZDPRequest;
import ie.wombat.zigbee.ZDPResponseListener;
import ie.wombat.zigbee.address.Address16;
import ie.wombat.zigbee.address.Address64;
import ie.wombat.zigbee.zcl.Cluster;
import ie.wombat.zigbee.zdo.Addr16Response;
import ie.wombat.zigbee.zdo.ZDPStatus;
import ie.wombat.zigbee.zdo.ZDPUtil;

/**
 * Object that represents the ZigBee network. Most objects of interest can be
 * accessed through this object.
 * 
 * @author joe
 *
 */
public class HANetwork {

	private static Logger log = Logger.getLogger(HANetwork.class);

	private static HashMap<Long, HANetwork> instanceHash = new HashMap<Long,HANetwork>();
	
	//private static HANetwork instance = new HANetwork ();
	private ZigBeeNIC nic = null;
	private Long networkId;
	
	
	
	private HashMap<Address64,Address16> addr64ToAddr16Cache = new HashMap<Address64,Address16>();
	private HashMap<Address16,Address64> addr16ToAddr64Cache = new HashMap<Address16,Address64>();
	
	private HashMap<Address64, DeviceDriver> addr64ToDeviceDriverHash = new HashMap<Address64,DeviceDriver>();
	private List<AppBase> applications = new ArrayList<AppBase>();
	
	NetworkDiscoveryListener networkDiscoveryListener = null;
	
	private ScheduledThreadPoolExecutor stpe;
	
	private HANetwork (Network networkRecord) {
		
		Long networkId = networkRecord.getId();
		
		if (networkId == 4L) {
			return;
		}
		
//if (networkId == null) {
	//log.warn ("Attempting to create HANetwork with networkId==null. Using 1L as default network ID.");
	//networkId = 1L;
//}
		log.info("Creating HANetwork for networkId=" + networkId);
		this.networkId = networkId;
		
		// Load configuration file
		File configFile = new File ("/var/tmp/ha.properties");
		Properties properties = new Properties();
		try {
			properties.load(new FileInputStream(configFile));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		String[] p = networkRecord.getNicDriver().split(":");
		String nicHardware = p[0];
		String nicIOAdapter = "http";
		if (p.length > 1) {
			nicIOAdapter = p[1];
		}
		
		try {
			
			
			// How do we communicate with the NIC?
			UARTAdapter uartAdapter = null;
			if ("http".equals(nicIOAdapter)) {
				uartAdapter = new ServletAdapter();
			} else if ("tcp".equals(nicIOAdapter)) {
				String nicIOHost = p[2];
				int nicIOPort = Integer.parseInt(p[3]);
				Socket sock = new Socket(nicIOHost, nicIOPort);
				InputStream sin = sock.getInputStream();
				OutputStream sout = sock.getOutputStream();
				if ("ie.wombat.ha.nic.zstack.ZStackDriver".equals(nicHardware)) {
					uartAdapter = new ZStackStreamAdapter(sin, sout);
				} else if ("ie.wombat.ha.nic.xbee.XBeeDriver".equals(nicHardware)) {
					uartAdapter = new XBeeStreamAdapter(sin, sout);
				}
			} else if ("sio".equals(nicIOAdapter)) {
				/*
				String nicDeviceName = properties.getProperty("nicDeviceName","/dev/ttyUSB0");
				int nicDeviceSpeed = Integer.parseInt(properties.getProperty("nicDeviceSpeed", "57600"));
				*/
				String nicDeviceName = p[2];
				int nicDeviceSpeed = Integer.parseInt(p[3]);
				SerialPort sioPort;
				try {
					sioPort = SIOUtil.openSerialPort(nicDeviceName,
							nicDeviceSpeed);
				} catch (PortInUseException e) {
					throw new IOException("Port " + nicDeviceName + " in use");
				} catch (Error e) {
					throw new IOException("Unknown error opening port " + nicDeviceName );
				}
				if (sioPort == null) {
					log.error("sioPort==null, unable to open serial port " + nicDeviceName);
					throw new IOException("Unable to open serial port " + nicDeviceName);
				}
				if ("ie.wombat.ha.nic.zstack.ZStackDriver".equals(nicHardware)) {
					uartAdapter = new ZStackStreamAdapter(sioPort.getInputStream(),
							sioPort.getOutputStream());
				} else if ("ie.wombat.ha.nic.xbee.XBeeDriver".equals(nicHardware)) {
					uartAdapter = new XBeeStreamAdapter(sioPort.getInputStream(),
						sioPort.getOutputStream());
				}
			}

			if ("ie.wombat.ha.nic.zstack.ZStackDriver".equals(nicHardware)) {
				nic = new ZStackDriver(uartAdapter);
			} else if ("ie.wombat.ha.nic.xbee.XBeeDriver".equals(nicHardware)) {
				nic = new XBeeDriver(uartAdapter);
			} else {
				// TODO: wrong exception
				throw new IOException ("Unrecognized NIC type " + nicHardware);
			}
			
			log.info("nic=" + nic);
			log.info("ioAdapter=" + uartAdapter);
			
			log.info("pinging nic...");
			nic.ping();
			
			try {
			log.info("testing nic...");
			nic.test();
			} catch (IOException e) {
				e.printStackTrace();
				// otherwise ignore for the moment.. because we need the NIC
				// to send AT commands to fix the nic if it's misconfigured.
			}
		
			// Add listener for logging data
			nic.addZigBeePacketListener(new ReportAttributesListener());
		
			// Keep tabs on last contact from devices
			//nic.addZigBeePacketListener(this);
			//LastContactListener lc = new LastContactListener();
			nic.addZigBeePacketListener(new LastContactListener(networkId));
			
		} catch (IOException e) {
			e.printStackTrace();
			nic = new NullDriver();

		}
		
		//
		// Create device drivers
		//
		
		EntityManager em = HibernateUtil.getEntityManager();
		List<Device> deviceRecords = em.createQuery("from Device where network.id=:networkId order by id")
				.setParameter("networkId",getNetworkId())
				.getResultList();
				
		for (Device deviceRecord : deviceRecords) {
						
			String deviceDriverClassName = deviceRecord.getDriverClassName();
			if (deviceDriverClassName == null) {
				log.debug("No device driver specified for " + deviceRecord.getName() );
				continue;
			}
			
			log.debug("Attempting to initialize device driver for "
					+ deviceRecord.getName()
					);
			
			DeviceDriver deviceDriver = DeviceFactory.getInstance().getDeviceDriver(this, deviceRecord);
			
			// TODO: throw exception instead
			if (deviceDriver == null) {
				log.error ("Error detected in initializing device driver " + deviceDriverClassName);
				continue;
			}
			
			deviceDriver.setName(deviceRecord.getName());
			
			log.info("Device " + deviceDriver + " initialized.");
			Address64 addr64 = new Address64(deviceRecord.getAddress64());
			addr64ToDeviceDriverHash.put(addr64,deviceDriver);
		
		}
		
		
	}
	

	public synchronized static HANetwork getInstance(Long networkId) {
		return instanceHash.get(networkId);
	}
	public synchronized static AppBase getApplication(Long appId) {
		for (HANetwork net : instanceHash.values()) {
			for (AppBase app : net.getApplications()) {
				if (app.getId().equals(appId)) {
					return app;
				}
			}
		}
		return null;
	}
	
	/**
	 * Assign new 16 bit network address to a device on the network.
	 * 
	 * @param addr16
	 */
	public void updateAddress16 (Address64 addr64, Address16 addr16) {
		
		DeviceDriver device = getDevice(addr64);
		if (device == null) {
			log.error ("updateAddress16(): unable to find device with addr64 " + addr64);
			return;
		}

		if (device.getAddress16().equals(addr16)) {
			log.debug("updateAddress16(): addr16 unchanged. No action required.");
			return;
		}
		
		log.info("Updating addr16 for device " + addr64 + " from " + device.getAddress16() + " to " + addr16);

		device.setAddress16(addr16);
		
		// Update database
		EntityManager em = HibernateUtil.getEntityManager();
		em.getTransaction().begin();
		List<Device> list = em.createQuery("from Device where network.id=:networkId and address64=:addr64")
				.setParameter("networkId", networkId)
				.setParameter("addr64", addr64.toString())
				.getResultList();
		if (list.size()==1) {
			Device deviceRecord = list.get(0);
			deviceRecord.setAddress16(addr16.toString());
			deviceRecord.logEvent(LogRecord.INFO, "addr16_change", addr16.toString());
		} else {
			log.error ("updateAddress16(): unable to find device " + addr64 + " in DB while attempting to update addr16");
		}
		em.getTransaction().commit();
		
	}
	
	public synchronized static HANetwork createNetwork (Network networkRecord) {
		HANetwork network = instanceHash.get(networkRecord.getId());
		if (network != null) {
			log.warn ("Requested to create Network#" + networkRecord.getId() + " but it has already been created");
			return network;
		}
		
		network = new HANetwork (networkRecord);
		instanceHash.put(networkRecord.getId(), network);
		return network;
	}
	public synchronized void release () {
		nic.close();
		nic = null;
	}
	
	public void addApplication (AppBase app) {
		applications.add(app);
	}
	public List<AppBase> getApplications() {
		return applications;
	}
	
	/**
	 * Get ZigBee NIC object. 
	 * @return
	 */
	public ZigBeeNIC getNIC () {
		return nic;
	}
	
	public Long getNetworkId() {
		return networkId;
	}
	

	public List<DeviceDriver> getDevices() {
		return new ArrayList<DeviceDriver>(addr64ToDeviceDriverHash.values());
	}
	
	/**
	 * Get device by 64 bit IEEE address (octets in hex separated by colon) or by 
	 * device name if the parameter does not parse as an IEEE address.
	 * 
	 * If names are not unique, then one of the devices matching the parameter
	 * will be returned. Non-unique device names are strongly discouraged.
	 * 
	 * @param addr64str
	 * @return
	 */
	public DeviceDriver getDevice (String addr64str) {
		try {
			Address64 addr64 = new Address64(addr64str);
			return getDevice(addr64);
		} catch (NumberFormatException e) {
			log.debug("getDevice() treating paramater " + addr64str + " as device name");
			for (DeviceDriver device : addr64ToDeviceDriverHash.values()) {
				if (device.getName().equals(addr64str)) {
					return device;
				}
			}
			log.debug("getDevice() device name " + addr64str + " not found");
		}
		return null;
		//return getDevice(new Address64(addr16str));
	}
	public DeviceDriver getDevice (Address64 addr64) {
		return addr64ToDeviceDriverHash.get(addr64);
	}
	
	/**
	 * Got to the network to resolve a IEEE 64 bit address to the network 16 bit address.
	 * 
	 * @param addr64
	 * @return
	 */
	public Address16 askAddress16(Address64 addr64) {
		
		System.err.println ("************** " + "askAddress16(networkId=" + networkId + " addr64=" + addr64 + ")");
		log.info ("askAddress16(networkId=" + networkId + " addr64=" + addr64 + ")");
		
		// Reuse the GWT service
		AddressServiceImpl svc = new AddressServiceImpl();
		String addr16Str = svc.getDeviceAddr16(getNetworkId(), addr64.toString());
		Address16 addr16 = new Address16(addr16Str);
		updateAddress16(addr64, addr16);
		return addr16;
	}
	
}
