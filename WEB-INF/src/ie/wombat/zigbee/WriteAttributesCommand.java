package ie.wombat.zigbee;

import ie.wombat.ha.ZigBeeNIC;
import ie.wombat.zigbee.address.Address16;
import ie.wombat.zigbee.address.Address64;
import ie.wombat.zigbee.zcl.Attribute;

import java.io.IOException;
import java.util.List;

import org.apache.log4j.Logger;

/**
 * Sends a ZigBee Write Attributes command to a profile/cluster/endpoint and
 * returns status via a callback to a object implementing
 * {@link WriteAttributesResponse} interface. 
 * 
 * Write attribute related commands:
 * 0x02: Write attributes
 * 0x03: Write attributes undivided (all or nothing: if there is a problem with an attribute ignore the entire write operation)
 * 0x04: Write attributes response
 * 0x05: Write attributes no response
 * 
 * This version is a wrapper around {@link ZigBeeCommand}. 
 * @author joe
 *
 */
public class WriteAttributesCommand implements ZigBeeCommandResponse {
	
	private static Logger log = Logger.getLogger(WriteAttributesCommand.class);
	
	private Address16 address16;
	private Address64 address64;
	private int profileId;
	private int clusterId;
	private int sourceEndpoint;
	private int destinationEndpoint;
	private Attribute[] attributes;

	private WriteAttributesResponse callback;
	
	private ZigBeeCommand zcmd;
	
	public WriteAttributesCommand(ZigBeeNIC nic) {
		zcmd = new ZigBeeCommand(nic);
	}
	
	
	public void exec () throws IOException {
		log.debug ("exec()");
		
		zcmd.setProfileId(profileId);
		zcmd.setClusterId(clusterId);
		zcmd.setSourceEndpoint(sourceEndpoint);
		zcmd.setDestinationEndpoint(destinationEndpoint);
		zcmd.setAddress64(address64);
		zcmd.setAddress16(address16);
		
		int serializedAttributesLength = 0;
		byte[][] serializedAttributesArray = new byte[attributes.length][];
		for (int i = 0; i < attributes.length; i++) {
			serializedAttributesArray[i] = attributes[i].serialize();
			serializedAttributesLength += serializedAttributesArray[i].length;
		}
		byte[] command = new byte[1 + serializedAttributesLength];
		
		command[0] = 0x05; // Write attribute, no response (ZCL, table 2.9, page 16)
	
		int p = 1;
		for (int i = 0; i < attributes.length; i++) {
			System.arraycopy(serializedAttributesArray[i], 0, command, p, serializedAttributesArray[i].length);
			p += serializedAttributesArray[i].length;
		}
		
		// ZCL 2.4.1.1.1 "The frame type sub-field shall be set to indicate a profile wide command".
		zcmd.setClusterSpecific(false);
		
		zcmd.setCommand(command);
		
		// Call the handleResponse() method of this object when result is available.
		//zcmd.setCallback(this);
		
		log.debug("exec(): calling exec() zcmd");
		zcmd.exec();
	}
	

	//
	// Accessor methods
	//
	
	public Attribute[] getAttributes() {
		return attributes;
	}

	public void setAttributes(Attribute[] attributes) {
		this.attributes = attributes;
	}


	public Address16 getAddress16() {
		return address16;
	}


	public void setAddress16(Address16 address16) {
		this.address16 = address16;
	}


	public Address64 getAddress64() {
		return address64;
	}


	public void setAddress64(Address64 address64) {
		this.address64 = address64;
	}


	public int getProfileId() {
		return profileId;
	}


	public void setProfileId(int profileId) {
		this.profileId = profileId;
	}


	public int getClusterId() {
		return clusterId;
	}


	public void setClusterId(int clusterId) {
		this.clusterId = clusterId;
	}


	public int getSourceEndpoint() {
		return sourceEndpoint;
	}


	public void setSourceEndpoint(int sourceEndpoint) {
		this.sourceEndpoint = sourceEndpoint;
	}


	public int getDestinationEndpoint() {
		return destinationEndpoint;
	}


	public void setDestinationEndpoint(int destinationEndpoint) {
		this.destinationEndpoint = destinationEndpoint;
	}


	public WriteAttributesResponse getCallback() {
		return callback;
	}


	public void setCallback(WriteAttributesResponse callback) {
		this.callback = callback;
	}


	public void handleZigBeeCommandResponse(int status, Address16 addr16,
			ZigBeeCommand zcmd, byte[] payload) {
		// TODO Auto-generated method stub
		
	}
	
}
