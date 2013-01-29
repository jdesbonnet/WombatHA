package ie.wombat.ha.ui.server;

import java.io.IOException;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import org.apache.log4j.Logger;

import ie.wombat.ha.ByteFormatUtils;
import ie.wombat.ha.HANetwork;
import ie.wombat.ha.ZigBeeNIC;
import ie.wombat.ha.ZigBeePacket;
import ie.wombat.ha.ZigBeePacketFilter;
import ie.wombat.ha.server.Device;
import ie.wombat.ha.ui.client.DeviceInfo;

import ie.wombat.ha.ui.client.ZigBeeService;
import ie.wombat.zigbee.ReadAttributesCommand;
import ie.wombat.zigbee.ReadAttributesResponse;
import ie.wombat.zigbee.ZigBeeCommand;
import ie.wombat.zigbee.ZigBeeCommandResponse;
import ie.wombat.zigbee.address.Address16;
import ie.wombat.zigbee.address.Address64;
import ie.wombat.zigbee.zcl.AttributeValue;
import ie.wombat.zigbee.zcl.Cluster;
import ie.wombat.zigbee.zcl.Profile;

import ie.wombat.zigbee.zdo.AttributesDiscovery;
import ie.wombat.zigbee.zdo.ReportingConfiguration;

import com.google.gwt.user.server.rpc.RemoteServiceServlet;

/**
 * The server side implementation of the RPC service.
 */
@SuppressWarnings("serial")
public class ZigBeeServiceImpl extends RemoteServiceServlet implements
		ZigBeeService, ReadAttributesResponse, ZigBeeCommandResponse {

	private static final Logger log = Logger.getLogger(ZigBeeServiceImpl.class);
	
	private static final int TIMEOUT = 90000;
	
	private String[] attrValues;
	
	public String[] getAttributeValues(Long deviceId, int endPoint, int profileId, int clusterId, int[] attrIds) throws IllegalArgumentException {
		
		log.info("getAttributeValues(deviceId=" + deviceId + " clusterId=" + clusterId + " attrIds=" + attrIds);
		
		DeviceInfo deviceInfo = getDeviceInfo(deviceId);
		Long networkId = deviceInfo.networkId;
		
		log.debug ("networkId=" + networkId);
		
		attrValues = null;
		
		Address64 addr64 = new Address64(deviceInfo.addr64);
		Address16 addr16 = new Address16(deviceInfo.addr16);
		
		//Address64 addr64 = Address64.BROADCAST;
		//Address16 addr16 = Address16.BROADCAST;
		
		ZigBeeNIC nic = HANetwork.getInstance(networkId).getNIC();
		log.debug ("nic=" + nic);
		ReadAttributesCommand cmd = new ReadAttributesCommand(nic);
		cmd.setAddress64(addr64);
		cmd.setAddress16(addr16);
		cmd.setSourceEndpoint(0x0a);
		cmd.setDestinationEndpoint(endPoint);
		cmd.setProfileId(profileId);
		cmd.setClusterId(clusterId);
		
		cmd.setAttributeIds(attrIds);
		
		cmd.setCallback(this);
		
		try {
			cmd.exec();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		synchronized (cmd) {
			try {
				cmd.wait(TIMEOUT);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		
		// TODO
		return attrValues;
		
	}
	


	public String getReporting (Long deviceId, final int ep, final int clusterId, int attrId) throws IllegalArgumentException {
		
		DeviceInfo deviceInfo = getDeviceInfo(deviceId);
		final Address64 addr64 = new Address64(deviceInfo.addr64);
		final Address16 addr16 = new Address16(deviceInfo.addr16);
		
		ZigBeeNIC nic = HANetwork.getInstance(deviceInfo.networkId).getNIC();
		ZigBeeCommand zcmd = new ZigBeeCommand(nic);
		
		zcmd.setSequenceId(ZigBeeCommand.AUTO_SEQUENCE);
				
		zcmd.setAddress64(addr64);
		zcmd.setAddress16(addr16);
		zcmd.setSourceEndpoint(0x0a);
		zcmd.setDestinationEndpoint(ep);
		zcmd.setProfileId(Profile.HOME_AUTOMATION);
		zcmd.setClusterId(clusterId);
		
		final ReportingConfiguration reportingCfg = new ReportingConfiguration ();
		log.debug ("created " + reportingCfg);
		zcmd.setCallback(new ZigBeeCommandResponse() {
			
			public void handleZigBeeCommandResponse(int status, Address16 addr16,
					ZigBeeCommand zcmd, byte[] payload) {
				reportingCfg.addPacket(payload, 3); // skip ZCL header, seqId, commandCode
				synchronized (zcmd) {
					zcmd.notifyAll();
				}
			}
		},new ZigBeePacketFilter() {
			public boolean allow(ZigBeePacket packet) {
				byte[] payload = packet.getPayload();
				if (payload.length < 2) {
					return false;
				}
				
				// Expect command 0x09 (Read Reporting Configuration Response Command)
				int commandCode = payload[2];
				if (commandCode != 0x09) {
					log.debug ("callback filter: rejecting packet because not command type 0x09");
					return false;
				}
				return true;
			}
		});
				
				
		
		byte[] command = new byte[4];
		command[0] = 0x08; // Read Reporting Configuration Command
		command[1] = 0x00; // Direction: 0x00 = reported
		command[2] = (byte)(attrId & 0xff);
		command[3] = (byte)(attrId >> 8);
		
		zcmd.setCommand(command);
		zcmd.setClusterSpecific(false);
		
		log.debug ("getReporting(): calling exec() on " + zcmd);
		try {
			zcmd.exec();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		log.debug ("getReporting(): sleep");
		synchronized (zcmd) {
			try {
				zcmd.wait(TIMEOUT);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		log.debug ("getReporting(): wake");

		
		/*
		if (reportingCfg.getStatus() == 0x00) {
			return reportingCfg.toString();
		} else {
			return "error";
		}
		*/
		log.debug("returning string value of " + reportingCfg);
		//return reportingCfg.toString();
		return "status=" + reportingCfg.getStatus()
				+ " attrId=" + reportingCfg.getAttributeId()
				+ " min=" + reportingCfg.getMinInterval() 
				+ " max=" + reportingCfg.getMaxInterval()
				+ " delta=" + reportingCfg.getReportableChange()
				;
	}
	
	/**
	 * Get a list of attribute IDs from a cluster at an end point. Use ZCL Discover Attributes Command (0x0C), 
	 * ref ZigBee Cluster Library spec 2.4.13, page 39.
	 */
	public int[] getAttributeIds (Long networkId, Long deviceId, final int ep, final int clusterId) throws IllegalArgumentException {
		
		DeviceInfo deviceInfo = getDeviceInfo(deviceId);
		final Address64 addr64 = new Address64(deviceInfo.addr64);
		final Address16 addr16 = new Address16(deviceInfo.addr16);
		
		ZigBeeNIC nic = HANetwork.getInstance(deviceInfo.networkId).getNIC();
		ZigBeeCommand zcmd = new ZigBeeCommand(nic);
		
		zcmd.setSequenceId(ZigBeeCommand.AUTO_SEQUENCE);
				
		zcmd.setAddress64(addr64);
		zcmd.setAddress16(addr16);
		zcmd.setSourceEndpoint(0x0a);
		zcmd.setDestinationEndpoint(ep);
		zcmd.setProfileId(Profile.HOME_AUTOMATION);
		zcmd.setClusterId(clusterId);
		
		final AttributesDiscovery attrDisc = new AttributesDiscovery ();
		log.debug ("created " + attrDisc);
		zcmd.setCallback(new ZigBeeCommandResponse() {
			
			public void handleZigBeeCommandResponse(int status, Address16 addr16,
					ZigBeeCommand zcmd, byte[] payload) {
				log.trace ("attribute discovery payload: " + ByteFormatUtils.byteArrayToString(payload));
				attrDisc.addPacket(payload, 3); // skip ZCL header, seqId, commandCode 
				synchronized (zcmd) {
					zcmd.notifyAll();
				}
			}
		},new ZigBeePacketFilter() {
			public boolean allow(ZigBeePacket packet) {
				byte[] payload = packet.getPayload();
				if (payload.length < 2) {
					return false;
				}
				
				// Expect command 0x09 (Read Reporting Configuration Response Command)
				int commandCode = payload[2];
				if (commandCode != 0x0D) {
					log.trace ("attribute discovery callback filter: rejecting packet because not command type 0x0D");
					return false;
				}
				log.debug ("attribute discovery callback filter: accepting packet");
				return true;
			}
		});
				
		byte[] command = new byte[4];
		command[0] = 0x0C; // Discover attributes
		command[1] = 0x00; // Start
		command[2] = 0x00;
		command[3] = 0x10; // Max attr
		
		
		zcmd.setCommand(command);
		zcmd.setClusterSpecific(false);
		
		log.debug ("getAttributes(): calling exec() on " + zcmd);
		try {
			zcmd.exec();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		log.debug ("getAttributes(): sleep");
		synchronized (zcmd) {
			try {
				zcmd.wait(TIMEOUT);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		log.debug ("getAttributes(): wake");

		
		log.debug("returning string value of " + attrDisc);
		return attrDisc.getAttributeIds();
	}
	
	public Integer setReporting (Long deviceId, 
			int ep, int clusterId, int attrId, 
			int minInterval, int maxInterval, 
			int reportableChange) throws IllegalArgumentException {
		
		DeviceInfo deviceInfo = getDeviceInfo(deviceId);
		Address64 addr64 = new Address64(deviceInfo.addr64);
		Address16 addr16 = new Address16(deviceInfo.addr16);
		
		ZigBeeNIC nic = HANetwork.getInstance(deviceInfo.networkId).getNIC();
		ZigBeeCommand cmd = new ZigBeeCommand(nic);
		//cmd.setSequenceId(ZigBeeCommand.NO_SEQUENCE);
		cmd.setSequenceId(ZigBeeCommand.AUTO_SEQUENCE);
				
		cmd.setAddress64(addr64);
		cmd.setAddress16(addr16);
		cmd.setSourceEndpoint(0x0a);
		cmd.setDestinationEndpoint(ep);
		cmd.setProfileId(Profile.HOME_AUTOMATION);
		cmd.setClusterId(clusterId);
		
		
		byte[] command = new byte[11];
		command[0] = 0x06; // Configure Reporting Configuration Command
		command[1] = 0x00; // Direction: 0x00 = reported
		command[2] = (byte)(attrId & 0xff);
		command[3] = (byte)(attrId >> 8);
		command[4] = 0x29; // Data type TODO: wtf do we get that
		command[5] = (byte)(minInterval & 0xff);
		command[6] = (byte)(minInterval>>8);
		command[7] = (byte)(maxInterval & 0xff);
		command[8] = (byte)(maxInterval>>8);
		command[9] = (byte)(reportableChange & 0xff);
		command[10] = (byte)(reportableChange>>8);
		
		
		cmd.setCommand(command);
		cmd.setClusterSpecific(false);
		
		try {
			cmd.exec();
		} catch (IOException e) {
			e.printStackTrace();
			return 1;
		}
		return 0;
	}
	
public Integer identify (Long deviceId) throws IllegalArgumentException {
		
		DeviceInfo deviceInfo = getDeviceInfo(deviceId);
		Address64 addr64 = new Address64(deviceInfo.addr64);
		Address16 addr16 = new Address16(deviceInfo.addr16);
		
		ZigBeeNIC nic = HANetwork.getInstance(deviceInfo.networkId).getNIC();
		ZigBeeCommand cmd = new ZigBeeCommand(nic);
		//cmd.setSequenceId(ZigBeeCommand.NO_SEQUENCE);
		cmd.setSequenceId(ZigBeeCommand.AUTO_SEQUENCE);
				
		cmd.setAddress64(addr64);
		cmd.setAddress16(addr16);
		cmd.setSourceEndpoint(0x0a);
		cmd.setDestinationEndpoint(0xff);
		cmd.setProfileId(Profile.HOME_AUTOMATION);
		cmd.setClusterId(Cluster.IDENTIFY);
		
		
		//byte[] command = {0x01};
		byte[] command = {0x00,0x3c,0x00};
		cmd.setCommand(command);
		cmd.setClusterSpecific(true);
		
		try {
			cmd.exec();
		} catch (IOException e) {
			e.printStackTrace();
			return 1;
		}
		return 0;
	}
	private DeviceInfo getDeviceInfo (Long deviceId) {
		
		if (deviceId == 0) {
			DeviceInfo coord = new DeviceInfo();
			coord.id=new Long(0L);
			coord.name = "Coordinator";
			coord.addr16 = Address16.COORDINATOR.toString();
			coord.addr64 = Address64.COORDINATOR.toString();
			return coord;
		}
		if (deviceId == -1) {
			DeviceInfo bcast = new DeviceInfo();
			bcast.id=new Long(-1L);
			bcast.name = "Broadcast";
			bcast.addr16 = Address16.BROADCAST_TO_ALL.toString();
			bcast.addr64 = Address64.BROADCAST.toString();
			return bcast;
		}
		EntityManagerFactory emf = Persistence.createEntityManagerFactory("ie.wombat.ha.server");
		EntityManager em = emf.createEntityManager();
		em.getTransaction().begin();
				
		Device device = em.find(Device.class, deviceId);
		DeviceInfo deviceInfo = new DeviceInfo();
		deviceInfo.id = deviceId;
		deviceInfo.networkId = device.getNetwork().getId();
		deviceInfo.name = device.getName();
		deviceInfo.addr64 = device.getAddress64();
		deviceInfo.addr16 = device.getAddress16();
		
		em.getTransaction().commit();
		em.close();
		emf.close();
		
		return deviceInfo;
		
	}
	
	
	
	/**
	 * Handle ReadAttributesCommand callbacks
	 */
	public void handleReadAttributesResponse(int status, Address16 addr16, ReadAttributesCommand zcmd,
			List<AttributeValue> attributes) {
		
		if (status == 0) {
			attrValues = new String[attributes.size()];
			for (int i = 0; i < attributes.size(); i++) {
				attrValues[i] = attributes.get(i).toString();
			}
		}
	
		synchronized (zcmd) {
			zcmd.notifyAll();
		}
		
	}

	/**
	 * Handle  callbacks
	 */
	public void handleZigBeeCommandResponse(int status, Address16 addr16,
		ZigBeeCommand zcmd, byte[] payload) {
			
		synchronized (zcmd) {
			zcmd.notifyAll();
		}
	}
}
