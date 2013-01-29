package ie.wombat.zigbee.zdo;

/**
 * ZDP status codes (in response to requests sent to the ZDO).
 * 
 * Reference ZigBee Specification (document 053474r17) 2.4.5 ZDP Enumeration Description, table 2.137, page 211
 * @author joe
 *
 */
public interface ZDPStatus {

	
	public static final int SUCCESS = 0x00;
	public static final int INV_REQUESTTYPE = 0x80;
	public static final int DEVICE_NOT_FOUND = 0x81; 
	public static final int INVALID_EP  = 0x82;
	public static final int NOT_ACTIVE = 0x83;
	public static final int NOT_SUPPORTED = 0x84;
	public static final int TIMEOUT = 0x85;
	public static final int NO_MATCH = 0x86;

	// ZigBee spec says 0x87 is reserved, but many references on the net
	// indicated that it's table full
	public static final int TABLE_FULL = 0x87;
	
	public static final int NO_ENTRY = 0x88;
	public static final int NO_DESCRIPTOR = 0x89;
	public static final int INSUFFICIENT_SPACE = 0x8a;
	public static final int NOT_PERMITTED = 0x8b;
	
	//public static final int TABLE_FULL = 0x8c;
	public static final int NOT_AUTHORIZED = 0x8d;
	// 0x8e - 0xff reserved

}
