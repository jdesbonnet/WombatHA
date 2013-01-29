package ie.wombat.zigbee.zdo;

import java.io.Serializable;

import org.apache.log4j.Logger;

import ie.wombat.zigbee.address.Address16;

@SuppressWarnings("serial")
public class SimpleDescriptorResponse implements ZDOResponse, Serializable {

	private static Logger log = Logger.getLogger(SimpleDescriptorResponse.class);
	
	private int status;
	private Address16 addr16;
	private int endPoint;
	private int profileId;
	private int applicationDeviceId;
	private int applicationVersion;
	private int[] inputClusters;
	private int[] outputClusters;
	private boolean completeFlag = false;
	
	public SimpleDescriptorResponse () {
	}
	
	public void addPacket(byte[] bytes, int offset) {
		
		// Example:
		// 00 00 [addr16: 52 0b] 14 [ep: 01] [profile: 04 01] [appDevId: 09 00] 
		// 00 06 00 00 03 00 04 00 05 00 06 00 02 07 00
		
		this.status = bytes[offset+0] & 0xff;

		this.addr16 = new Address16(bytes,offset+1, true /* lsbfirst */);
		
		//int len = bytes[offset + 3] & 0xff;
		
		int p = offset + 4;
		
		endPoint = bytes[p++];
		log.trace("endPoint=" + endPoint);
		
		profileId = bytes[p++]&0xff;
		profileId |= (bytes[p++]&0xff) << 8;
		log.trace("profileId=0x" + Integer.toHexString(profileId));
			
		applicationDeviceId  = bytes[p++]&0xff;
		applicationDeviceId |= (bytes[p++]&0xff) << 8;
		log.trace("applicationDeviceId=0x" + Integer.toHexString(applicationDeviceId));
		
		
		// This was omitted until 25 April 2012, yet it somehow worked.
		// It worked because the applicationVersion is usually 0 so the 
		// inputClusterCount is interpreted as 0. So the inputClusters
		// are missed as inputClusters but interpreted as outputClusters
		// instead.
		applicationVersion = bytes[p++]&0xff;
		log.trace("applicationVersion=0x" + Integer.toHexString(applicationVersion));

		int clusterCount = bytes[p++] & 0xff;
		inputClusters = new int[clusterCount];
		log.trace("inputClusterCount=" + clusterCount);
		
		int i;
		for (i = 0; i < clusterCount; i++) {
			inputClusters[i] = bytes[p++]&0xff;
			inputClusters[i] |= (bytes[p++]&0xff) << 8;
			log.trace ("    input cluster 0x" + Integer.toHexString(inputClusters[i]));
		}
		
		clusterCount = bytes[p++] & 0xff;
		outputClusters = new int[clusterCount];
		log.trace("outputClusterCount=" + clusterCount);

		for (i = 0; i < clusterCount; i++) {
			outputClusters[i] = bytes[p++]&0xff;
			outputClusters[i] += (bytes[p++]&0xff)<<8;
			log.trace ("    output cluster 0x" + Integer.toHexString(outputClusters[i]));
		}
		
		log.trace ("SimpleDescriptorResponse is complete");
		completeFlag = true;
	}

	public boolean isComplete() {
		return completeFlag;
	}
	
	public String toString () {
		int i;

		StringBuffer buf = new StringBuffer();
		buf.append("ep=" + endPoint);
		buf.append(" profileId=0x" + Integer.toHexString(profileId));
		buf.append(" inputClusters={");
		if (inputClusters == null) {
			buf.append ("null");
		} else {
			for (i = 0; i < inputClusters.length; i++) {
				buf.append ("0x" + Integer.toHexString(inputClusters[i]) + " ");
			}
		}
		buf.append ("}");
		
		buf.append(" outputClusters={");
		if (outputClusters == null) {
			buf.append ("null");
		} else {
			for (i = 0; i < outputClusters.length; i++) {
				buf.append ("0x" + Integer.toHexString(outputClusters[i]) + " ");
			}
		}
		
		buf.append ("}");
		
		return buf.toString();
		
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

	public int getEndPoint() {
		return endPoint;
	}

	public void setEndPoint(int endPoint) {
		this.endPoint = endPoint;
	}

	public int getProfileId() {
		return profileId;
	}

	public void setProfileId(int profileId) {
		this.profileId = profileId;
	}

	public int getApplicationDeviceId() {
		return applicationDeviceId;
	}

	public void setApplicationDeviceId(int applicationDeviceId) {
		this.applicationDeviceId = applicationDeviceId;
	}

	
	public int getApplicationVersion() {
		return applicationVersion;
	}

	public void setApplicationVersion(int applicationVersion) {
		this.applicationVersion = applicationVersion;
	}

	public int[] getInputClusters() {
		return inputClusters;
	}

	public void setInputClusters(int[] inputClusters) {
		this.inputClusters = inputClusters;
	}

	public int[] getOutputClusters() {
		return outputClusters;
	}

	public void setOutputClusters(int[] outputClusters) {
		this.outputClusters = outputClusters;
	}

	
	
	
}
