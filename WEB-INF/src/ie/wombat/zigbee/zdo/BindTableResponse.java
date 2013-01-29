package ie.wombat.zigbee.zdo;

import java.io.Serializable;

//import org.apache.log4j.Logger;

//import ie.wombat.zigbee.zcl.ZCLStatus;

/**
 * Response to Bind Table Request (Cluster ID 0x0033). 
 * 
 * @author joe
 *
 */
@SuppressWarnings("serial")
public class BindTableResponse implements ZDOResponse, Serializable {

	//private static final Logger log = Logger.getLogger(BindTableResponse.class);
	
	private int status;
	private int nEntriesTotal;
	private int nEntriesReceived = 0;
	
	private int packetCounter = 0;
	
	private BindTableEntry[] bindTableEntries = null;
	
	public BindTableResponse () {
		
	}
	
	/**
	 * Get Bind Table Response ZigBee Specification 2.4.4.3.4 page 203. Cluster 0x8033.
	 * 
	 * @param bytes
	 * @param offset
	 */
	public void addPacket (byte[] bytes, int offset) {
		
		packetCounter++;
		//log.info ("BindTableResponse " + this + "  packetCounter=" + packetCounter);
		
		status = bytes[offset+0] & 0xff;
		
		if (status != 0x00) {
			return;
		}
		
		nEntriesTotal = bytes[offset+1] & 0xff;
		
		if (bindTableEntries == null) {
			bindTableEntries = new BindTableEntry[nEntriesTotal];
		}
		
		int startIndex = bytes[offset+2] & 0xff;
		
		int nEntriesThisPacket = bytes[offset+3] & 0xff;
		
		int bteOffset = offset + 4;
		for (int i = 0; i < nEntriesThisPacket; i++) {
			BindTableEntry bte = new BindTableEntry(bytes,bteOffset);
			if (bindTableEntries[startIndex+i] == null) {
				bindTableEntries[startIndex+i] = bte;
				nEntriesReceived++;
				bteOffset += bte.size();
			}
		}
		
	}
	/**
	 * Return true of all NTEs have been set.
	 * @return
	 */
	public boolean isComplete () {
		if (bindTableEntries == null) {
			return false;
		}
		return (nEntriesReceived == nEntriesTotal);
	}
	
	public String toString() {
		StringBuffer buf = new StringBuffer();
		
		buf.append ("nEntries=" + nEntriesTotal);
		
		for (int i = 0; i < nEntriesTotal; i++) {
			buf.append ("nte[" + i + "]={");
			if (bindTableEntries[i] != null) {
				buf.append (bindTableEntries[i].toString());
			}
			buf.append ("},\n");	
		}
		return buf.toString();
	}
	
	

	public int getStatus() {
		return status;
	}

	public void setStatus(int status) {
		this.status = status;
	}

	public BindTableEntry[] getEntries() {
		return bindTableEntries;
	}

	
	
}
