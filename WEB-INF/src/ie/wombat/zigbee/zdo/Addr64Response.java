package ie.wombat.zigbee.zdo;

import java.io.Serializable;

//import org.apache.log4j.Logger;

import ie.wombat.zigbee.address.Address16;
import ie.wombat.zigbee.address.Address64;

/**
 * Used to hold the contents of  ZDO_IEEE_addr_rsp (cluster 0x8001). This is sent
 * in response to a ZDO IEEE_addr_req (cluster 0x0001).
 * Reference ZigBee Specification 2.4.4.1.2 page 154.
 *  
 * @author Joe Desbonnet, jdesbonnet@gmail.com
 *
 */
@SuppressWarnings("serial")
public class Addr64Response implements ZDOResponse, Serializable {

	//private static final Logger log = Logger.getLogger(Addr64Response.class);
	
	private int status = -1;
	private Address64 addr64;
	private Address16 addr16;
	
	
	
	public Addr64Response (Address16 addr16) {
		this.addr16 = addr16;
	}
	
	/**
	 * 
	 * @param bytes
	 * @param offset
	 */
	public void addPacket (byte[] bytes, int offset) {
				
		status = bytes[offset+0] & 0xff;
		
		if ( status != ZDPStatus.SUCCESS ) {
			//log.error ("ZDO error, " + ZDPUtil.getStatusName(status));
			return;
		}
		
		// bytes 1 - 8 are addr64. 
		boolean lsbFirst = true;
		Address64 responseAddr64 = new Address64(bytes, offset+1, lsbFirst);
		Address16 responseAddr16 = new Address16(bytes, offset+9, lsbFirst);

		if (! responseAddr16.equals(addr16)) {
			//log.info("ignoring ZDO response because it's for a different addr16, expecting "
			//		+ addr16.toString() + " but got " + responseAddr16.toString());
			return;
		}
		
		// bytes 9,10 are addr16
		addr64 = responseAddr64;
		status = 0x00;
		//log.info("Got good response addr64[" + addr16 + "]=" + addr64);

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

	public Address64 getAddress64() {
		return addr64;
	}

	public void setAddress64(Address64 addr64) {
		this.addr64 = addr64;
	}
	

	public Address16 getAddress16() {
		return addr16;
	}

	public void setAddress16(Address16 addr16) {
		this.addr16 = addr16;
	}

}
