package ie.wombat.zigbee.zcl;

/**
 * A ZCL attribute + value. Used for writing attributes etc. Limition: using 32 bit int
 * to store the value. Some values (strings, etc) exceed that, so need to figure a more
 * general way of handling this.
 * 
 * @author Joe Desbonnet, jdesbonnet@gmail.com
 *
 */
public class Attribute {

	private int id;
	private DataType type;
	private int intValue;
	
	public Attribute (int id, DataType type, int intValue) {
		this.id = id;
		this.type = type;
		this.intValue = intValue;
	}
	
	public int getId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
	}
	public DataType getType() {
		return type;
	}
	public void setType(DataType type) {
		this.type = type;
	}
	public int getIntValue() {
		return intValue;
	}
	public void setIntValue(int intValue) {
		this.intValue = intValue;
	}
	
	public byte[] serialize () {
		// Attribute Id (2 bytes)
		// Data type (1 byte)
		// Value (variable, depending on data type)
		int dataLen = getDataLength();
		byte[] ret = new byte[dataLen+3];
		ret[0] = (byte)(id & 0xff);
		ret[1] = (byte)((id>>8) & 0xff);
		ret[2] = (byte)type.getValue();
		
		for (int i = 0; i < dataLen; i++) {
			ret[3+i] = (byte)(intValue>>(i*8) & 0xff);
		}
		return ret;
	}
	
	public int getDataLength() {
		switch (this.type) {
		case BITMAP8:
		case UINT8:
		case INT8:
		case ENUM8:
			return 1;
		case BITMAP16:
		case UINT16:
		case INT16:
		case ENUM16:
			return 2;
			
		}
		return -1;
		
	}
	
}
