package ie.wombat.zigbee.zdo;

//import ie.wombat.ha.ByteFormatUtils;

@SuppressWarnings("serial")
public class NetworkDiscoveryResponse implements ZDOResponse {

	private byte[] rawBytes;
	
	private int status;

	
	public NetworkDiscoveryResponse () {
	}
	
	public void addPacket(byte[] bytes, int offset) {
		this.status = bytes[offset+0] & 0xff;
		// Keep a copy of the raw bytes
		rawBytes = new byte[bytes.length];
		System.arraycopy(bytes, 0, rawBytes, 0, bytes.length);
	}

	public boolean isComplete() {
		return (rawBytes != null);
	}
	
	/*
	public String toString () {
		StringBuffer buf = new StringBuffer();
		buf.append (ByteFormatUtils.byteArrayToString(rawBytes));
		return buf.toString();
	}
	*/
	

	public int getStatus() {
		return status;
	}

	public void setStatus(int status) {
		this.status = status;
	}

	
}
