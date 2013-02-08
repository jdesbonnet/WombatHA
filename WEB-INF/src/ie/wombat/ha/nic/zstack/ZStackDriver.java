package ie.wombat.ha.nic.zstack;

import ie.wombat.ha.AcknowledgementListener;
import ie.wombat.ha.ByteFormatUtils;
import ie.wombat.ha.Listener;
import ie.wombat.ha.ZigBeePacket;
import ie.wombat.ha.ZigBeePacketFilter;
import ie.wombat.ha.ZigBeePacketListener;
import ie.wombat.ha.ZigBeeNIC;
import ie.wombat.ha.nic.APIFrameListener;
import ie.wombat.ha.nic.NICErrorListener;
import ie.wombat.ha.nic.UARTAdapter;
import ie.wombat.ha.nic.xbee.XBeeUtil;
import ie.wombat.zigbee.Ack;
import ie.wombat.zigbee.address.Address16;
import ie.wombat.zigbee.address.Address64;
import ie.wombat.zigbee.zcl.Cluster;
import ie.wombat.zigbee.zcl.Profile;


import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import org.apache.log4j.Logger;

/**
 * Z-Stack driver using Monitor and Test API v1.4 (TI document SWRA198). Using
 * Cleode UBee coordinator (Z-Stack unknown version) for testing.
 * 
 * Design notes: there will be one of these objects in active memory for each network.
 * So must keep memory footprint as low as possible. Consider replacing HashMap objects
 * with a simple array and iterate to lookup. Probably net gain in CPU cycles too for
 * tables < 16 entries (ie the max for a typical network).
 * 
 * Design note: There is starting to be a lot of common code between this driver
 * and the XBee driver. Can we move common code to a superclass?
 * 
 * @author Joe Desbonnet, jdesbonnet@gmail.com
 *
 */
public class ZStackDriver implements ZigBeeNIC, APIFrameListener {

	private static Logger log = Logger.getLogger(ZStackDriver.class);
	
	/** 
	 * Experimental feature to echo outgoing (server to XBee) packets
	 * to {@link APIFrameListener} objects. This is to facilitate
	 * remote debug. 
	 */
	private static final boolean ECHO_OUTGOING_PACKETS_TO_LISTENERS = true;
	
	private static final int START_OF_FRAME = 0xFE;
	
	/** Objects that have registered to receive notification of arrival of ZigBee packets */
	private ArrayList<ZigBeePacketListener> zigbeePacketListeners = new ArrayList<ZigBeePacketListener>();
	
	/** Objects that have registered to receive notification of arrival of NIC ACKs 
	 * (currently not implemented for ZStack driver)
	 */
	private ArrayList<AcknowledgementListener> apiAckListeners = new ArrayList<AcknowledgementListener>();
	
	/**
	 * Objects that have registered to receive notification of NIC errors.
	 */
	private ArrayList<NICErrorListener> errorListeners = new ArrayList<NICErrorListener>();
	
	/**
	 * Filter which are applied to ZigBee packets before notifying {@link ZigBeePacketListener}
	 */
	private HashMap<ZigBeePacketListener,ZigBeePacketFilter> filters = new HashMap<ZigBeePacketListener,ZigBeePacketFilter>();
	
	/** 
	 * Used in {@link #addAcknowledgementListener(AcknowledgementListener, int)} to store
	 * the frame ID for which to filter for.
	 */
	private HashMap<AcknowledgementListener, Integer> ackFrameIdFilter = new HashMap<AcknowledgementListener, Integer>();

	
	/**
	 * Clients can subscribe to get all API API packets from the UART. Used in
	 * AT command etc.
	 */
	private ArrayList<APIFrameListener> apiListeners = new ArrayList<APIFrameListener>();
	
	/** 
	 * Object which sends and receives API packets to/from the NIC UART. This might be
	 * a very direct path (eg using the gnu.io.* to talk directly to a serial port) or
	 * very indirect using the HTTP bridge. 
	 * */
	private UARTAdapter uartAdapter;
	
	/**
	 * Time at which the last good packet was received form the NIC. Used to detect
	 * problems.
	 */
	private long lastRxTime = 0;
	
	private int seqId = 1;
	
	/**
	 * Create driver. This is to be called from a factory object. 
	 * @param uartAdapter Object which sends and receives API packets from the XBee.
	 */
	public ZStackDriver (UARTAdapter uartAdapter) {
		this.uartAdapter = uartAdapter;
		uartAdapter.setRxAPIFrameListener(this);
		
		
		// To communicate with XBee devices, must register 
		// EP 230 (command EP) and 232 (data EP)
		{
		int[] inClusters = {0x0021};
		int[] outClusters = {};
		AF_REGISTER(
				230, // EP 
				0xC105, // Profile ID
				0x0000, //appDeviceId, 
				0x01, //appDevVer
				0x00, //latencyReq, 
				inClusters, 
				outClusters
				);
		}
		
		// Cluster 0x0011 on EP 232 (data) is where a XBee running AT firmware will send
		// serial port data.
		{
		int[] inClusters = {0x0011};
		int[] outClusters = {0x0011}; // 22dec added 0x11 on out cluster
		AF_REGISTER(
				232, // EP 
				0xC105, // Profile ID
				0x0000, //appDeviceId, 
				0x01, //appDevVer
				0x00, //latencyReq, 
				inClusters, 
				outClusters
				);
		}
		
		
	}
	
	
	/**
	 * Send ZigBee command. Use this if 16 bit network address is unknown.  If both
	 * 16bit and 64bit addresses are known prefer the 16 bit address version of this
	 * method (it is more network efficient).
	 * 
	 * @param addr64 64 bit IEEE unique device address
	 * @param clusterId
	 * @param profileId
	 * @param srcEp
	 * @param dstEp
	 * @param command
	 * @return
	 * @throws IOException
	 */
	public int sendZigBeeCommand(Address64 addr64,
			int clusterId, int profileId, int srcEp, int dstEp,
			byte[] command) throws IOException {
		return sendZigBeeCommand(addr64,Address16.UNKNOWN,
				clusterId, profileId, srcEp,dstEp,command);
	}
	
	/**
	 * Send ZigBee command. Use this if 16 bit network address is known. If both
	 * 16bit and 64bit addresses are known prefer the 16 bit address version of this
	 * method (it is more network efficient).
	 * 
	 * @param addr16 16 bit network address
	 * @param clusterId
	 * @param profileId
	 * @param srcEp
	 * @param dstEp
	 * @param command
	 * @return
	 * @throws IOException
	 */
	public int sendZigBeeCommand(Address16 addr16,
			int clusterId, int profileId, int srcEp, int dstEp,
			byte[] command) throws IOException {
		return sendZigBeeCommand(Address64.UNKNOWN,addr16,
				clusterId, profileId, srcEp,dstEp,command);
	}
	
	
	/**
	 * This is a private method which implements the {@link #sendZigBeeCommand(Address64, int, int, int, int, byte[]) } and
	 * {@link #sendZigBeeCommand(Address16, int, int, int, int, byte[]) }
	 * @param address64
	 * @param address16
	 * @param clusterId
	 * @param profileId
	 * @param srcEp
	 * @param dstEp
	 * @param command
	 * @return
	 * @throws IOException
	 */
	public int sendZigBeeCommand(Address64 address64,
			Address16 address16,
			int clusterId, int profileId, int srcEp, int dstEp,
			byte[] command) throws IOException {

		log.trace("sendZigBeeCommand(addr64=" + address64 + ",addr16=" + address16
				 + ",clusterId=0x" + Integer.toHexString(clusterId)
				 + ",profileId=0x" + Integer.toHexString(profileId)
				 + ",srcEp=" + srcEp
				 + ",dstEp=" + dstEp
				 + ",seqId=0x" + Integer.toHexString(seqId&0xff)
				 + ",command=" + ByteFormatUtils.byteArrayToString(command)
				 );
		
		// Construct API Frame
		// Frame = {SOF, LEN, CMD, DATA, FCS}
		// SOF = 0xFE
		// LEN = length of DATA
		
		byte[] frame = new byte[14 + command.length + 1];
		
		int packetLen = 10 + command.length;
		
		frame[0] = (byte)0xFE; // Start-of-Frame
		
		// Length (frame excluding SOF, length and command 
		frame[1] = (byte)packetLen;

		// Command (AF_DATA_REQUEST)
		frame[2] = 0x24;
		frame[3] = 0x01;
		
		// header bytes 4,5 are 16 bit destination address
		System.arraycopy(address16.getBytesLSBF(), 0, frame, 4, 2);
		
		frame[6] = (byte)dstEp;
		frame[7] = (byte)srcEp;
		
		frame[8] = (byte)(clusterId & 0xff);
		frame[9] = (byte)(clusterId>>8);
		
		//frame[10] = 0x01; //seqId
		frame[10] = (byte)seqId++;
		
		// Options
		// x--- ----  Skip routing
		// -x-- ----  APS security
		// --x- ----  Discover route
		// ---x ----  APS ack
		frame[11] = 0x00; 
		//frame[11] = 0x10; // Experimental (enable APS ACK)

		
		frame[12] = 0x08; // radius
		
		frame[13] = (byte)command.length; // Length of payload
		
		
		System.arraycopy(command, 0, frame, 14, command.length);
		
		// Calculate frame checksum (XOR of all bytes excluding the SOF)
		int cs=0;
		for (int i = 1; i < frame.length-1; i++) {
			cs ^= (frame[i] & 0xff); // TODO: do we need the &0xff?
		}
		frame[14+command.length] = (byte)cs;
		
		sendAPIFrame(frame, frame.length);
		
		return 0;
	}

	//
	// Z-Stack Monitor and Test API specific commands
	//
	
	/**
	 * AF_REGISTER sect 3.2.1.1, page 5.
	 * 
	 * @param endPoint
	 * @param profileId
	 * @param appDeviceId 16 bit value that specifies the device description ID for this end point (?)
	 * @param appDevVer
	 * @param latencyReq 0x00 no latency, 0x01 fast beacons, 0x02 slow beacons
	 * @param inClusters
	 * @param outClusters
	 */
	public void AF_REGISTER (int endPoint, int profileId, int appDeviceId, int appDevVer,
			int latencyReq, int[] inClusters, int[] outClusters) {
		int i;
		byte[] frame = new byte[4+9+inClusters.length*2 + outClusters.length*2 + 1];
		
		// SOF
		frame[0] = (byte)0xfe;
		
		// LEN
		frame[1] = (byte)(9 + inClusters.length*2 + outClusters.length*2);
		
		// AF_REGISTER command
		frame[2] = 0x24; 
		frame[3] = 0x00;
		
		frame[4] = (byte)endPoint;
		
		frame[5] = (byte)(profileId&0xff);
		frame[6] = (byte)((profileId>>8)&0xff);

		frame[7] = (byte)(appDeviceId & 0xff);
		frame[8] = (byte)((appDeviceId>>8) & 0xff);

		frame[9] = (byte)appDevVer;
		frame[10] = (byte)latencyReq;
		frame[11] = (byte)inClusters.length;
		int ptr = 12;
		
		for (i = 0; i < inClusters.length; i++) {
			frame[ptr+i*2] = (byte)(inClusters[i] & 0xff);
			frame[ptr+i*2+1] = (byte)((inClusters[i]>>8) & 0xff);
		}
		ptr += inClusters.length*2;
		
		frame[ptr] = (byte)outClusters.length;
		for (i = 0; i < outClusters.length; i++) {
			frame[ptr+i*2] = (byte)(outClusters[i] & 0xff);
			frame[ptr+i*2+1] = (byte)((outClusters[i]>>8) & 0xff);
		}
		ptr += outClusters.length*2;
		
		frame[ptr] = (byte)calculateCheckum(frame,1 , frame.length-2);
		
		try {
			sendAPIFrame(frame,frame.length);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	public void UTIL_GET_DEVICE_INFO () {
		byte[] command = {(byte)0xfe,0x00,0x27,0x00,0x00};
		command[4] = (byte)calculateCheckum(command,1 , 3);
		
		try {
			sendAPIFrame(command,5);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	
	
	// Experiment for ZStack NIC. Send UTIL_ADDRMGR_NWK_ADDR_LOOKUP
	public void UTIL_CALLBACK_SUB_CMD () throws IOException {
		log.debug ("UTIL_CALLBACK_SUB_CMD()");

		byte[] zstackcmd = new byte[8]; // SOF + LEN + 2xCMD + payload + FCS
		zstackcmd[0] = (byte)0xfe; //SOF
		zstackcmd[1] = 0x03; // length 3
		zstackcmd[2] = 0x27; // CMD0
		zstackcmd[3] = 0x06; // CMD1
		zstackcmd[4] = (byte)0xff;
		zstackcmd[5] = (byte)0xff;
		zstackcmd[6] = 1;
		zstackcmd[7] = (byte)calculateCheckum(zstackcmd, 1, 6);
		sendAPIFrame(zstackcmd, zstackcmd.length);
	}
	
	// Experiment for ZStack NIC. Send UTIL_ADDRMGR_NWK_ADDR_LOOKUP
	public void UTIL_ADDRMGR_EXT_ADDR_LOOKUP (Address64 addr64) throws IOException {
		log.debug ("UTIL_ADDRMGR_EXT_ADDR_LOOKUP(" + addr64 + ")");
		byte[] addr64bytes = addr64.getBytesLSBF();
		byte[] zstackcmd = new byte[15]; // SOF + LEN + 2xCMD + payload + FCS
		zstackcmd[0] = (byte)0xfe; //SOF
		zstackcmd[1] = 0x0a; // length 10
		zstackcmd[2] = 0x25; // CMD0
		zstackcmd[3] = 0x00; // CMD1
		System.arraycopy(addr64bytes, 0, zstackcmd, 4, 8);
		
		zstackcmd[12] = 0; // ReqType
		zstackcmd[13] = 0; // StartIndex
		
		zstackcmd[14] = (byte)calculateCheckum(zstackcmd, 1, 13);
		sendAPIFrame(zstackcmd, zstackcmd.length);
	}
	
	// Experiment for ZStack NIC. Send UTIL_ADDRMGR_NWK_ADDR_LOOKUP
	public void UTIL_APSME_LINK_KEY_DATA_GET (Address64 addr64) throws IOException {
		log.debug ("UTIL_APSME_LINK_KEY_DATA_GET(" + addr64 + ")");
		byte[] addr64bytes = addr64.getBytesLSBF();
		byte[] zstackcmd = new byte[13]; // SOF + LEN + 2xCMD + payload + FCS
		zstackcmd[0] = (byte)0xfe; //SOF
		zstackcmd[1] = 0x08; // length 8
		zstackcmd[2] = 0x27; // CMD0
		zstackcmd[3] = 0x44; // CMD1
		System.arraycopy(addr64bytes, 0, zstackcmd, 4, 8);
		zstackcmd[12] = (byte)calculateCheckum(zstackcmd, 1, 11);
		sendAPIFrame(zstackcmd, zstackcmd.length);
	}
	
	// Experiment for ZStack NIC. Send UTIL_ADDRMGR_NWK_ADDR_LOOKUP
	public void UTIL_GET_NV_INFO () throws IOException {
		log.debug ("UTIL_GET_NV_INFO ()");
		
		byte[] zstackcmd = new byte[13]; // SOF + LEN + 2xCMD + payload + FCS
		zstackcmd[0] = (byte)0xfe; //SOF
		zstackcmd[1] = 0x00; // length 0
		zstackcmd[2] = 0x27; // CMD0
		zstackcmd[3] = 0x01; // CMD1		
		zstackcmd[4] = (byte)calculateCheckum(zstackcmd, 1, 3);
		sendAPIFrame(zstackcmd, zstackcmd.length);
	}
	
	
	// Experiment for ZStack NIC. Send UTIL_ADDRMGR_NWK_ADDR_LOOKUP
	public void UTIL_ADDRMGR_NWK_ADDR_LOOKUP (Address16 addr16) throws IOException {
		byte[] addr16bytes = addr16.getBytesLSBF();
		byte[] zstackcmd = new byte[7];
		zstackcmd[0] = (byte)0xfe; //SOF
		zstackcmd[1] = 2; // length
		zstackcmd[2] = 0x27; // CMD0
		zstackcmd[3] = 0x41; // CMD1
		zstackcmd[4] = addr16bytes[0];
		zstackcmd[5] = addr16bytes[1];
		zstackcmd[6] = (byte)(zstackcmd[1] ^ zstackcmd[2] ^ zstackcmd[3] ^ zstackcmd[4] ^ zstackcmd[5]);
		sendAPIFrame(zstackcmd, zstackcmd.length);
	}
	/**
	 * This command is used to control the joining permissions and thus allows or disallows 
	 * new devices from joining the network. Ref Z-Stack Monitor and Test API SWRA198 Rev 1.4,
	 * section 3.7.1.3 page 39. ** THIS IS CURRENTLY NOT WORKING. UBEE RETURNING CMD 0x6000
	 * WHICH IS UNDOCUMENTED **
	 * 
	 * @param addr16 The destination parameter indicates the address of the device for which the
	 * joining permissions should be set. This can be 0xFFFC for all routers and the coordinator. 
	 * @param timeout The timeout in seconds. 0 means no devices are allowed to join. 0xFF means
	 * that devices can join at any time.
	 * @throws IOException
	 */
	public void ZB_PERMIT_JOINING_REQUEST (Address16 addr16, int timeout) throws IOException {
		byte[] command = {(byte)START_OF_FRAME,
				0x03, // length
				0x26, // CMD0
				0x08, // CMD1
				0x00, 0x00, // place holder for destination addr16
				0x00, // place holder for timeout
				0x00 // place holder for CRC
				};
		byte[] addr16bytes = addr16.getBytesLSBF();
		command[4] = addr16bytes[0];
		command[5] = addr16bytes[1];
		command[6] = (byte)timeout;
		
		command[7] = (byte)calculateCheckum(command,1 , 6);
	
		sendAPIFrame(command,command.length);
	}
	
	
	// Experiment for ZStack NIC. Send ZDO_NWK_ADDR_REQ
	public void ZDO_NWK_ADDR_REQ (Address64 addr64) throws IOException {
		log.debug ("ZDO_NWK_ADDR_REQ(" + addr64 + ")");
		byte[] addr64bytes = addr64.getBytesLSBF();
		byte[] zstackcmd = new byte[15]; // SOF + LEN + 2xCMD + payload + FCS
		zstackcmd[0] = (byte)0xfe; //SOF
		zstackcmd[1] = 0x0a; // length 10
		zstackcmd[2] = 0x25; // CMD0
		zstackcmd[3] = 0x00; // CMD1
		System.arraycopy(addr64bytes, 0, zstackcmd, 4, 8);
		
		zstackcmd[12] = 0; // ReqType
		zstackcmd[13] = 0; // StartIndex
		
		zstackcmd[14] = (byte)calculateCheckum(zstackcmd, 1, 13);
		sendAPIFrame(zstackcmd, zstackcmd.length);
	}
	/**
	 * Send a NIC API frame to the NIC UART. 
	 * 
	 * @param packet The content of the API packet starting with the API packet type and
	 * excluding the checksum.
	 * @param packetLen
	 * @throws IOException
	 */
	public void sendAPIFrame (byte[] packet, int packetLen) throws IOException {
		
		log.trace ("sendAPIPacket(): " + ByteFormatUtils.byteArrayToString(packet, 0, packetLen));
		
		uartAdapter.txAPIFrame(packet, packetLen);
		
		// Experimental
		if (ECHO_OUTGOING_PACKETS_TO_LISTENERS) {
			for (APIFrameListener l :  apiListeners) {
				l.handleAPIFrame(packet, packetLen);
			}
		}
	}
	
	/**
	 * {@inheritDoc}
	 */
	public void close () {
		uartAdapter.close();
	}
	
	/**
	 * Register a listener for ZigBee packets (XBee API packet type 0x91)
	 */
	public synchronized void addZigBeePacketListener(ZigBeePacketListener listener) {
		log.debug ("addZigBeePacketListener(): adding " + listener 
				+ ". #listeners: " + zigbeePacketListeners.size());
		zigbeePacketListeners.add(listener);
	}
	
	/**
	 * Register a listener for ZigBee packets (XBee API packet type 0x91) applying
	 * a filter.
	 */
	public synchronized void addZigBeePacketListener(ZigBeePacketListener listener,
			ZigBeePacketFilter filter) {
		log.debug ("addZigBeePacketListener(): adding " + listener 
				+ " with filter " + filter
				+ ". #listeners: " + zigbeePacketListeners.size()
				+ ". #filters: " + filters.size());
		zigbeePacketListeners.add(listener);
		filters.put (listener, filter);
	}

	public synchronized void removeZigBeePacketListener(ZigBeePacketListener listener) {
		log.debug ("removeZigBeePacketListener(): removing " + listener);
		zigbeePacketListeners.remove(listener);
		filters.remove(listener);
	}

	/**
	 * Register a listener for all XBee API packets.
	 * 
	 * @param listener
	 */
	public synchronized void addAPIPacketListener(APIFrameListener listener) {
		log.debug ("addAPIPacketListener(): adding " + listener);
		apiListeners.add(listener);
	}
	
	public synchronized void removeAPIPacketListener (APIFrameListener listener) {
		log.debug ("addAPIPacketListener(): removing " + listener);
		apiListeners.remove(listener);
	}
	
	/**
	 * {@inheritDoc}
	 * 
	 * ZStack specific notes: Called by {@link ZStackReadThread} [or remote NIC] when an API frame is
	 * received by the NIC and transmitted out on its UART. The frame
	 * starts with the API command and ends with the last byte of data.
	 * The Start-of-Frame, Frame length and Frame checksum are omitted.
	 */
	public synchronized void handleAPIFrame(byte[] frame, int frameLen) {
		
		lastRxTime = System.currentTimeMillis();
		
		log.info("handleAPIFrame(): rxTime=" + lastRxTime 
				+ ": " + ByteFormatUtils.byteArrayToString(frame, 0, frameLen));
		
		// Remove expired listeners
		removeExpiredListeners(zigbeePacketListeners);
		removeExpiredListeners(apiAckListeners);
		removeExpiredListeners(apiListeners);
		
		// If there are any apiListeners notify them now. Loop on a copy
		// because the handler may attempt to unregister itself (causing
		// ConcurrentModificationException if operating on the original.
		ArrayList<APIFrameListener> apiListenersCopy = new ArrayList<APIFrameListener>(apiListeners);
		for (APIFrameListener l :  apiListenersCopy) {
			l.handleAPIFrame(frame, frameLen);
		}
		
		// What type of XBee API packet type?
		int apiPacketType = frame[1] & 0xff;
		apiPacketType |= (frame[0]&0xff)<<8;
		
		log.debug ("apiPacketType=0x" + Integer.toHexString(apiPacketType));
		switch (apiPacketType) {
			
		// SYS_RESET_IND
		case 0x4180: {
			log.debug("CMD=0x4480 (SYS_RESET_IND)");
			break;
		}
		
		case 0x4480: {
			log.debug("CMD=0x4481 (AF_DATA_CONFIRM)"
					+ " status=0x" + Integer.toHexString(frame[2] & 0xff) 
					+ " ep=0x" + Integer.toHexString(frame[3]&0xff)
					+ " transId=0x" + Integer.toHexString(frame[4]&0xff));
			break;
		}
		// AF_INCOMING_MESSAGE
		case 0x4481: {
			log.debug("CMD=0x4481 (AF_INCOMING_MESSAGE)");
			ZigBeePacket zbpacket = new ZigBeePacket();
			
			// This API call does not give us addr64
			zbpacket.setSourceAddress64(Address64.UNKNOWN);
			
			// Bytes 0,1: Group ID 
			int groupId = XBeeUtil.get16LSBFirst(frame, 2);
			zbpacket.setClusterId(XBeeUtil.get16LSBFirst(frame, 4));
			final boolean lsbFirst = true; 
			Address16 addr16 = new Address16(frame,6,lsbFirst);
			zbpacket.setSourceAddress16(addr16);
			zbpacket.setSourceEndPoint(frame[8] & 0xff); 
			zbpacket.setDestinationEndPoint(frame[9] & 0xff);
			
			// byte 8: broadcast flag
			// byte 9: link quality
			int lqi = frame[11]&0xff;
			zbpacket.setLqi(lqi);
			
			int securityFlag = frame[12]&0xff;
			int timestamp = XBeeUtil.get32LSBFirst(frame, 13); // ???
			int seqId = frame[17]&0xff;
			int len = frame[18]&0xff;
			
			log.info("group=" + groupId
					+ " srcAddr16=" + addr16
					+ " clusterId=0x" + Integer.toHexString(zbpacket.getClusterId())
					+ " srcEp=" + zbpacket.getSourceEndPoint()
					+ " dstEp=" + zbpacket.getDestinationEndPoint()
					+ " lqi=" + lqi + " sec=" + securityFlag + " ts=" + timestamp + " seq=" + seqId + " len="+len
					);

		
			// TODO: We are not given profileId, so will have to guess for the moment.
			// If EP0 then must be ZDP profile, else can only guess HA as that's the 
			// only other possible profile right now. 
			zbpacket.setProfileId(zbpacket.getSourceEndPoint() == 0 
					? Profile.ZIGBEE_DEVICE_PROFILE : Profile.HOME_AUTOMATION);
			
			byte[] payload = new byte[len];
			System.arraycopy(frame, 19, payload, 0, len);
			zbpacket.setPayload(payload);
			notifyZigBeePacketListners(zbpacket);
			break;
		}
		// ZDO_NODE_DESC_RSP
		case 0x4580:
		case 0x4581:
		case 0x4582:
		case 0x4584:
		case 0x4585:
			
		case 0x4593: // Cluster 0x8013 (end device announce response eg 45 93 f1 ea f1 ea c6 00 10 c2 50 00 00 10 00)
		case 0x45a1: // Cluster 0x80a1 
		case 0x45b0:
		case 0x45b1:
		case 0x45b3:
		{
			log.debug("CMD=0x44xx (ZDO_xxxx_RSP)");
			ZigBeePacket zbpacket = new ZigBeePacket();
			zbpacket.setSourceAddress64(Address64.UNKNOWN);
			zbpacket.setSourceAddress16(new Address16(frame,2,true /*lsbFirst*/));
			
			// API CMD0 = 0x45 CMD1 = clusterId-0x8000+0x80
			// So response cluster 0x8033 becomes CMD0=0x45 CMD1=0xb3
			zbpacket.setClusterId(0x8000 | apiPacketType&0x7f);
			
			// ZDO is always profileId 0x0000 and end point 0x00
			zbpacket.setProfileId(Profile.ZIGBEE_DEVICE_PROFILE);
			zbpacket.setSourceEndPoint(0x0);

			// Now it gets messy
			byte[] payload;
			if (apiPacketType==0x4580 || apiPacketType==0x4581) {
				// ZDO payload is API packet excluding 2 bytes CMD
				payload = new byte[frameLen-2+1];
				System.arraycopy(frame, 2, payload, 1, frameLen-2);
				
				// This is very unfortunate. Notice that in case of
				// error, address fields are jibberish. And we don't
				// a transaction sequence number in the response. However
				// I notice that payload[7] *seems* to hold the TSN.
				// So copy it into position 0 where it belongs.
				if (payload[1] != 0x00) {
					payload[0] = payload[7]; // TODO: getting array out of bounds exception here on occasion
				}
			} else {
				// ZDO payload is API packet excluding 2 bytes CMD and 2 bytes ADDR16
				payload = new byte[frameLen-4+1];
				System.arraycopy(frame, 4, payload, 1, frameLen-4);
			}
			
			//payload[0] = 0x00; // fake ZCL header
			
			log.debug ("payload=" + ByteFormatUtils.byteArrayToString(payload));
			zbpacket.setPayload(payload);
			notifyZigBeePacketListners(zbpacket);
			break;
		}
		case 0x6101:
			int capability = ((frame[3]&0xff)<<8) | (frame[2]&0xff);
			log.debug("CMD=0x6101 (UTIL_SYS_PING response), capabilities=0x"
					+Integer.toHexString(capability)
					+ " ("
					+ ( (capability & 0x0001) != 0 ? "SYS " : "") 
					+ ( (capability & 0x0002) != 0 ? "MAC " : "") 
					+ ( (capability & 0x0004) != 0 ? "NWK " : "") 
					+ ( (capability & 0x0008) != 0 ? "AF " : "") 
					+ ( (capability & 0x0010) != 0 ? "ZDO " : "") 
					+ ( (capability & 0x0020) != 0 ? "SAPI " : "") 
					+ ( (capability & 0x0040) != 0 ? "UTIL " : "") 
					+ ( (capability & 0x0080) != 0 ? "DEBUG " : "") 
					+ ( (capability & 0x0100) != 0 ? "APP " : "") 
					+ ( (capability & 0x1000) != 0 ? "ZOAD " : "") 
					+ ")"
					);
			break;
		case 0x6500:
			log.debug("CMD=0x6101 (ZDO_NWK_ADDR_REQ response)");
			break;
		case 0x653E:
			log.debug("CMD=0x613E (ZDO_MSG_CB_REGISTER response) status=0x" + Integer.toHexString(frame[2]&0xff));
			break;
		case 0x6401:
			log.debug("CMD=0x6401 (AF_DATA_REQUEST response), status=0x"+Integer.toHexString(frame[2]&0xff));
			break;
		case 0x6608:
			log.debug("CMD=0x6608 (ZB_PERMIT_JOINING_REQUEST response), status=0x"+Integer.toHexString(frame[2]&0xff));
			break;
		case 0x6700:
			log.debug("CMD=0x6700 (UTIL_GET_DEVICE_INFO_RSP), status=0x"+Integer.toHexString(frame[2]&0xff));
			log.trace("    addr64=" + new Address64(frame,3, true).toString());
			log.trace("    addr16=" + new Address16(frame,11, true).toString());
			log.trace("    devType=" + (frame[13]==1 ? "Coordinator" : (frame[13]==2 ? "Router" : "EndDevice")));
			log.trace("    deviceState=" + frame[14]);
			log.trace("    numDevices=" + frame[15]);
			for (int i = 0; i < frame[15]; i++) {
				log.trace ("      addr16=" + new Address16(frame,16+i*2, true).toString());
			}
			break;
			
		case 0x6741:
			// Experimental:
			// Still cannot match response to request. Only thing going is that
			// I think we can expect a response to this request very quickly. So
			// can setup a queue and only allow one query in play at any given 
			// point in time.
			// Response to UTIL_ADDRMGR_NWK_ADDR_LOOKUP
			// Fake ZDO response packet
			ZigBeePacket zbpacket = new ZigBeePacket();
			zbpacket.setProfileId(Profile.ZIGBEE_DEVICE_PROFILE);
			zbpacket.setSourceEndPoint(0x00);
			zbpacket.setSourceAddress64(Address64.COORDINATOR);
			zbpacket.setSourceAddress16(Address16.COORDINATOR);
			zbpacket.setClusterId(0x8001);
			byte[] payload = new byte[11];
			payload[0] = 0x00;
			System.arraycopy(frame, 0, payload, 1, 8);
			notifyZigBeePacketListners(zbpacket);
			break;
			
		default:
			log.warn ("unrecognized API packet type " + Integer.toHexString(apiPacketType));
		} // end switch
			
	}
	

	private void notifyZigBeePacketListners(ZigBeePacket zbpacket) {
		// Do callbacks
		log.debug("Sending ZigBee packet to " + zigbeePacketListeners.size()
				+ " listeners:");

		// Make a copy as these callbacks may want to unregister themselves
		// which
		// would cause a Concurrent modification exception
		ArrayList<ZigBeePacketListener> listeners = new ArrayList<ZigBeePacketListener>(
				zigbeePacketListeners);
		for (ZigBeePacketListener l : listeners) {

			ZigBeePacketFilter filter = filters.get(l);
			log.trace("  " + l.toString() + " filter=" + filter);

			// If there is a filter for this listener, test the filter
			// before notifying listener. If no filter then always notify.
			if (filter != null) {
				if (filter.allow(zbpacket)) {
					l.handleZigBeePacket(zbpacket);
					log.trace("    filter allows");
				} else {
					log.trace("    filter rejects");
				}
			} else {
				log.trace("    allowed by default");
				l.handleZigBeePacket(zbpacket);
			}

		}
	}
	/**
	 * Register a listener for ZigBee ACK packets (XBee API packet type 0x8B)
	 */
	public synchronized void addAcknowledgementListener(AcknowledgementListener listener) {
		apiAckListeners.add(listener);
	}

	/**
	 * Register a listener for ZigBee ACK packets (XBee API packet type 0x8B) but only
	 * invoke the listener if frameId matches.
	 */
	public synchronized void addAcknowledgementListener(AcknowledgementListener listener,
			int frameId) {
		ackFrameIdFilter.put (listener, frameId);
		apiAckListeners.add(listener);		
	}

	public synchronized void removeAcknowledgementListener(AcknowledgementListener listener) {
		apiAckListeners.remove(listener);
	}

	
	
	/**
	 * Iterate through a {@link List} of listener objects implementing the
	 * {@link Listener} interface, removing those that have expired.
	 * @param listeners
	 */
	private void removeExpiredListeners (ArrayList<? extends Listener> listeners) {
		ArrayList<Listener> expiredListeners = new ArrayList<Listener>();
		Date now = new Date();
		for (Listener l : listeners) {
			if ( (l.getExpiryTime() != null) && now.after(l.getExpiryTime()) ) {
				expiredListeners.add(l);
			}
		}
		for (Listener l : expiredListeners) {
			log.debug("removeExpiredListeners(): removing expired listener " + l);
			listeners.remove(l);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public Date getExpiryTime() {
		// XBeeDriver never stops listening -- so always return null
		return null;
	}

	/**
	 * {@inheritDoc}
	 */
	public void setExpiryTime(Date expire) {
		// ignore
	}

	/**
	 * {@inheritDoc}
	 */
	public UARTAdapter getUARTAdapter() {
		return uartAdapter;
	}


	/**
	 * {@inheritDoc}
	 */
	public void ping() throws IOException {
		
		byte[] ping = {
				(byte)START_OF_FRAME,
				0x00,  // length (no payload)
				0x21, 0x01, // (SYS_PING command)
				0x20 // known checksum
		};
		sendAPIFrame(ping, ping.length);
	}
	
	/**
	 * {@inheritDoc}
	 */
	public void reset() throws IOException {
		
		byte[] reset = {
				(byte)START_OF_FRAME,
				0x01,  // length
				0x41, 0x00, // (SYS_RESET_REQ command)
				0x00, // Hardware reset (!=0 is soft reset)
				0x40 // known checksum
		};
		sendAPIFrame(reset, reset.length);
	}
	
	/**
	 * {@inheritDoc}
	 */
	public void test () throws IOException {
		
	}
	/**
	 * {@inheritDoc}
	 */
	public long getLastRxTime () {
		return lastRxTime;
	}

	/**
	 * Used to calculate checksum of API frame according to the TI Monitor and 
	 * Test API 1.4 specification.
	 * 
	 * @param bytes  Data on which to calculate checksum
	 * @param start  Offset into 'bytes' array. For a API frame generally that is 1 (skip SOF)
	 * @param length Number of bytes to calculate checksum over.
	 * @return Return 8 bit checksum (values 0..255).
	 */
	public static final int calculateCheckum (byte[] bytes, int start, int length) {
		int cs=0;
		int end = start+length;
		for (int i = start; i < end; i++) {
			cs ^= bytes[i];
		}
		return cs & 0xff;
	}
	
	@Override
	public void addListener(Listener listener) {
		if (listener instanceof ZigBeePacketListener) {
			zigbeePacketListeners.add((ZigBeePacketListener)listener);
			return;
		}
		if (listener instanceof APIFrameListener) {
			apiListeners.add((APIFrameListener)listener);
			return;
		}
		if (listener instanceof NICErrorListener) {
			errorListeners.add((NICErrorListener)listener);
			return;
		}
		
	}

	@Override
	public void removeListener(Listener listener) {
		if (listener instanceof ZigBeePacketListener) {
			zigbeePacketListeners.remove((ZigBeePacketListener)listener);
			return;
		}
		if (listener instanceof APIFrameListener) {
			apiListeners.remove((APIFrameListener)listener);
			return;
		}
		if (listener instanceof NICErrorListener) {
			errorListeners.remove((NICErrorListener)listener);
		}
	}
	
}
