package ie.wombat.zigbee.zdo;

//import org.apache.log4j.Logger;


@SuppressWarnings("serial")
public class LQIResponse implements ZDOResponse {

	//private static final Logger log = Logger.getLogger(LQIResponse.class);
	
	private int status;
	private int nEntriesTotal;
	private int nEntriesReceived = 0;
	
	private int packetCounter = 0;
	
	private NeighborTableEntry[] neighborTableEntries = null;
	
	public LQIResponse () {
		
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
		
		packetCounter++;
		//log.info ("LQIResponse " + this + "  packetCounter=" + packetCounter);
		
		status = bytes[offset+0] & 0xff;
		
		if (status != 0x00) {
			return;
		}
		
		nEntriesTotal = bytes[offset+1] & 0xff;
		
		if (neighborTableEntries == null) {
			neighborTableEntries = new NeighborTableEntry[nEntriesTotal];
		}
		
		int startIndex = bytes[offset+2] & 0xff;
		
		int nEntriesThisPacket = bytes[offset+3] & 0xff;
		
		for (int i = 0; i < nEntriesThisPacket; i++) {
			NeighborTableEntry nte = new NeighborTableEntry(bytes,offset + 4 + i*22);
			if (neighborTableEntries[startIndex+i] == null) {
				neighborTableEntries[startIndex+i] = nte;
				nEntriesReceived++;
			}
		}
		
	}
	/**
	 * Return true of all NTEs have been set.
	 * @return
	 */
	public boolean isComplete () {
		if (neighborTableEntries == null) {
			return false;
		}
		/*
		for (int i = 0; i < neighborTableEntries.length; i++) {
			if (neighborTableEntries[i] == null) {
				return false;
			}
		}
		return true;
		*/
		return (nEntriesReceived == nEntriesTotal);
	}
	
	public String toString() {
		StringBuffer buf = new StringBuffer();
		for (int i = 0; i < nEntriesTotal; i++) {
			buf.append ("nte[" + i + "]={");
			if (neighborTableEntries[i] != null) {
				buf.append (neighborTableEntries[i].toString());
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


	
	
}
