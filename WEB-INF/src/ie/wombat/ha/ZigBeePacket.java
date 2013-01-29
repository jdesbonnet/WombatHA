package ie.wombat.ha;

import ie.wombat.zigbee.address.Address16;
import ie.wombat.zigbee.address.Address64;

/**
 * Used to store the contents of a ZigBee packet received from the ZigBee NIC.
 * 
 * @author joe
 *
 */
public class ZigBeePacket {

	private int frameId;
	
	private Address64 sourceAddress64;
	private Address16 sourceAddress16;
	private Address64 destinationAddress64;
	private Address16 destinationAddress16;
	
	private int profileId;
	private int clusterId;
	
	private int sourceEndPoint;
	private int destinationEndPoint;
	
	private boolean encrypted;
	private boolean fromEndDevice;
	private boolean broadcast;
	private boolean acknowledgement;
	
	private byte[] payload;
	
	private int lqi;

	public int getFrameId() {
		return frameId;
	}

	public void setFrameId(int frameId) {
		this.frameId = frameId;
	}

	public Address64 getSourceAddress64() {
		return sourceAddress64;
	}

	public void setSourceAddress64(Address64 sourceAddress64) {
		this.sourceAddress64 = sourceAddress64;
	}

	public Address16 getSourceAddress16() {
		return sourceAddress16;
	}

	public void setSourceAddress16(Address16 sourceAddress16) {
		this.sourceAddress16 = sourceAddress16;
	}

	public Address64 getDestinationAddress64() {
		return destinationAddress64;
	}

	public void setDestinationAddress64(Address64 destinationAddress64) {
		this.destinationAddress64 = destinationAddress64;
	}

	public Address16 getDestinationAddress16() {
		return destinationAddress16;
	}

	public void setDestinationAddress16(Address16 destinationAddress16) {
		this.destinationAddress16 = destinationAddress16;
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

	public int getSourceEndPoint() {
		return sourceEndPoint;
	}

	public void setSourceEndPoint(int sourceEndPoint) {
		this.sourceEndPoint = sourceEndPoint;
	}

	public int getDestinationEndPoint() {
		return destinationEndPoint;
	}

	public void setDestinationEndPoint(int destinationEndPoint) {
		this.destinationEndPoint = destinationEndPoint;
	}

	public byte[] getPayload() {
		return payload;
	}

	public void setPayload(byte[] payload) {
		this.payload = payload;
	}

	public boolean isEncrypted() {
		return encrypted;
	}

	public void setEncrypted(boolean encrypted) {
		this.encrypted = encrypted;
	}

	public boolean isFromEndDevice() {
		return fromEndDevice;
	}

	public void setFromEndDevice(boolean fromEndDevice) {
		this.fromEndDevice = fromEndDevice;
	}

	public boolean isBroadcast() {
		return broadcast;
	}

	public void setBroadcast(boolean broadcast) {
		this.broadcast = broadcast;
	}

	public boolean isAcknowledgement() {
		return acknowledgement;
	}

	public void setAcknowledgement(boolean acknowledgement) {
		this.acknowledgement = acknowledgement;
	}

	public int getLqi() {
		return lqi;
	}

	public void setLqi(int lqi) {
		this.lqi = lqi;
	}

	
}
