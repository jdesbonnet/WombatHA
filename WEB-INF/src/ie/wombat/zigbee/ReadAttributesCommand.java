package ie.wombat.zigbee;

import ie.wombat.ha.ZigBeeNIC;
import ie.wombat.zigbee.address.Address16;
import ie.wombat.zigbee.address.Address64;
import ie.wombat.zigbee.zcl.AttributeResponseDecode;
import ie.wombat.zigbee.zcl.AttributeValue;

import java.io.IOException;
import java.util.List;

import org.apache.log4j.Logger;

/**
 * Sends a ZigBee Read Attributes command to a profile/cluster/endpoint and
 * returns the result via a callback to a object implementing
 * {@link ReadAttributesResponse} interface. 
 * 
 * This version is a wrapper around {@link ZigBeeCommand}. 
 * @author joe
 *
 */
public class ReadAttributesCommand implements ZigBeeCommandResponse {
	
	private static Logger log = Logger.getLogger(ReadAttributesCommand.class);
	
	private Address16 address16;
	private Address64 address64;
	private int profileId;
	private int clusterId;
	private int sourceEndpoint;
	private int destinationEndpoint;
	private int[] attributeIds;

	private ReadAttributesResponse callback;
	
	private ZigBeeCommand zcmd;
	
	public ReadAttributesCommand(ZigBeeNIC nic) {
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
		
		byte[] command = new byte[1 + attributeIds.length*2];
		
		command[0] = 0x00; // Read Attribute Command (ZCL, table 2.9, page 16)
	
		for (int i = 0; i < attributeIds.length; i++) {
			command[1+i*2] = (byte)(attributeIds[i] & 0xff);
			command[2+i*2] = (byte)(attributeIds[i] >> 8);
		}
		
		// ZCL 2.4.1.1.1 "The frame type sub-field shall be set to indicate a profile wide command".
		zcmd.setClusterSpecific(false);
		
		zcmd.setCommand(command);
		
		// Call the handleResponse() method of this object when result is available.
		zcmd.setCallback(this);
		
		log.debug("exec(): calling exec() zcmd");
		zcmd.exec();
	}
	

	/**
	 * This is called when response to the Read Attributes command is received.
	 */
	public void handleZigBeeCommandResponse(int status, Address16 addr16, ZigBeeCommand zcmd, byte[] payload) {

		if (status != 0) {
			log.error("handleResponse(): received non-success status=0x" + Integer.toHexString(status));
			callback.handleReadAttributesResponse(status, addr16, this, null);
			return;
		}
		
		if (payload[2] == 0x01) {
			log.info("handleResponse(): cmd == Read Attribute Response (0x01) as expected");
		} else {
			log.error("handleResponse(): packet type is 0x" + Integer.toHexString(payload[2]));
			log.error("handleResponse(): ignoring packet");
			return;
		}
		
		log.info("handleResponse(): received expected ZigBee Attribute Response packet. Decoding... ");
		
		List<AttributeValue> attributes = AttributeResponseDecode.decode(payload, 3, payload.length);
		log.debug ("handleResponse(): found " + attributes.size() + " attributes.");
		
		callback.handleReadAttributesResponse(0, zcmd.getCommandAck().addr16, this, attributes);
	}
	
	//
	// Accessor methods
	//
	
	public int[] getAttributeIds() {
		return attributeIds;
	}

	public void setAttributeIds(int[] attributeIds) {
		this.attributeIds = attributeIds;
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


	public ReadAttributesResponse getCallback() {
		return callback;
	}


	public void setCallback(ReadAttributesResponse callback) {
		this.callback = callback;
	}
	
}
