package ie.wombat.ha.devices;

import java.io.IOException;
import java.util.Date;
import java.util.List;

import javax.persistence.EntityManager;

import org.apache.log4j.Logger;

import ie.wombat.ha.HibernateUtil;
import ie.wombat.ha.ZCLSequence;
import ie.wombat.ha.ZigBeeNIC;
import ie.wombat.ha.ZigBeePacket;
import ie.wombat.ha.ZigBeePacketFilter;
import ie.wombat.ha.ZigBeePacketListener;
import ie.wombat.ha.server.DataLogRecord;
import ie.wombat.zigbee.AddressNotFoundException;
import ie.wombat.zigbee.ReadAttributesCommand;
import ie.wombat.zigbee.ReadAttributesResponse;
import ie.wombat.zigbee.ZigBeeCommand;
import ie.wombat.zigbee.ZigBeeCommandResponse;
import ie.wombat.zigbee.ZigBeeException;
import ie.wombat.zigbee.address.Address16;
import ie.wombat.zigbee.address.Address64;
import ie.wombat.zigbee.zcl.AttributeValue;
import ie.wombat.zigbee.zcl.Cluster;
import ie.wombat.zigbee.zcl.Profile;
import ie.wombat.zigbee.zcl.ZCLDataType;
import ie.wombat.zigbee.zcl.ZCLException;

public abstract class DeviceDriver implements ZigBeePacketListener {

	private static Logger log = Logger.getLogger(DeviceDriver.class);
	
	private static final int TIMEOUT = 15000;
	
	/** The ID in the database */
	private Long id;
	private String name;
	
	protected Address64 address64;
	protected Address16 address16;
	
	protected ZigBeeNIC nic;
	
	// Use this to pass a string attribute value from callback to getStringAttribute()
	private String returnString;
	
	// Use this to pass a int attribute value from callback to getIntAttribute()
	private int returnInt;
		
	// Use this to pass status code from ZigBee command to getXXXAttribute(). Status
	// values are defined in AttributeQueryCommand public constants.
	private int returnStatus;
	
	// Use this to pass response data from callback to getZDPInfo()
	private byte[] response;
	
	private int batteryStatus=-1;
	private Date batteryStatusUpdateTime;
	private long lastRxTime=0;
	
	public DeviceDriver (final Address64 address64, final Address16 address16, ZigBeeNIC nic) {
		this.address64 = address64;
		this.address16 = address16;
		this.nic = nic;
		
		// Register this device driver with the NIC. Limit to packets addressed
		// to this device (or broadcasts).
		nic.addZigBeePacketListener(this, new ZigBeePacketFilter() {
			
			public boolean allow(ZigBeePacket packet) {
				
				if (packet.isBroadcast()) {
					return true;
				}

				if (address64.equals(packet.getSourceAddress64())) {
					return true;
				}
				
				if (address16.equals(packet.getSourceAddress16())) {
					return true;
				}
				
				return false;
				
			}
		});
	}
	
	public synchronized String getManufacturer () throws ZigBeeException, IOException {
		return getStringAttribute(Profile.HOME_AUTOMATION,
				Cluster.BASIC, 
				0x0a,  // Source EP 10
				0x01,  // Destination EP 1
				0x0004 // Attribute ID 4 (Manufacturer Name)
				);
	}
	public synchronized String getModel () throws ZigBeeException, IOException {
		return getStringAttribute(Profile.HOME_AUTOMATION,
				Cluster.BASIC, 
				0x0a,  // Source EP 10
				0x01,  // Destination EP 1
				0x0005 // Attribute ID 5 (Model Name)
				);
	}
	public synchronized int[] getEndPoints () throws ZigBeeException, IOException {
		
		byte[] command = new byte[2];
		command[0] = (byte)0xe1;
		command[1] = (byte)0xf1;
		byte[] response = getZDPInfo(
				Profile.ZIGBEE_DEVICE_PROFILE,
				0x0005,
				0x00,
				0x00,
				command
				);
		int status = response[1];
		if (status != 0x00) {
			log.error ("getEndPoints(): status=0x" + Integer.toHexString(status));
			throw new ZCLException ("Error, status=0x" + Integer.toHexString(status));
		}
		int nEp = response[4];
		int[] endPoints = new int[nEp];
		for (int i = 0; i < nEp; i++) {
			endPoints[i] = response[5+i] & 0xff;
		}
		return endPoints;
		
	}

	/**
	 * Experimental
	 * @param profileId
	 * @param clusterId
	 * @param sourceEndpoint
	 * @param destinationEndpoint
	 * @param attributeId
	 * @return
	 * @throws ZCLException
	 * @throws IOException
	 */
	protected byte[] getZDPInfo (int profileId, int clusterId, 
			int sourceEndpoint, int destinationEndpoint, byte[] command) throws ZCLException, IOException {
		
		final ZigBeeCommand q = new ZigBeeCommand(nic);
		
		q.setAddress64(address64);
		q.setProfileId(profileId);
		q.setClusterId(clusterId);
		q.setSourceEndpoint(sourceEndpoint);
		q.setDestinationEndpoint(destinationEndpoint);
		q.setCommand(command);
		
		q.setCallback(new ZigBeeCommandResponse() {
			public synchronized void handleZigBeeCommandResponse(int status, Address16 addr16, ZigBeeCommand zcmd, byte[] payload) {
				returnStatus = status;
				if (status == ZigBeeCommand.SUCCESS) {
					response = new byte[payload.length];
					System.arraycopy(payload, 0, response, 0, payload.length);
					
				}
				synchronized (q) {
					q.notifyAll();
				}
			}
		});
		
		returnStatus = -1;
		
		q.exec();
		
		try {
			log.debug("getZDPInfo(): SLEEP");
			synchronized (q) {
				wait(10000);
			}
			log.debug("getZDPInfo(): WAKE");
		} catch (InterruptedException e) {
			e.printStackTrace();
			return null;
		}
		
		if (returnStatus == -1) {
			throw new ZCLException ("something went wrong (see logs)");
		}
		
		log.debug ("getZDPInfo(): response.length=" + response.length);
		return response;
	}
	
	protected String getStringAttribute (int profileId, int clusterId, 
			int sourceEndpoint, int destinationEndpoint, int attributeId) 
			throws AddressNotFoundException, ZCLException, IOException {
		int[] attrIds = new int[1];
		attrIds[0] = attributeId;
		
		final ReadAttributesCommand q = new ReadAttributesCommand(nic);
		
		q.setAddress64(address64);
		q.setProfileId(profileId);
		q.setClusterId(clusterId);
		q.setSourceEndpoint(sourceEndpoint);
		q.setDestinationEndpoint(destinationEndpoint);
		q.setAttributeIds(attrIds);
		
		q.setCallback(new ReadAttributesResponse() {
			public synchronized void handleReadAttributesResponse(int status, Address16 addr16, ReadAttributesCommand zcmd, List<AttributeValue> attributes) {
				returnStatus = status;
				if (status == ZigBeeCommand.SUCCESS) {
					AttributeValue attrVal = attributes.get(0);
					if (attrVal.getType() != ZCLDataType.STRING) {
						log.error ("getStringAttribute(): callback received non string attribute");
					} else {
						returnString = attributes.get(0).getStringValue();
					}
				}
				// Wake the thread blocked with wait() 
				synchronized (q) {
					q.notifyAll();
				}
				
			}
		});
		
		returnStatus = -1;
		
		q.exec();
		
		// Sleep (wait) until callback has been invoked (q.notifyAll())
		synchronized (q) {
			try {
				log.debug("getStringAttribute(): SLEEP");
				q.wait(10000);
				log.debug("getStringAttribute(): WAKE");
			} catch (InterruptedException e) {
				// ignore
			}
		}
		
		if (returnStatus != ZigBeeCommand.SUCCESS) {
			log.debug("getStringAttribute(): error=" + returnStatus);
			
			// This should not happen if we have a functioning NIC -- we should always get an ACK.
			if (returnStatus == -1) {
				log.warn("No response received. Is NIC connected?");
			}
			
			if (returnStatus == ZigBeeCommand.ADDRESS_NOT_FOUND) {
				throw new AddressNotFoundException("Address " + address64 + " not found.");
			}
		}
		log.error ("getStringAttribute(): returning " + returnString);
		
		return returnString;
	}
	

	/**
	 * Method that retrieves a single attribute from a cluster. Executes synchronously.
	 * 
	 * TODO: fix.
	 * 
	 * TODO: this duplicates a log of getStringAttribute(). Need to consolidate.
	 * @param profileId
	 * @param clusterId
	 * @param sourceEndpoint
	 * @param destinationEndpoint
	 * @param attributeId
	 * @return
	 * @throws AddressNotFoundException
	 * @throws ZCLException
	 * @throws IOException
	 */
	protected int getIntegerAttribute (int profileId, int clusterId, 
			int sourceEndpoint, int destinationEndpoint, int attributeId) 
			throws AddressNotFoundException, ZCLException, IOException {
		
		log.trace ("getIntegerAttribute(profileId=0x" + Integer.toHexString(profileId)
				+ " clusterId=0x" + Integer.toHexString(clusterId)
				+ " srcEp=" + sourceEndpoint
				+ " dstEp=" + destinationEndpoint
				+ " addrId=" + attributeId
				);

		int[] attrIds = new int[1];
		attrIds[0] = attributeId;
				
		final ReadAttributesCommand q = new ReadAttributesCommand(nic);
		
		q.setAddress64(address64);
		q.setAddress16(address16);
		q.setProfileId(profileId);
		q.setClusterId(clusterId);
		q.setSourceEndpoint(sourceEndpoint);
		q.setDestinationEndpoint(destinationEndpoint);
		q.setAttributeIds(attrIds);
		
		//returnInt = -999;
		
		q.setCallback(new ReadAttributesResponse() {
			public void handleReadAttributesResponse(int status, Address16 addr16, ReadAttributesCommand zcmd, List<AttributeValue> attributes) {
				returnStatus = status;
				if (status == ZigBeeCommand.SUCCESS) {
					// TODO: got indexOutOfBoundsException here
					//AttributeValue attrVal = attributes.get(0);
					if (attributes.size()>0 && attributes.get(0).isInteger()) {
						returnInt = attributes.get(0).getIntValue();
						log.debug ("getIntegerAttribute(): callback received integer attribute " + returnInt);
					} else {
						log.error ("getIntegerAttribute(): callback received non integer attribute");
					}
				}
				log.error ("getIntegerAttribute(): notifyAll() t=" + System.currentTimeMillis());
				
				// Wake the thread blocked with wait() 
				synchronized (q) {
					q.notifyAll();
				}
				
			}
		});
		
		returnStatus = -1;
		
		q.exec();
		
		// Sleep (wait) until callback has been invoked (q.notifyAll())
		synchronized (q) {
			try {
				q.wait(TIMEOUT);
			} catch (InterruptedException e) {
				// ignore
			}
		}
				
		if (returnStatus != ZigBeeCommand.SUCCESS) {
			log.debug("getIntAttribute(): error=" + returnStatus);
			
			// This should not happen if we have a functioning NIC -- we should always get an ACK.
			if (returnStatus == -1) {
				log.error("No response received.");
				throw new ZCLException ("No response received");
			}
			
			if (returnStatus == ZigBeeCommand.ADDRESS_NOT_FOUND) {
				throw new AddressNotFoundException("Address " + address64 + " not found.");
			}
		}
		log.error ("getIntegerAttribute(): returning " + returnInt);
		return returnInt;
	}

	/**
	 * Method that sets a single attribute from a cluster. Executes synchronously.
	 * 
	 * TODO: fix.
	 * 
	 * TODO: this duplicates a log of getStringAttribute(). Need to consolidate.
	 * @param profileId
	 * @param clusterId
	 * @param sourceEndpoint
	 * @param destinationEndpoint
	 * @param attributeId
	 * @return
	 * @throws AddressNotFoundException
	 * @throws ZCLException
	 * @throws IOException
	 */
	protected void setIntegerAttribute (int profileId, int clusterId, 
			int sourceEndpoint, int destinationEndpoint, int attributeId, int attributeValue) 
			throws AddressNotFoundException, ZCLException, IOException {
		
		log.trace ("setIntegerAttribute(profileId=0x" + Integer.toHexString(profileId)
				+ " clusterId=0x" + Integer.toHexString(clusterId)
				+ " srcEp=" + sourceEndpoint
				+ " dstEp=" + destinationEndpoint
				+ " attrId=" + attributeId
				+ " attrValue=" + attributeValue
				);

		// TODO
	}
	
	public abstract boolean isBatteryPowered ();
	
	/**
	 * Send ZCL query to retreive battery status of device. 
	 * @return 0 = battery OK, 1 = battery low, -1 n/a or error.
	 */
	public int queryBatteryStatus () {
		// Assuming EP1 is HA profile and has Alarm cluster 0x0009 and that bit 0 is the battery alarm. 
		// TODO: All big assumptions. 
		try {
			batteryStatus = getIntegerAttribute(Profile.HOME_AUTOMATION, 0x0009, 10, 1, 0x0000);
			batteryStatusUpdateTime = new Date();
			return batteryStatus;
		} catch (AddressNotFoundException e) {
			return -1;
		} catch (ZCLException e) {
			return -1;
		} catch (IOException e) {
			return -1;
		}
	}
	/**
	 * Get cached battery status of device. 
	 * @return 0 = battery OK, 1 = battery low, -1 n/a or error.
	 */
	public int getBatteryStatus () {
		// Check if device battery status is too old
		if (batteryStatusUpdateTime==null) {
			return -1;
		}
		if ((System.currentTimeMillis() - batteryStatusUpdateTime.getTime()) > 1800000L) {
			batteryStatus = -1;
		}
		return batteryStatus;
	}
	public Date getBatteryStatusUpdateTime () {
		return batteryStatusUpdateTime;
	}
	
	// Device data services. Need logger (timestamp, key, value) and time independent key,value pairs.
	
	public void saveData (String key, String value) {
		EntityManager em = HibernateUtil.getEntityManager();
		em.getTransaction().begin();
		DataLogRecord record = new DataLogRecord();
		//record.setNetworkId()
		record.setAddress64(address64.toString());
		record.setKey(key);
		record.setValue(value);
		em.persist(record);
		em.getTransaction().commit();
	}
	
	
	public Date getExpiryTime() {
		// TODO Auto-generated method stub
		return null;
	}

	public void setExpiryTime(Date expire) {
		// TODO Auto-generated method stub
		
	}
	
	public long getLastRxTime () {
		return lastRxTime;
	}

	public void handleZigBeePacket(ZigBeePacket packet) {
		if (packet.getSourceAddress16().equals(address16)) {
			lastRxTime = System.currentTimeMillis();
		}
	}
	
	public Address16 getAddress16() {
		return address16;
	}
	public void setAddress16(Address16 addr16) {
		this.address16 = addr16;
	}
	public Address64 getAddress64() {
		return address64;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
	
	public Long getId() {
		return this.id;
	}
	public void setId (Long id) {
		this.id = id;
	}
}
