package ie.wombat.zigbee.zdo;

import ie.wombat.zigbee.address.Address16;

/**
 * Data structure for ZDO Active Endpoint Response (Cluster ID 0x8005)
 * 
 * @author joe
 *
 */
@SuppressWarnings("serial")
public class ActiveEndpointResponse implements ZDOResponse {

	private int status;
	private Address16 addr16;
	private int[] endpoints;
	
	public ActiveEndpointResponse () {
	}

	
	public void addPacket(byte[] bytes, int offset) {
		
		if (bytes.length - offset < 4) {
			// TODO: use log4j
			System.err.println ("addPacket(): expecting at least 4 bytes, but got " + (bytes.length-offset));
			return;
		}
		this.status = bytes[offset+0] & 0xff;

		this.addr16 = new Address16(bytes,offset+1, true /* lsbfirst */);
		
		int nep = bytes[offset+3] & 0xff;
		this.endpoints = new int[nep];
		for (int i = 0; i < nep; i++) {
			this.endpoints[i] = bytes[offset+4+i] & 0xff;
		}
	}

	public boolean isComplete() {
		return (endpoints != null);
	}
	
	public int getStatus() {
		return status;
	}

	public void setStatus(int status) {
		this.status = status;
	}

	public Address16 getAddr16() {
		return addr16;
	}

	public void setAddr16(Address16 addr16) {
		this.addr16 = addr16;
	}

	public int[] getEndpoints() {
		return endpoints;
	}

	public void setEndpoints(int[] endpoints) {
		this.endpoints = endpoints;
	}


	
	
	
}
