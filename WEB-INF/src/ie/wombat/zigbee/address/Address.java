package ie.wombat.zigbee.address;

/**
 * Common methods implemented by ZigBee address classes.
 * 
 * @author joe
 *
 */
public interface Address {

	/**
	 * Get bytes with must significant byte first
	 * @return Return address octets / bytes with the most significant byte at index 0. This 
	 * is the norm in the XBee binary API.
	 */
	public byte[] getBytesMSBF();
	
	/**
	 * Get bytes with least significant byte first
	 * @return Return address octets / bytes with the least significant byte at index 0. This
	 * is the norm in the ZigBee standard.
	 */
	public byte[] getBytesLSBF();
}
