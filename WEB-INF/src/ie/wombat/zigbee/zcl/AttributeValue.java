package ie.wombat.zigbee.zcl;

public class AttributeValue {
	
	
	/* package scope */ int id;
	/* package scope */ int type;
	int intValue;
	String stringValue;
	
	public boolean isInteger () {
		
		// Bitmaps 8 - 32 bits
		if (type >= 0x18 && type <= 0x1b) {
			return true;
		}
		
		// Unsigned int 8 - 32 bits
		if (type >= 0x20 && type <= 0x24) {
			return true;
		}
		// Signed int 8 - 32 bits
		if (type >= 0x28 && type <= 0x2b) {
			return true;
		}
		
		return false;
	}
	public int getId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
	}
	public int getType() {
		return type;
	}
	public void setType(int type) {
		this.type = type;
	}
	public int getIntValue() {
		return intValue;
	}
	public void setIntValue(int intValue) {
		this.intValue = intValue;
		//this.stringValue = "0x" + Integer.toHexString(intValue);
		this.stringValue = ""+intValue;
	}
	public String getStringValue() {
		return stringValue;
	}
	public void setStringValue(String stringValue) {
		this.stringValue = stringValue;
	}
	
	public String toString () {
		return "(0x" + Integer.toHexString(type) + ") " 
				+ ( isInteger() ? "0x"+Integer.toHexString(intValue) : stringValue);
	}
	
}
