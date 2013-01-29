package ie.wombat.zigbee.zdo;

import java.io.Serializable;

import org.apache.log4j.Logger;

//import ie.wombat.ha.ByteFormatUtils;
import ie.wombat.zigbee.address.Address16;
import ie.wombat.zigbee.address.Address64;

@SuppressWarnings("serial")
public class Addr16Response implements ZDOResponse, Serializable {

	//private static final Logger log = Logger.getLogger(Addr16Response.class);
	
	private int status = -1;
	private Address64 addr64;
	private Address16 addr16;
	
	
	
	public Addr16Response (Address64 addr64) {
		this.addr64 = addr64;
	}
	
	/**
	 * An LQI response typically take more than one response packet to complete. Use this
	 * method to add response packets and {@link #isComplete()} will return true when the
	 * LQI response is complete.
	 * 
	 * @param bytes
	 * @param offset
	 */
	public void addPacket (byte[] bytes, int offset) {
		
		//log.debug (addr64 + ": " + ByteFormatUtils.byteArrayToString(bytes));
		
		status = bytes[offset+0] & 0xff;
		
		if ( status != ZDPStatus.SUCCESS ) {
			//log.error ("ZDO error, " + ZDPUtil.getStatusName(status));
			return;
		}
		
		// bytes 1 - 8 are addr64. 
		boolean lsbFirst = true;
		Address64 responseAddr64 = new Address64(bytes, offset+1, lsbFirst);
		
		if (! responseAddr64.equals(addr64)) {
			//log.info("ignoring ZDO response because it's for a different addr64, expecting "
			//		+ addr64.toString() + " but got " + responseAddr64.toString());
			return;
		}
		
		// bytes 9,10 are addr16
		addr16 = new Address16(bytes,offset+9,lsbFirst);
		status = 0x00;
		//log.info("Got good response addr16[" + addr64 + "]=" + addr16);

	}
	/**
	 * Return true of all NTEs have been set.
	 * @return
	 */
	public boolean isComplete () {
		return (status >= 0);
	}
	
	
	public int getStatus() {
		return status;
	}

	public void setStatus(int status) {
		this.status = status;
	}

	public Address16 getAddress16() {
		return addr16;
	}

	public void setAddress16(Address16 addr16) {
		this.addr16 = addr16;
	}


	public Address64 getAddress64() {
		return addr64;
	}

	public void setAddress64(Address64 addr64) {
		this.addr64 = addr64;
	}


	
}
