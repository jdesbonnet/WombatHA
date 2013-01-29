package ie.wombat.zigbee.zcl;

/**
 * ZCL status codes. See ZCL specification, Table 2.16 Enumerated Status Values (page 64) 
 * @author joe
 *
 */
public interface ZCLStatus {

	
	public static final int SUCCESS = 0x00;
	public static final int FAILURE = 0x01;
	public static final int MALFORMED_COMMAND = 0x80; 
	public static final int UNSUP_CLUSTER_COMMAND  = 0x81;
	public static final int UNSUP_MANUF_GENERAL_COMMAND = 0x84;
	public static final int INVALID_FIELD = 0x85;
	public static final int UNSUPPORTED_ATTRIBUTE = 0x86;
	

}
