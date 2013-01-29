package ie.wombat.zigbee.address;

import java.io.Serializable;

import ie.wombat.zigbee.util.ByteFormatUtils;

/**
 * 16bit ZigBee network address. These addresses are assigned to a device when it first 
 * joins the network. There are methods and constructors to handle address with
 * MSB and LSB first (XBee API is MSB first while 802.15.4/ZigBee is LSB first).
 * 
 * @author joe
 *
 */
@SuppressWarnings("serial")
public class Address16 implements Address, Serializable {

	private static final String BCAST = "_BCAST_";
	
	public static final Address16 COORDINATOR = new Address16("00 00");

	public static final Address16 BROADCAST_TO_ALL = new Address16("FFFF");
	public static final Address16 BROADCAST_TO_AWAKE = new Address16("FFFD");
	public static final Address16 BROADCAST_TO_ROUTERS = new Address16("FFFC");
	
	// TODO: this seems to be XBee specific, so can't put it here
	public static final Address16 UNKNOWN = new Address16("FFFE");

	/** 16 bit address stored with the most significant byte first (lowest index) */
	public byte[] addr = new byte[2];
	
	public Address16 () {
		
	}
	
	public Address16 (String addrStr) {
		if (addrStr == null) {
			addrStr = "FF FE";
		}
		addrStr = addrStr.replaceAll("\\s+","");
		addrStr = addrStr.replaceAll(":","");
		if (addrStr.length() != 4) {
			addr[0] = (byte)0xff;
			addr[1] = (byte)0xff;
			return;
		}
		int i;
		for (i = 0; i < 2; i++) {
			addr[i] = (byte)Integer.parseInt(addrStr.substring(i*2,i*2+2),16);
		}
	}
	/**
	 * Create Address16 with array of two bytes, MSB first.
	 * @param bytes
	 */
	public Address16 (byte[] bytes) {
		System.arraycopy(bytes, 0, this.addr, 0, 2);
	}
	/**
	 * Create Address16 with array of two bytes or more. Specify the
	 * offset into the array (must be two bytes available at that location)
	 * and set lsbFirst=true to have the first byte (lowest index) as LSB. If
	 * lstFirst=false then first byte will be the MSB.
	 *  
	 * @param bytes
	 */
	public Address16 (byte[] bytes, int offset, boolean lsbFirst) {
		if (lsbFirst) {
			this.addr[0] = bytes[offset+1];
			this.addr[1] = bytes[offset];
		} else {
			System.arraycopy(bytes, offset, this.addr, 0, 2);
		}
	}
	

	public byte[] getBytesMSBF() {
		// TODO: should we return a copy? 
		return addr;
	}

	//@Override
	public byte[] getBytesLSBF() {
		byte[] ret = new byte[2];
		ret[0]=addr[1];
		ret[1]=addr[0];
		return ret;
	}

	public String toString() {
		return ByteFormatUtils.formatHexByte(addr[0]) + ByteFormatUtils.formatHexByte(addr[1]);
	}
	
	@Override
	public int hashCode() {
		int hash = 1;
		hash = hash*31 + addr[0];
		hash = hash*31 + addr[1];
		return hash;
	}
	
	@Override
	public boolean equals(Object o) {
		if ( o == null ) return false;
		if ( this.getClass() != o.getClass() ) return false;
		Address16 a = (Address16)o;
		return (a.addr[0] == addr[0] && a.addr[1]==addr[1]);
	}
	 
}
