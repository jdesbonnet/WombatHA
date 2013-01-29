package ie.wombat.zigbee.zcl;

/**
 * ZCL data types as specified in §2.5.2 and Table 2.15, page 52 of the ZCL
 * Specification (29 May 2008).
 * 
 * @author Joe Desbonnet, jdesbonnet@gmail.com
 * 
 */
public enum DataType {

	BINARY(0x10), BITMAP8(0x18), BITMAP16(0x19), BITMAP24(0x1a), BITMAP32(0x1b), UINT8(
			0x20), UINT16(0x21), UINT24(0x22), UINT32(0x23), UINT40(0x24), UINT48(
			0x25), INT8(0x28), INT16(0x29), ENUM8(0x30), ENUM16(0x31), STRING(
			0x42);

	private int value;

	private DataType(int value) {
		this.value = value;
	}

	public int getValue() {
		return this.value;
	}
}
