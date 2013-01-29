package ie.wombat.zigbee.zcl;

/**
 * ZCL data types as specified in §2.5.2 and Table 2.15, page 52 of the ZCL Specification (29 May 2008).
 * 
 * @author joe
 *
 */
public interface ZCLDataType {
	public static final int BINARY = 0x10;
	

	public static final int BITMAP8 = 0x18;
	public static final int BITMAP16 = 0x19;
	public static final int BITMAP24 = 0x1a;
	public static final int BITMAP32 = 0x1b;
	
	/** 8 bit unsigned integer (0 .. 255) */
	public static final int UINT8 = 0x20;
	public static final int UINT16 = 0x21;
	public static final int UINT24 = 0x22;
	public static final int UINT32 = 0x23;
	public static final int UINT40 = 0x24;
	public static final int UINT48 = 0x25;
	


	/** 8 bit signed integer (-128 .. 127) */
	public static final int INT8 = 0x28;
	
	/** 16 bit signed integer (-32768 .. 32767) */
	public static final int INT16 = 0x29;
	
	public static final int ENUM8 = 0x30;
	public static final int ENUM16 = 0x31;
	
	/** String type. Character encoding is specified in the Complex Descriptor. Ref §2.5.2.12. */
	public static final int STRING = 0x42;
}
