package ie.wombat.ha.nic;


import java.io.IOException;

/**
 * Objects that can physically communicate with the NIC UART implement this
 * class. The interface allows the sending and receiving of whole (escaped)
 * API frames. 
 *   
 * @author joe
 *
 */
public interface UARTAdapter {

	/**
	 * Transmit API frame to NIC UART.
	 * 
	 * @param apiFrameData  The full API packet (including Start-of-Packet 
	 * delimiter, checksum and escaping).
	 * @param frameLen Length of escaped API packet.
	 * @throws IOException
	 */
	public void txAPIFrame (byte[] apiFrameData, int frameLen) throws IOException;
	

	/**
	 * Set the {@link APIFrameListener} object to be notified when an 
	 * API frame received from the NIC UART is received.
	 * 
	 * @param listener
	 */
	public void setRxAPIFrameListener (APIFrameListener listener);
	
	/**
	 * EXPERIMENTAL:
	 * Allows a name to be attached. This is for thread based adapters, so that
	 * the thread name can be assigned for debugging purposes.
	 * @param name
	 */
	public void setName (String name);
	
	/**
	 * Close the adapter releasing any associated resources. Untested.
	 */
	public void close();
}
