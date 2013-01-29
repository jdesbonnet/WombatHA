package ie.wombat.ha.ui.server;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import org.apache.log4j.Logger;

import ie.wombat.ha.ByteFormatUtils;
import ie.wombat.ha.HANetwork;
import ie.wombat.ha.HibernateUtil;
import ie.wombat.ha.ZigBeeNIC;
import ie.wombat.ha.ZigBeePacket;
import ie.wombat.ha.ZigBeePacketFilter;
import ie.wombat.ha.nic.xbee.XBeeUtil;
import ie.wombat.ha.server.Device;
import ie.wombat.ha.server.EndPoint;
import ie.wombat.ha.ui.client.DeviceInfo;
import ie.wombat.ha.ui.client.ZDOService;

import ie.wombat.zigbee.ReadAttributesCommand;
import ie.wombat.zigbee.ZDPRequest;
import ie.wombat.zigbee.ZDPResponseListener;
//import ie.wombat.zigbee.ZigBeeCommand;
import ie.wombat.zigbee.address.Address16;
import ie.wombat.zigbee.address.Address64;
import ie.wombat.zigbee.zcl.AttributeValue;
import ie.wombat.zigbee.zcl.Cluster;
import ie.wombat.zigbee.zcl.Profile;
import ie.wombat.zigbee.zdo.ActiveEndpointResponse;
import ie.wombat.zigbee.zdo.BindResponse;
import ie.wombat.zigbee.zdo.BindTableResponse;
import ie.wombat.zigbee.zdo.LQIResponse;
import ie.wombat.zigbee.zdo.NetworkDiscoveryResponse;
import ie.wombat.zigbee.zdo.NodeDescriptorResponse;
import ie.wombat.zigbee.zdo.SimpleDescriptorResponse;
import ie.wombat.zigbee.zdo.ZDOResponse;

import com.google.gwt.user.server.rpc.RemoteServiceServlet;

/**
 * The server side implementation of the RPC service.
 */
@SuppressWarnings("serial")
public class ZDOServiceImpl extends RemoteServiceServlet implements
		ZDOService, ZDPResponseListener {

	private static final Logger log = Logger.getLogger(ZDOServiceImpl.class);

	
	private static final int TIMEOUT = 90000;
	
	private String[] attrValues;
	
	// TODO: thread safety
	// TODO: name confusion: ZDOResponse vs ZDPResponse (similar name, different functions)
	private HashMap<ZDPRequest, ZDOResponse> commandResponseHash = new HashMap<ZDPRequest, ZDOResponse>();

	/**
	 * Ref ZigBee Specification (document 053474r17) 2.4.3.1.6 Active_EP_req, page104 
	 */
	public int[] getActiveEndpoints(Long networkId, Long deviceId) throws IllegalArgumentException {
		
		log.info ("getActiveEndpoints(deviceId=" + deviceId +")");

		DeviceInfo deviceInfo = getDeviceInfo(deviceId);
		Address16 addr16 = new Address16(deviceInfo.addr16);
		
		
		byte[] command = addr16.getBytesLSBF();
		
		// Experiment: adding 0x77 as transaction sequence number. Doesn't work!
		//byte[] command = new byte[3];
		//command[0] = 0x77;
		//command[1] = addr16.getBytesLSBF()[0];
		//command[2] = addr16.getBytesLSBF()[1];
		
		ActiveEndpointResponse aer = new ActiveEndpointResponse();
		doZDORequest (networkId, deviceId, Cluster.ZDO_ACTIVE_EP_REQUEST, command, aer);
		
		return aer.getEndpoints();
	}

	// TODO: return data structure
	public String getSimpleDescriptor(Long networkId, Long deviceId, int endPoint) throws IllegalArgumentException {

		DeviceInfo deviceInfo = getDeviceInfo(deviceId);
		Address64 addr64 = new Address64(deviceInfo.addr64);
		Address16 addr16 = new Address16(deviceInfo.addr16);
		log.info ("getSimpleDescriptor(deviceId=" + deviceId
				+ " addr64=" + addr64
				+ " addr16=" + addr16
				+ " ep=" + endPoint +")");

		byte[] command = new byte[3];
		byte[] addr16bytes = addr16.getBytesLSBF();
		command[0] = addr16bytes[0];
		command[1] = addr16bytes[1];
		command[2] = (byte)endPoint;
		SimpleDescriptorResponse zdoResponse = new SimpleDescriptorResponse();
		SimpleDescriptorFilter filter = new SimpleDescriptorFilter();
		filter.setEndPoint(endPoint);
		doZDORequest (networkId, addr64, addr16, Cluster.ZDO_SIMPLE_DESCRIPTOR_REQUEST, command, zdoResponse, filter);
		return zdoResponse.toString();
	}
	
	public SimpleDescriptorResponse getSimpleDescriptor2(Long networkId, Long deviceId, int endPoint) throws IllegalArgumentException {
		
		DeviceInfo deviceInfo = getDeviceInfo(deviceId);
		Address64 addr64 = new Address64(deviceInfo.addr64);
		Address16 addr16 = new Address16(deviceInfo.addr16);
		
		log.info ("getSimpleDescriptor2(deviceId=" + deviceId
				+ " addr64=" + addr64
				+ " addr16=" + addr16
				+ " ep=" + endPoint +")");
		
		byte[] command = new byte[3];
		byte[] addr16bytes = addr16.getBytesLSBF();
		command[0] = addr16bytes[0];
		command[1] = addr16bytes[1];
		command[2] = (byte)endPoint;
		SimpleDescriptorResponse zdoResponse = new SimpleDescriptorResponse();
		SimpleDescriptorFilter filter = new SimpleDescriptorFilter();
		filter.setEndPoint(endPoint);
		doZDORequest (networkId, addr64, addr16, Cluster.ZDO_SIMPLE_DESCRIPTOR_REQUEST, command, zdoResponse, filter);
		return zdoResponse;
	}
	
	
	// TODO: return data structure
	public String getNodeDescriptor (Long networkId, Long deviceId) throws IllegalArgumentException {
		log.info ("getNodeDescriptor(deviceId=" + deviceId +")");

		DeviceInfo deviceInfo = getDeviceInfo(deviceId);
		Address16 addr16 = new Address16(deviceInfo.addr16);
		byte[] command = addr16.getBytesLSBF();
		NodeDescriptorResponse zdoResponse = new NodeDescriptorResponse();
		doZDORequest (networkId, deviceId, Cluster.ZDO_NODE_DESCRIPTOR_REQUEST, command, zdoResponse);
		
		return zdoResponse.toString();
	}
	
	public String getNetworkDiscovery (Long networkId, Long deviceId) throws IllegalArgumentException {
		
		log.info ("getNetworkDiscovery(deviceId=" + deviceId +")");

		byte[] command = new byte[6];
			
		// Channel mask (0x07fff800 for all channels 11 - 26)
		command[0] = (byte)0x00;
		command[1] = (byte)0xf8;
		command[2] = (byte)0xff;
		command[3] = (byte)0x07;
		
		command[4] = 8; // Time per channel
		command[5] = 0; // Start index (?)
		
		ZDOResponse zdoResponse = new NetworkDiscoveryResponse();
		doZDORequest (networkId, deviceId, Cluster.ZDO_NETWORK_DISCOVERY_REQUEST, command, zdoResponse);
		return zdoResponse.toString();
	}
	
	
	public String getLQI (Long networkId, Long deviceId) throws IllegalArgumentException {
		log.info ("getLQI(deviceId=" + deviceId +")");

		byte[] command = new byte[1];
		command[0] = 0; // Start index
		ZDOResponse zdoResponse = new LQIResponse();
		doZDORequest (networkId, deviceId, Cluster.ZDO_LQI_REQUEST, command, zdoResponse);
		
		// TODO: fix
		command[0] = 3;
		doZDORequest (networkId, deviceId, Cluster.ZDO_LQI_REQUEST, command, zdoResponse);
		
		return zdoResponse.toString();
	}
		
	/**
	 * Get binding table. Uses ZDP Mgmt_bind_req ZDP cluster 0x0033 (ref ZigBee Specification, 2.4.3.3.4 page 142.
	 */
	public String getBindTable (Long networkId, Long deviceId) {
		log.info ("getBindTable(deviceId=" + deviceId +")");

		byte[] command = new byte[1];
		command[0] = 0; // Start index
		ZDOResponse zdoResponse = new BindTableResponse();
		doZDORequest (networkId, deviceId, Cluster.ZDO_BIND_TABLE_REQUEST, command, zdoResponse);
		log.debug("getBindTable(): response=" + zdoResponse.toString());
		return zdoResponse.toString();
	}
	
	public BindTableResponse getBindTable2 (Long networkId, Long deviceId) {
		log.info ("getBindTable(deviceId=" + deviceId +")");

		byte[] command = new byte[1];
		command[0] = 0; // Start index
		BindTableResponse zdoResponse = new BindTableResponse();
		doZDORequest (networkId, deviceId, Cluster.ZDO_BIND_TABLE_REQUEST, command, zdoResponse);
		log.debug("getBindTable(): response=" + zdoResponse.toString());
		return zdoResponse;
	}
	
	
	/**
	 * Ref ZigBee Specification 2.4.3.2.2 page 125
	 * 
	 * @param deviceId
	 * @return
	 * @throws IllegalArgumentException
	 */
	public Integer bindRequest (Long networkId, Long srcDeviceId, Long dstDeviceId, int clusterId, int srcEp, int dstEp) throws IllegalArgumentException {
		
		log.info ("bindRequst("
				+ " networkId=" + networkId
				+ " srcDeviceId=" + srcDeviceId
				+ " srcEp=" + srcEp
				+ " dstDeviceId=" + dstDeviceId
				+ " dstEp=" + dstEp
				+ " clusterId=" + clusterId
				+")");

		// TODO: consolidate db transactions
		DeviceInfo srcDeviceInfo = getDeviceInfo(srcDeviceId);
		DeviceInfo dstDeviceInfo = getDeviceInfo(dstDeviceId);
		
		Address64 srcAddr64 = new Address64(srcDeviceInfo.addr64);
		Address64 dstAddr64 = new Address64(dstDeviceInfo.addr64);
		
		log.debug ("srcAddr64=" + srcAddr64);
		log.debug ("dstAddr64=" + dstAddr64);
		
		byte[] command = new byte[21];	
		int i;
		for (i = 0; i < 8; i++) {
			command[0+i] = srcAddr64.addr64[7-i];
		}
		command[8] = (byte)srcEp;
		
		command[9] = (byte)(clusterId & 0xff);
		command[10] = (byte)(clusterId>>8);
		
		command[11] = 0x03; // 64bit dst addr
		
		for (i = 0; i < 8; i++) {
			command[12+i] = dstAddr64.addr64[7-i];
		}
		
		command[20] = (byte)dstEp;
		
		
		ZDOResponse zdoResponse = new BindResponse();
		doZDORequest (networkId, srcDeviceId, Cluster.ZDO_BIND_REQUEST, command, zdoResponse);
			
		return ((BindResponse)zdoResponse).getStatus();
	}
	
	/**
	 * Ref ZigBee Specification 2.4.3.2.3 Unbind_req page 127
	 * 
	 * @param deviceId
	 * @return
	 * @throws IllegalArgumentException
	 */
	public Integer unbindRequest (Long networkId, String srcAddr64str, String dstAddr64str, int clusterId, int srcEp, int dstEp) throws IllegalArgumentException {
		
		// TODO: this duplicates much code from bindRequest(). Can we consolidate?
		
		log.info ("unbindRequst("
				+ " networkId=" + networkId
				+ " srcDeviceId=" + srcAddr64str
				+ " srcEp=" + srcEp
				+ " dstDeviceId=" + dstAddr64str
				+ " dstEp=" + dstEp
				+ " clusterId=" + clusterId
				+")");

		
		Address64 srcAddr64 = new Address64(srcAddr64str);
		Address64 dstAddr64 = new Address64(dstAddr64str);
		
		log.debug ("srcAddr64=" + srcAddr64);
		log.debug ("dstAddr64=" + dstAddr64);
		
		byte[] command = new byte[21];	
		int i;
		for (i = 0; i < 8; i++) {
			command[0+i] = srcAddr64.addr64[7-i];
		}
		command[8] = (byte)srcEp;
		
		command[9] = (byte)(clusterId & 0xff);
		command[10] = (byte)(clusterId>>8);
		
		command[11] = 0x03; // 64bit dst addr
		
		for (i = 0; i < 8; i++) {
			command[12+i] = dstAddr64.addr64[7-i];
		}
		
		command[20] = (byte)dstEp;
		
		ZDOResponse zdoResponse = new BindResponse();
		doZDORequest (networkId, srcAddr64str , Cluster.ZDO_UNBIND_REQUEST, command, zdoResponse);
			
		return ((BindResponse)zdoResponse).getStatus();
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
	
	private DeviceInfo getDeviceInfo (String addr64str) {
		
		EntityManager em = HibernateUtil.getEntityManager();
		addr64str = addr64str.trim().toUpperCase();
		
		List <Device> list = em.createQuery("from Device where address64=:addr64")
			.setParameter("addr64",addr64str)
			.getResultList();
		
		if (list.size() != 1) {
			log.warn ("expecting exactly one record matching addr64=" + addr64str + " but found " + list.size());
			return null;
		}
		
		Device device = list.get(0);
		
		DeviceInfo deviceInfo = new DeviceInfo();
		deviceInfo.id = device.getId();
		deviceInfo.networkId = device.getNetwork().getId();
		deviceInfo.name = device.getName();
		deviceInfo.addr64 = device.getAddress64();
		deviceInfo.addr16 = device.getAddress16();
		
		return deviceInfo;
		
	}
	
	private void doZDORequest (Long networkId, Long deviceId, final int clusterId, byte[] command, ZDOResponse zdoResponse) {
		log.trace ("using standard ZDP filter");
		ZDPFilter stdFilter = new ZDPFilter();
		stdFilter.setClusterId(clusterId);
		DeviceInfo deviceInfo = getDeviceInfo(deviceId);
		Address64 addr64 = new Address64(deviceInfo.addr64);
		Address16 addr16 = new Address16(deviceInfo.addr16);
		doZDORequest(networkId, addr64, addr16, clusterId, command, zdoResponse,stdFilter);
	}
	
	
	private void doZDORequest (Long networkId, String deviceAddr64, final int clusterId, byte[] command, ZDOResponse zdoResponse) {
		log.trace ("using standard ZDP filter");
		ZDPFilter stdFilter = new ZDPFilter();
		stdFilter.setClusterId(clusterId);
		DeviceInfo deviceInfo = getDeviceInfo(deviceAddr64);
		Address64 addr64 = new Address64(deviceAddr64);
		Address16 addr16 = new Address16(deviceInfo.addr16);
		doZDORequest(networkId, addr64, addr16, clusterId, command, zdoResponse,stdFilter);
	}

	/**
	 * Synchronously issue ZDO request. Issue ZDO request, wait up to TIMEOUT period and return response (if any).
	 * 
	 * @param deviceId
	 * @param clusterId
	 * @param command
	 * @return ZDO response or null if TIMEOUT or error.
	 */
	private void doZDORequest (Long networkId, Address64 addr64, Address16 addr16, 
			final int clusterId, byte[] command, ZDOResponse zdoResponse, ZDPFilter filter) {

		log.info("ZDO Request clusterId=0x" + Integer.toHexString(clusterId)
				+ " command=" + ByteFormatUtils.byteArrayToString(command));
		
		
		//final Address64 addr64 = new Address64(deviceInfo.addr64);
		//final Address16 addr16 = new Address16(deviceInfo.addr16);
		
		ZigBeeNIC nic = HANetwork.getInstance(networkId).getNIC();
		ZDPRequest zcmd = new ZDPRequest(nic);		
		
		//zcmd.setAddress64(Address64.UNKNOWN); // for XBee
		zcmd.setAddress64(addr64);
		zcmd.setAddress16(addr16);
		zcmd.setSourceEndpoint(0x0); // must be 0x0 ??
		zcmd.setClusterId(clusterId);
		zcmd.setCommand(command);
		
		commandResponseHash.put(zcmd, zdoResponse);
	
		zcmd.setCallback(this,filter);
						
		try {
			zcmd.exec();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		synchronized (zcmd) {
			try {
				zcmd.wait(TIMEOUT);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
	
	
	/**
	 * Handle ReadAttributesCommand callbacks
	 */
	public void handleReadAttributesResponse(int status, Address16 addr16, ReadAttributesCommand zcmd,
			List<AttributeValue> attributes) {
		
		if (status == 0) {
			attrValues = new String[attributes.size()];
			for (int i = 0; i < attributes.size(); i++) {
				attrValues[i] = attributes.get(i).getStringValue();
			}
		}
		
		//if (status == 0) {
			//attrValue = attributes.get(0);
		//}
		
		synchronized (zcmd) {
			zcmd.notifyAll();
		}
		
	}

	/**
	 * Handle ZDP responses
	 */
	public void handleZDPResponse(int status, Address16 addr16,
			ZDPRequest zcmd, byte[] payload) {
		
		log.info("Got ZDO response deliveryStatus=" 
				+ XBeeUtil.getTxStatusDescription(status) // TODO: why XBee?
				+ " clusterId=0x" + Integer.toHexString(zcmd.getClusterId())
				+ " payload=" + ByteFormatUtils.byteArrayToString(payload)
				);
		

		if (status == 0x00) {
			
			ZDOResponse zdoResponse = commandResponseHash.get(zcmd);
			if (zdoResponse == null) {
				log.warn ("No ZDOResponse found for ZigBeeCommand=" + zcmd);
			}
			
			// We got a response to the command. But the response could still be an error.
			log.debug("ZDO Response: " + ByteFormatUtils.byteArrayToString(payload));

			// Copy packet payload (excluding header byte) to zdoResponse
			//zdoResponse = new byte[payload.length-1];
			//System.arraycopy(payload, 1, zdoResponse, 0, zdoResponse.length);
			
			// Skip transaction sequence number (first byte)
			zdoResponse.addPacket(payload,  1); 
		
		} else {
			log.error ("ZDO delivery error, status=" + ByteFormatUtils.formatHexByte(status));
		}
		
		// If the response is complete then unblock thread waiting for zdoResponse
		//if (zdoResponse.isComplete()) {
			synchronized (zcmd) {
				zcmd.notifyAll();
			}
		//} else {
			//log.info("ZDO Response not yet complete");
		//}
		
		
	}

	
	/**
	 * Get the 16 bit network address of a device. Node Descriptor Request (0x0002) seems to be the most
	 * reliable way of getting this information. Cluster 0x0000 is not working reliably for me.
	 */
	public String getDeviceAddr16(Long networkId,Long deviceId)
			throws IllegalArgumentException {
		
		// Device ID 0 is always the coordinator.
		if (deviceId == 0) {
			return "0000";
		}
		
		DeviceInfo deviceInfo = getDeviceInfo(deviceId);
		Address16 addr16 = new Address16(deviceInfo.addr16);
		byte[] command = addr16.getBytesLSBF();
		NodeDescriptorResponse zdoResponse = new NodeDescriptorResponse();
		doZDORequest (networkId, deviceId, Cluster.ZDO_NODE_DESCRIPTOR_REQUEST, command, zdoResponse);
		if (zdoResponse.getStatus() == 0x00) {
			return zdoResponse.getAddr16().toString();
		} else {
			return "ERR" + Integer.toHexString(zdoResponse.getStatus());
		}
	}

}
