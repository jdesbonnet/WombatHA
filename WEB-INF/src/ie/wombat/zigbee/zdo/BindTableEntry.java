package ie.wombat.zigbee.zdo;

import java.io.Serializable;

import ie.wombat.zigbee.address.Address16;
import ie.wombat.zigbee.address.Address64;

public class BindTableEntry implements Serializable{
	public Address64 srcAddr64;
	public int srcEp;
	public int clusterId;
	public int addrMode=-1;
	public Address64 dstAddr64;
	public Address16 dstAddr16;
	public int dstEp;
	
	public BindTableEntry () {
		
	}
	
	public BindTableEntry (byte[] bytes, final int offset) {
		
		final boolean lsbFirst = true;
		srcAddr64 = new Address64(bytes, 0+offset, lsbFirst);
		
		srcEp = bytes[8+offset]&0xff;
		
		clusterId = bytes[9+offset];
		clusterId |= bytes[10+offset]<<8;
		clusterId &= 0xffff;
		
		addrMode = bytes[11+offset];
		
		if (addrMode == 1) {
			// Group address
			dstAddr16 = new Address16 (bytes, 12+offset, lsbFirst);
			dstAddr64 = null;
			dstEp = 0xff;
		} else if (addrMode == 3) {
			dstAddr64 = new Address64 (bytes, 12+offset, lsbFirst);
			dstAddr16 = null;
			dstEp = bytes[20+offset]&0xff;
		}
	}
	
	/**
	 * Return size in bytes of table entry or zero if not known.
	 * @return
	 */
	public int size () {
		if (addrMode == 1) {
			return 15;
		} else if (addrMode == 3) {
			return 21;
		} else {
			return 0;
		}
	}
	
	public String toString () {
		StringBuffer buf = new StringBuffer();
		buf.append ("srcAddr64=" + srcAddr64.toString());
		buf.append (" srcEp=0x" + Integer.toHexString(srcEp));
		buf.append (" clusterId=0x" + Integer.toHexString(clusterId));
		if (addrMode == 1) {
			buf.append (" dstAddr16=" + dstAddr16.toString());
		} else if (addrMode == 3) {
			buf.append (" dstAddr64=" + dstAddr64.toString());
		} else {
			buf.append (" dstAddr=?");
		}
		return buf.toString();
	}
}
