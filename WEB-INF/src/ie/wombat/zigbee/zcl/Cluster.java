package ie.wombat.zigbee.zcl;

/**
 * Cluster ID constants.
 * 
 * TODO: isn't the cluster definition dependent on the Profile ID??
 * 
 * @author joe
 *
 */
public interface Cluster {
	public static final int BASIC = 0x0000;
	public static final int POWER_CONFIGURATION = 0x0001;
	public static final int DEVICE_TEMPERATURE = 0x00002;
	public static final int IDENTIFY = 0x0003;
	public static final int GROUPS = 0x0004;
	public static final int SCENES = 0x0005;
	public static final int ON_OFF = 0x0006;
	
	public static final int ALARMS = 0x0009;
	
	public static final int OCCUPANCY = 0x0406;
	public static final int TEMPERATURE_SENSOR = 0x0402;
	
	//
	// ZigBee Device Profile (ZDP) related clusters.
	// TODO: probably should rename with ZDP_ prefix
	//
	
	public static final int ZDO_ADDR16_REQUEST = 0x0000;
	public static final int ZDO_ADDR16_RESPONSE = 0x8000;
	
	public static final int ZDO_ADDR64_REQUEST = 0x0001;
	public static final int ZDO_ADDR64_RESPONSE = 0x8001;
	
	public static final int ZDO_NODE_DESCRIPTOR_REQUEST = 0x0002;
	public static final int ZDO_NODE_DESCRIPTOR_RESPONSE = 0x8002;
	
	public static final int ZDO_SIMPLE_DESCRIPTOR_REQUEST = 0x0004;
	public static final int ZDO_SIMPLE_DESCRIPTOR_RESPONSE = 0x8004;
	
	public static final int ZDO_ACTIVE_EP_REQUEST = 0x0005;
	public static final int ZDO_ACTIVE_EP_RESPONSE = 0x8005;
	

	
	
	public static final int ZDO_BIND_REQUEST = 0x0021;
	public static final int ZDO_BIND_RESPONSE = 0x8021;
	
	public static final int ZDO_UNBIND_REQUEST = 0x0022;
	public static final int ZDO_UNBIND_RESPONSE = 0x8022;
	
	public static final int ZDO_NETWORK_DISCOVERY_REQUEST = 0x0030;
	public static final int ZDO_NETWORK_DISCOVERY_RESPONSE = 0x8030;
	
	public static final int ZDO_LQI_REQUEST = 0x0031;
	public static final int ZDO_LQI_RESPONSE = 0x8031;
	
	public static final int ZDO_BIND_TABLE_REQUEST = 0x0033;
	public static final int ZDO_BIND_TABLE_RESPONSE = 0x8033;
	
	public static final int ZDO_MGMT_PERMIT_JOINING_REQUEST = 0x0036;
	public static final int ZDO_MGMT_PERMIT_JOINING_RESPONSE = 0x8036;

	
}
