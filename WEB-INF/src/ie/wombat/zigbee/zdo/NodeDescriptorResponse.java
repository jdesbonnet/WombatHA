package ie.wombat.zigbee.zdo;

//import ie.wombat.ha.ByteFormatUtils;
import ie.wombat.zigbee.address.Address16;

@SuppressWarnings("serial")
public class NodeDescriptorResponse implements ZDOResponse {

	private byte[] nodeDescriptorBytes;
	
	private int status = -1;
	private Address16 addr16;
	private int nodeType;
	private boolean complexDescriptorAvailable;
	private boolean userDescriptorAvailable;
	private int frequencyBand;
	
	//private int macCapability;
	//private boolean alternatePANCoordinator;
	private int manufacturerCode;
	private int maxBufferSize;
	private int maxIncomingTransferSize;
	// private int serverMask;
	private int maxOutgoingTransferSize;
	
	// Descriptor capability field
	private int descriptorCapabilityField;
	private boolean extendedActiveEndpointListAvailable;
	private boolean extendedSimpleDescriptorListAvailable;
	
	public NodeDescriptorResponse () {
	}
	
	public void addPacket(byte[] bytes, int offset) {

		this.status = bytes[offset+0] & 0xff;
		
		if (status != 0) {
			return;
		}

		this.addr16 = new Address16(bytes,offset+1, true /* lsbfirst */);
				
		int p = offset + 3;
		
		nodeType = bytes[p] & 0x07;
		
		complexDescriptorAvailable = (bytes[p] & 0x08) != 0;
		userDescriptorAvailable = (bytes[p] & 0x10) != 0;
		
		p++;
		frequencyBand = bytes[p++];
		
		// MAC Capability, skip
		p++;
		
		manufacturerCode = bytes[p++];
		manufacturerCode += bytes[p++]<<8;
		
		maxBufferSize = bytes[p++];
		
		// Keep a copy of the raw bytes
		nodeDescriptorBytes = new byte[bytes.length];
		System.arraycopy(bytes, 0, nodeDescriptorBytes, 0, bytes.length);
	}

	public boolean isComplete() {
		return (status>0);
	}
	
	public String toString () {
		StringBuffer buf = new StringBuffer();
		buf.append(" nodeType=" + nodeType + " ");
		if (nodeDescriptorBytes == null) {
			buf.append ("null");
		} else {
			//buf.append (ByteFormatUtils.byteArrayToString(nodeDescriptorBytes));
		}
		buf.append (" addr16=" + addr16);
		buf.append (" complexDescriptorAvailable=" + complexDescriptorAvailable);
		buf.append (" userDescriptorAvailable=" + userDescriptorAvailable);
		buf.append (" frequencyBand=" + frequencyBand);
		buf.append (" manufacturerCode=" + manufacturerCode);
		buf.append (" maxBufferSize=" + maxBufferSize);
		
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

	


	
	
}
