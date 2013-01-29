package ie.wombat.zigbee.zdo;

import java.io.Serializable;

//import org.apache.log4j.Logger;


/**
 * Ref ZigBee Specification 2.4.4.2.2, page 185.
 * 
 * @author joe
 *
 */
@SuppressWarnings("serial")
public class BindResponse implements ZDOResponse, Serializable {

	//private static final Logger log = Logger.getLogger(BindResponse.class);
	
	private int status=-1;
	
	public BindResponse () {
		
	}

	public void addPacket (byte[] bytes, int offset) {
		status = bytes[offset+0] & 0xff;
	}
	
	
	public String toString() {
		return ""+status;
	}
	
	

	public int getStatus() {
		return status;
	}

	public void setStatus(int status) {
		this.status = status;
	}

	public boolean isComplete () {
		return (status >=0);
	}
	
	
}
