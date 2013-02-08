package ie.wombat.ha;

import java.io.IOException;

import ie.wombat.ha.nic.APIFrameListener;
import ie.wombat.ha.nic.UARTAdapter;
import ie.wombat.zigbee.address.Address16;
import ie.wombat.zigbee.address.Address64;

/**
 * TODO: move to nic package.
 * 
 * Common interface which all ZigBee NIC drivers implement. Application code can send arbitrary ZigBee packets using
 * the {@link #sendZigBeeCommand()} method and get notifications of incoming ZigBee packets by registering as a
 * listener (listeners must implement the {@link ZigBeePacketListener} interface. An optional {@link ZigBeePacketFilter}
 * can be applied to limit received packet notifications to only those of interest to the application.
 * 
 * Also application code can register to get notification of low level NIC API frames and can transmit a NIC
 * API frame to the NIC using {@link #sendAPIFrame(byte[], int)}
 * 
 * @author joe
 *
 */
public interface ZigBeeNIC {

	/**
	 * Send ZigBee command to the device with the 64bit IEEE address specified. If the
	 * 16bit network address is known use that instead.
	 * 
	 * @param addr64 64bit IEEE unique device address or {@link Address64#BROADCAST} for broadcast
	 * @param clusterId Cluster ID
	 * @param profileId Profile ID
	 * @param srcEp Source End Point
	 * @param dstEp Destination End Point
	 * @param command ZCL command
	 * @return API frame ID used to link subsequent (optional) acknowledgement
	 * @throws IOException
	 */
	int sendZigBeeCommand (Address64 addr64, int clusterId, int profileId,
			int srcEp, int dstEp, byte[] command) throws IOException ;
	
	/**
	 * Send a ZigBee command on the network using the 16bit network address.
	 * 
	 * @param addr16 16bit network address
	 * @param clusterId Cluster ID
	 * @param profileId Profile ID
	 * @param srcEp Source End Point
	 * @param dstEp Destination End Point
	 * @param command ZCL command
	 * @return API frame ID used to link subsequent (optional) acknowledgement
	 * @throws IOException
	 */
	int sendZigBeeCommand (Address16 addr16, int clusterId, int profileId,
			int srcEp, int dstEp, byte[] command) throws IOException ;
	
	/**
	 * Send a ZigBee command on the network. TODO: explain why two addresses?.
	 * 
	 * @param addr64 64bit address
	 * @param addr16 16bit network address 
	 * @param clusterId Cluster ID
	 * @param profileId Profile ID
	 * @param srcEp Source End Point
	 * @param dstEp Destination End Point
	 * @param command ZCL command
	 * @return 802.15.4 (?) Frame ID used to link subsequent (optional) acknowledgement
	 * @throws IOException
	 */
	int sendZigBeeCommand (Address64 addr64, Address16 addr16, int clusterId, int profileId,
			int srcEp, int dstEp, byte[] command) throws IOException ;
	/**
	 * Add an object to which any incoming ZigBee packets will be notified.
	 * 
	 * @param listener
	 */
	void addZigBeePacketListener (ZigBeePacketListener listener);
	
	/**
	 * Add an object to which incoming ZigBee packets will be notified if they pass
	 * through the supplied filter.
	 * 
	 * @param listener
	 * @param filter A packet filter. If 'null' is passed, no filter is applied.
	 */
	void addZigBeePacketListener (ZigBeePacketListener listener, ZigBeePacketFilter filter);
	
	/**
	 * Remove a previously registered listener.
	 * 
	 * @param listener
	 */
	void removeZigBeePacketListener (ZigBeePacketListener listener);
	
	
	void addAcknowledgementListener (AcknowledgementListener listener);
	void addAcknowledgementListener (AcknowledgementListener listener, int frameId);
	void removeAcknowledgementListener (AcknowledgementListener listener);
	
	//
	// 
	//
	
	// New generic add/remove listener. Rathern than add/remove methods specific to
	// each type of listener. Keeps the API simpler.
	void addListener (Listener listener);
	void removeListener (Listener listener);
	
	//public void getAddr16 (Address64 addr64);
	// Methods related to the lower level NIC API. Application code should avoid
	// using these, but are included for debugging, network repeaters etc.
	// TODO: fix naming: frame vs packet.
	
	/**
	 * Send an API frame to NIC. The formatting of the API frame is implementation
	 * dependent.
	 * 
	 * @param int apiFrameId
	 * @param frame API frame (including SOF and FCS)
	 * @param frameLen
	 * @throws IOException
	 */
	void sendAPIFrame(byte[] frame, int frameLen) throws IOException;
	
	/**
	 * Send an API frame to NIC. The formatting of the API frame is implementation
	 * dependent. In this version of the method, the apiFrameId is obtained from
	 * an internal sequence counter in the implementing object.
	 * 
	 * @param int apiFrameId
	 * @param frame API frame (including SOF and FCS)
	 * @param frameLen
	 * @throws IOException
	 */
	//void sendAPIPacket (byte[] frame, int frameLen) throws IOException;
	
	/**
	 * This is normally called by a thread reading the NIC UART. Once a API packet is injected
	 * here it is passed up the layers of the application. This is not normally used by application
	 * code.
	 * 
	 * @param frame
	 * @param frameLen
	 */
	void handleAPIFrame(byte[] frame, int frameLen);
	void addAPIPacketListener(APIFrameListener listener);
	void removeAPIPacketListener (APIFrameListener listener);
	
	/**
	 * Get the UART adapter class for this NIC. The adapter is the object that physically
	 * communicates with the NIC hardware. 
	 * 
	 * @return
	 */
	public UARTAdapter getUARTAdapter();
	
	/**
	 * Send a dummy command to the NIC to verify connectivity.
	 * 
	 * @throws IOException
	 */
	public void ping() throws IOException ;
	
	/**
	 * Perform hardware reset on the NIC.
	 * @throws IOException
	 */
	public void reset() throws IOException;
	
	/**
	 * Perform comprehensive test on NIC.
	 */
	public void test() throws IOException;
	
	/**
	 * Get the time (Java ms from epoch time) at which the last good API packet
	 * was received from the NIC hardware. Used to detect NIC or comms channel
	 * failure.
	 * 
	 * @return Time in ms at which last good API packet was received.
	 */
	public long getLastRxTime ();
	
	/**
	 * Release any resources relating to the NIC driver. Not generally expected to be
	 * used, but is included for completeness.
	 */
	void close();
	
}
