package ie.wombat.zigbee.address;

import java.io.Serializable;

import ie.wombat.zigbee.util.ByteFormatUtils;

/**
 * Unique 64bit IEEE address. When a device joins a network it is assigned a {@link Address16}
 * which should be used as much as possible (more efficient than {@link Address64}).
 * 
 * @author joe
 *
 */
@SuppressWarnings("serial")
public class Address64 implements Address, Serializable {
	
	/** Address which can be used to reach all devices on the network */
	public static final Address64 BROADCAST = new Address64("00:00:00:00:00:00:FF:FF");
	
	/** Used in XBee API when 64 bit address is unknown */
	public static final Address64 UNKNOWN = new Address64("FF:FF:FF:FF:FF:FF:FF:FF");

	/** Address which can be used to reach the coordinator */
	public static final Address64 COORDINATOR = new Address64("00:00:00:00:00:00:00:00");

	/** 64 bit address stored with the most significant byte (lowest index) first */
	public byte[] addr64 = new byte[8];
	
	public Address64 () {
		
	}
	
	/**
	 * Create 64 bit IEEE address object with the address in hex (usual MSB first notation
	 * with octets optionally separated by spaces or colons).
	 * 
	 * @param addrStr
	 */
	public Address64 (String addrStr) {
		addrStr = addrStr.replaceAll("\\s+","");
		addrStr = addrStr.replaceAll(":","");
		int i;
		for (i = 0; i < 8; i++) {
			addr64[i] = (byte)Integer.parseInt(addrStr.substring(i*2,i*2+2),16);
		}
	}
	
	/**
	 * Create 64 bit address with an array of bytes. The MSB is first (array index 0). 
	 * 
	 * @param bytes  Array of bytes of length of 8 or longer. MSB is first.
	 */
	public Address64 (byte[] bytes) {
		System.arraycopy(bytes, 0, this.addr64, 0, 8);
	}
	/**
	 * Create 64 bit address with an array of bytes. The MSB is first (array index 0). 
	 * The offset into the array can be specified (array length must be at least 
	 * offset + 8) bytes. If lsbFirst=true the first byte (the one with the lowest
	 * index) becomes the LSB of the address. This is the normal order within 
	 * 802.15.4 / ZigBee packets.
	 *
	 * 
	 * @param bytes
	 */
	public Address64 (byte[] bytes, int offset, boolean lsbFirst) {
		if (lsbFirst) {
			for (int i = 0; i < 8; i++) {
				this.addr64[i] = bytes[offset+7-i];
			} 
		} else {
			System.arraycopy(bytes, offset, this.addr64, 0, 8);
		}
	}

	public byte[] getBytesMSBF() {
		// TODO: should we return a copy? 
		return addr64;
	}

	public byte[] getBytesLSBF() {
		byte[] ret = new byte[8];
		for (int i = 0; i < 8; i++) {
			ret[i] = addr64[7-i];
		}
		return ret;
	}
	
	public String toString() {
		StringBuffer buf = new StringBuffer();
		buf.append(ByteFormatUtils.formatHexByte(addr64[0]));
		for (int i = 1; i < 8; i++) {
			buf.append(":");
			buf.append(ByteFormatUtils.formatHexByte(addr64[i]));
		}
		return buf.toString();
	}
	
	@Override
	public int hashCode() {
		int hash = 1;
		hash = hash*31 + addr64[0];
		hash = hash*31 + addr64[1];
		hash = hash*31 + addr64[2];
		hash = hash*31 + addr64[3];
		hash = hash*31 + addr64[4];
		hash = hash*31 + addr64[5];
		hash = hash*31 + addr64[6];
		hash = hash*31 + addr64[7];
		return hash;
	}
	
	@Override
	public boolean equals(Object o) {
		if ( o == null ) return false;
		if ( this.getClass() != o.getClass() ) return false;
		Address64 a = (Address64)o;
		for (int i = 0; i < 8; i++) {
			if (a.addr64[i] != addr64[i]) {
				return false;
			}
		}
		return true;
	}
	
}
