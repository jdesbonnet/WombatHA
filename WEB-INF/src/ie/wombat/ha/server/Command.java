package ie.wombat.ha.server;

import ie.wombat.ha.ByteFormatUtils;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

import org.hibernate.annotations.GenericGenerator;

/**
 * TODO: thinking we need one more level of abstraction. Should we really
 * be diving into the details of ZigBee here. Eg we want to to ping a device,
 * or get it's model name. 
 * 
 * TODO: there are a *lot* of fields here that should really be broken into
 * separate event records.
 * @author joe
 *
 */
@Entity
public class Command {

	@Id
	@GeneratedValue(generator="increment")
	@GenericGenerator(name="increment", strategy = "increment")
	private Long id;
	
	@ManyToOne 
	private Network network;
	
	@Column (name="queued_t")
	private Date queuedTime = new Date();
	
	@Column (name="exec_t")
	private Date execTime;
	
	@Column (name="ack_t")
	private Date ackTime;
	
	@Column (name="response_t")
	private Date responseTime;
	
	/**
	 * Status of physical attempt to transmit packet (eg if NIC disconnected error
	 * goes here).
	 */
	@Column (name="send_s")
	private int sendStatus = -1;
	
	/**
	 * The ACK status of the command.
	 */
	@Column (name="ack_s")
	private int ackStatus = -1;
	
	@Column (name="retry_c")
	private int retryCount = -1;
	
	@Column (name="dsc_s")
	private int discoveryStatus = -1;
	
	@ManyToOne 
	private Device dstDevice;
	
	private int profileId;
	private int clusterId;
	private int srcEp;
	private int dstEp;
	
	private int frameId;
	private String commandPayload;
	private String responsePayload;

	
	public byte[] getPayloadBytes () {
		String payload = getCommandPayload();
		payload = payload.replaceAll("\\s+","");
		byte[] payloadBytes = new byte[payload.length()/2];
		
		int i;
		for (i = 0; i < payloadBytes.length; i++) {
			payloadBytes[i] = (byte)Integer.parseInt(payload.substring(i*2,i*2+2),16);
		}
		
		return payloadBytes;
		
	}
	
	/**
	 * Convenience method that sets the response payload in the format in which
	 * it is received, ie byte array. Stored as hex string internally (for the 
	 * moment).
	 * @param bytes
	 */
	public void setResponsePayload (byte[] bytes) {
		StringBuffer buf = new StringBuffer();
		for (int i = 0; i < bytes.length; i++) {
			buf.append (ByteFormatUtils.formatHexByte(bytes[i]));
			buf.append (" ");
		}
		// Remove tailing space
		buf.setLength(buf.length()-1); 
		setResponsePayload(buf.toString());
	}
	
	public Long getId() {
		return id;
	}
	public void setId(Long id) {
		this.id = id;
	}
	public Network getNetwork() {
		return network;
	}
	public void setNetwork(Network network) {
		this.network = network;
	}
	public Date getQueuedTime() {
		return queuedTime;
	}
	public void setQueuedTime(Date queuedTime) {
		this.queuedTime = queuedTime;
	}
	public Date getExecTime() {
		return execTime;
	}
	public void setExecTime(Date execTime) {
		this.execTime = execTime;
	}
	public Device getDstDevice() {
		return dstDevice;
	}
	public void setDstDevice(Device dstDevice) {
		this.dstDevice = dstDevice;
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
	public int getSrcEp() {
		return srcEp;
	}
	public void setSrcEp(int srcEp) {
		this.srcEp = srcEp;
	}
	public int getDstEp() {
		return dstEp;
	}
	public void setDstEp(int dstEp) {
		this.dstEp = dstEp;
	}
	public int getFrameId() {
		return frameId;
	}
	public void setFrameId(int frameId) {
		this.frameId = frameId;
	}
	public String getCommandPayload() {
		return commandPayload;
	}
	public void setCommandPayload(String commandPayload) {
		this.commandPayload = commandPayload;
	}
	public String getResponsePayload() {
		return responsePayload;
	}
	public void setResponsePayload(String responsePayload) {
		this.responsePayload = responsePayload;
	}
	public int getSendStatus() {
		return sendStatus;
	}
	public void setSendStatus(int sendStatus) {
		this.sendStatus = sendStatus;
	}
	public int getAckStatus() {
		return ackStatus;
	}
	public void setAckStatus(int ackStatus) {
		this.ackStatus = ackStatus;
	}

	
	public Date getAckTime() {
		return ackTime;
	}
	public void setAckTime(Date ackTime) {
		this.ackTime = ackTime;
	}
	public Date getResponseTime() {
		return responseTime;
	}
	public void setResponseTime(Date responseTime) {
		this.responseTime = responseTime;
	}
	public int getRetryCount() {
		return retryCount;
	}
	public void setRetryCount(int retryCount) {
		this.retryCount = retryCount;
	}
	public int getDiscoveryStatus() {
		return discoveryStatus;
	}
	public void setDiscoveryStatus(int discoveryStatus) {
		this.discoveryStatus = discoveryStatus;
	}
	

}
