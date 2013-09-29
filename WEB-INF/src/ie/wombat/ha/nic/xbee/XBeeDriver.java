package ie.wombat.ha.nic.xbee;

import ie.wombat.ha.AcknowledgementListener;
import ie.wombat.ha.ByteFormatUtils;
import ie.wombat.ha.DebugUtils;
import ie.wombat.ha.Listener;
import ie.wombat.ha.ZigBeePacket;
import ie.wombat.ha.ZigBeePacketFilter;
import ie.wombat.ha.ZigBeePacketListener;
import ie.wombat.ha.ZigBeeNIC;
import ie.wombat.ha.nic.APIFrameListener;
import ie.wombat.ha.nic.NICErrorListener;
import ie.wombat.ha.nic.UARTAdapter;
import ie.wombat.zigbee.Ack;
import ie.wombat.zigbee.AddressNotFoundException;
import ie.wombat.zigbee.ZigBeeCommand;
import ie.wombat.zigbee.address.Address16;
import ie.wombat.zigbee.address.Address64;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import org.apache.log4j.Logger;

/**
 * A XBee Series 2 module implementation of {@link ZigBeeNIC}. XBee must be in binary
 * API mode (AP=2). Other settings, AO=1, ZS=2, NJ=FF (always allow joins)
 * 
 * @author joe
 *
 */
public class XBeeDriver implements ZigBeeNIC, APIFrameListener, XBeeConstants {

	private static Logger log = Logger.getLogger(XBeeDriver.class);
	
	/** 
	 * Experimental feature to echo outgoing (server to XBee) packets
	 * to {@link APIFrameListener} objects. This is to facilitate
	 * remote debug. 
	 */
	private static final boolean ECHO_OUTGOING_PACKETS_TO_LISTENERS = true;
	
	/** Objects that have registered to receive notification of arrival of ZigBee packets */
	private ArrayList<ZigBeePacketListener> zigbeePacketListeners = new ArrayList<ZigBeePacketListener>();
	
	/** Objects that have registered to receive notification of arrival of XBee ACKs 
	 * (API packet type 0x8B) 
	 */
	private ArrayList<AcknowledgementListener> xbeeAckListeners = new ArrayList<AcknowledgementListener>();
	
	/**
	 * Objects that have registered to receive notification of NIC errors.
	 */
	private ArrayList<NICErrorListener> errorListeners = new ArrayList<NICErrorListener>();
	
	/**
	 * Filter which are applied to zigbee packets before notifying {@link ZigBeePacketListener}
	 */
	private HashMap<ZigBeePacketListener,ZigBeePacketFilter> filters = new HashMap<ZigBeePacketListener,ZigBeePacketFilter>();
	
	/** 
	 * Used in {@link #addAcknowledgementListener(AcknowledgementListener, int)} to store
	 * the frame ID for which to filter for.
	 */
	private HashMap<AcknowledgementListener, Integer> ackFrameIdFilter = new HashMap<AcknowledgementListener, Integer>();

	/**
	 * A thread listens to the xbeeIn input stream, triggering callbacks when an API packet arrives.
	 */
	//private XBeeReadThread readThread;
	
	/**
	 * Clients can subscribe to get all XBee API packets from the UART. Used in
	 * AT command etc.
	 */
	private ArrayList<APIFrameListener> apiListeners = new ArrayList<APIFrameListener>();
	
	/**
	 * A counter used to assign unique IDs to XBee API frames. 
	 */
	private int xBeeFrameId = 1;

	/** 
	 * Object which sends and receives API packets from the XBee UART. 
	 * */
	private UARTAdapter uartAdapter;
	
	private long lastRxTime = 0;
	
	/**
	 * Create XBeeDriver. This is to be called from a factory object. 
	 * @param uartAdapter Object which sends and receives API packets from the XBee.
	 */
	public XBeeDriver () {
	
	}
	
	public void setUARTAdapter (UARTAdapter uartAdapter) {
		this.uartAdapter = uartAdapter;
		uartAdapter.setRxAPIFrameListener(this);
		
		byte[] param = new byte[1];
		
		// Use local AT commands to ensure that NIC is properly configured
		try {
			param[0] = 2;
			sendLocalATCommand(getNextXBeeFrameId(), "ZS", param);
			param[0] = 1;
			sendLocalATCommand(getNextXBeeFrameId(), "AO", param);
		} catch (IOException e) {
			log.error(e.toString());
		}
	}
	/**
	 * Send an AT command to the local XBee (NIC).
	 * 
	 * @param command The two letter AT command (omit the AT prefix).
	 * @param params
	 * @return
	 * @throws IOException
	 */
	public int sendLocalATCommand (int frameId, String command, byte[] params) throws IOException {
		if (params == null) {
			params = new byte[0];
		}
		byte[] packet = new byte[4 + params.length];
		packet[0] = 0x08; // AT Command
		packet[1] = (byte)frameId; // frame ID
		
		packet[2] = (byte)command.charAt(0);
		packet[3] = (byte)command.charAt(1);
		
		for (int i = 0; i < params.length; i++) {
			packet[i+4] = params[i];
		}
		
		sendAPIFrame(packet,packet.length);
		
		return frameId;
	}
	public int sendLocalATCommand (int frameId, String command) throws IOException {
		return sendLocalATCommand (frameId, command, null);
	}
	/**
	 * Send AT command to a remote XBee device via the XBee API. This can also be accomplished by sending a regular
	 * ZigBee command on the control end point (0xE6) with Digi profile etc.
	 * @param addr64
	 * @param addr16
	 * @param command The two letter AT command (omit the AT prefix).
	 * @param params
	 * @return
	 * @throws IOException
	 */
	public int sendRemoteATCommand (Address64 addr64, Address16 addr16, String command, byte[] params) throws IOException {
		int frameId = getNextXBeeFrameId();
		
		if (params == null) {
			params = new byte[0];
		}
		
		// Packet = type (1) + frameId (1) + addr64(8) + addr16(2) + opts(1) + atcmd(2) + params(var)
		byte[] packet = new byte[15 + params.length];
		packet[0] = 0x17; // Remote AT Command
		packet[1] = (byte)frameId; // frame ID
		
		System.arraycopy(addr64.getBytesMSBF(), 0,packet, 2, 8);
		System.arraycopy(addr16.getBytesMSBF(), 0,packet, 10, 2);
		
		packet[12] = 0x00; // opts
			
		packet[13] = (byte)command.charAt(0);
		packet[14] = (byte)command.charAt(1);
		
		for (int i = 0; i < params.length; i++) {
			packet[15+i] = params[i];
		}
		
		sendAPIFrame(packet,packet.length);
		
		return frameId;
	}
	/**
	 * {@see #sendRemoteATCommand(Address64, Address16, String, byte[])
	 */
	public int sendRemoteATCommand (Address64 addr64, Address16 addr16, String command) throws IOException {
		return sendRemoteATCommand (addr64, addr16, command, null); 
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

		byte[] packetHeader = {
				// 0x7E,
				// 0x00,0x00, // Place holder for packet length
				0x11, // XBee API Explicit Transmit Request
				0x00, // Place holder for Frame ID
				0x00, 0x13, 0x7A, 0x00, 0x00, 0x00, 0x00, 0x00, // destaddr64 holder
				// 0x00,0x00,0x00,0x00,0x00,0x00,(byte)0xFF,(byte)0xFF, // bcast
				(byte) 0xFF, (byte) 0xFE, // Dest addr16
				0x0A, // Source end point
				0x0A, // Destination end point
				0x00, 0x06, // Cluster ID 0x0006
				0x01, 0x04, // Profile ID = HA 0x0104
				0x08, // Broadcast radius
				0x00 // TxOpts
		};
		// Tx Options are:
		// -x-- ---- : (0x40) extended timeout
		// --x- ---- : (0x20) APS encryption
		// ---- ---x : (0x01) disable ACK
		
		//packetHeader[1] = (byte) frameId;
		int xBeeFrameId = getNextXBeeFrameId();
		packetHeader[1] = (byte)xBeeFrameId;
		
		int i;
		
		// If address64 not specified use XBee UNKNOW address (all 0xFF)
		if (address64 == null) {
			address64 = Address64.UNKNOWN;
		}
		for (i = 0; i < 8; i++) {
			packetHeader[2 + i] = address64.addr64[i];
		}
		
		// TODO: It seems XBee API broadcast address is 0xFFFD, whereas it's actually FFFF on the wire. 
		// Need to look for FFFF and substitute for FFFD.
		
		// If address16 not specified use XBee UNKNOW address (all 0xFFFE)
		if (address16 == null) {
			address16 = Address16.UNKNOWN;
		}
		packetHeader[10] = address16.addr[0];
		packetHeader[11] = address16.addr[1];

		packetHeader[12] = (byte) srcEp;
		packetHeader[13] = (byte) dstEp;

		packetHeader[14] = (byte) (clusterId >> 8);
		packetHeader[15] = (byte) (clusterId & 0xFF);

		packetHeader[16] = (byte) (profileId >> 8);
		packetHeader[17] = (byte) (profileId & 0xFF);

		int packetLen = 20 + command.length;

		byte[] packet = new byte[packetLen];
	
		for (i = 0; i < packetHeader.length; i++) {
			packet[i] = packetHeader[i];
		}
		for (i = 0; i < command.length; i++) {
			packet[i + packetHeader.length] = command[i];
		}
		log.debug ("TX frameId="+xBeeFrameId + ": "
				+ DebugUtils.formatXBeeAPIFrame(packet, 0, packetLen)
				);
		sendAPIFrame(packet, packetLen);
		
		return xBeeFrameId;

	}

	/**
	 * Send a XBee API packet to the XBee UART. This method will take care of 
	 * adding Start-of-Packet delimiter, packet length, checksum and escaping 
	 * the data (bytes 0x7E, 0x7D, 0x11, 0x13 need to be escaped). The fully
	 * formatted/escaped API packet is then handed to the UART Adapter for
	 * transmission to the XBee UART.
	 * 
	 * @param packet The content of the API packet payload starting with the API packet type and
	 * excluding the checksum.
	 * @param packetLen
	 * @throws IOException
	 */
	public void sendAPIFrame (byte[] packet, int packetLen) throws IOException {
		
		byte[] apipacket = XBeeUtil.encodeAPIFrame(packet, packetLen);
		uartAdapter.txAPIFrame(apipacket, apipacket.length);
		
		// Experimental
		if (ECHO_OUTGOING_PACKETS_TO_LISTENERS) {
			for (APIFrameListener l :  apiListeners) {
				l.handleAPIFrame(packet, packetLen);
			}
		}
	}
	
	
	public void close () {
		uartAdapter.close();
	}
	
	/* package scope */ synchronized int getNextXBeeFrameId() {
		xBeeFrameId++;
		if (xBeeFrameId > 255) {
			xBeeFrameId = 1;
		}
		return xBeeFrameId;
	}

	/**
	 * Register a listener for ZigBee packets (XBee API packet type 0x91)
	 */
	public synchronized void addZigBeePacketListener(ZigBeePacketListener listener) {
		log.debug ("addZigBeePacketListener(): adding " + listener);
		zigbeePacketListeners.add(listener);
	}
	
	/**
	 * Register a listener for ZigBee packets (XBee API packet type 0x91) applying
	 * a filter.
	 */
	public synchronized void addZigBeePacketListener(ZigBeePacketListener listener,
			ZigBeePacketFilter filter) {
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
		log.debug ("addXBeeAPIPacketListener(): adding " + listener);
		apiListeners.add(listener);
	}
	
	public synchronized void removeAPIPacketListener (APIFrameListener listener) {
		log.debug ("addXBeeAPIPacketListener(): removing " + listener);
		apiListeners.remove(listener);
	}
	
	/**
	 * {@inheritDoc}
	 * 
	 * XBee specific notes: Receive XBee API packet from {@link XBeeReadThread} [or remote NIC]. The packet
	 * starts with the API packet type and does not include the checksum. The data is unescaped.
	 */
	public synchronized void handleAPIFrame(byte[] packet, int packetLen) {
		
		log.info("handleAPIFrame(): " + ByteFormatUtils.byteArrayToString(packet, 0, packetLen));
		
		lastRxTime = System.currentTimeMillis();
		
		// Remove expired listeners
		removeExpiredListeners(zigbeePacketListeners);
		removeExpiredListeners(xbeeAckListeners);
		removeExpiredListeners(apiListeners);
		
		// If there are any apiListeners notify them now. Loop on a copy
		// because the handler may attempt to unregister itself (causing
		// ConcurrentModificationException if operating on the original.
		ArrayList<APIFrameListener> apiListenersCopy = new ArrayList<APIFrameListener>(apiListeners);
		for (APIFrameListener l :  apiListenersCopy) {
			l.handleAPIFrame(packet, packetLen);
		}
		
		// What type of XBee API packet type?
		int apiPacketType = packet[0] & 0xff;
		
		switch (apiPacketType) {
			
		case 0x11:
			// Server->XBee packet echoed back. Ignore.
			// This method is normally called when a API packet is transmitted
			// by the XBee UART. However it may also be called by a bridge. As
			// 0x11 packets are always generated by the application and never 
			// by the XBee will drop it now to prevent endless loops.
			log.debug ("Ignoring type 0x11 packet.");
			break;
			
			
		case 0x88: {
			// Frame type (0x88), Frame ID, ATCMD0, ATCMD1, CMD_STATUS (0=OK,1=ERROR,2=InvCmd,3=InvParam), CMDDATA
			log.info ("AT Command Response");
			log.info ("FrameID=" + packet[1]);
			log.info ("CMD=" + (char)packet[2] + (char)packet[3]);
			log.info ("CMDStatus=" + packet[4]);
		}
			
		case 0x8B:
			Ack ack = new Ack();
			// packet[0] == 0x8B
			int frameId = packet[1] & 0xff;
			// packet[2,3] = addr16 (MSB)
			ack.addr16 = new Address16(packet, 2, false /*msbfirst*/);
			ack.retryCount = packet[4] & 0xff;
			ack.deliveryStatus = packet[5] & 0xff; // See XBeeUtil
			ack.discoveryStatus = packet[6] & 0xff;
			// Do call backs. An optional Frame ID filter can be set with
			// a listener. Check if there is an associated filter and if so
			// only invoke the handler if it matches. 
			// As the callback may attempt to unregister itself need to
			// operate on a copy of ackListeners.
			ArrayList <AcknowledgementListener> ackListenersCopy = new ArrayList<AcknowledgementListener>(xbeeAckListeners);
			
			for (AcknowledgementListener l : ackListenersCopy) {
				log.debug ("Looking at listener " + l);
				if (ackFrameIdFilter.containsKey(l)) {
					int filter = ackFrameIdFilter.get(l);
					log.debug ("FrameID filter is in effect. frameId=" + filter);
					if (filter == frameId) {
						log.debug ("Filter matches. Calling handleAck().");
						l.handleAcknowledgement(frameId,ack);
					} else {
						log.debug ("Filter does not match. " + filter + "!=" + frameId);
					}
				} else {
					log.debug ("No frameId filter in effect. Call handleAck().");
					l.handleAcknowledgement(frameId,ack);
				}
			}
		break;
		
		case 0x91: {
			ZigBeePacket zbpacket = new ZigBeePacket();
			
			// XBee API uses MSB first for addresses
			boolean lsbFirst = false; 
			Address64 addr64 = new Address64(packet,1,lsbFirst);
			Address16 addr16 = new Address16(packet,9,lsbFirst);
			zbpacket.setSourceAddress64(addr64);
			zbpacket.setSourceAddress16(addr16);

			zbpacket.setSourceEndPoint(packet[11] & 0xff); 
			zbpacket.setDestinationEndPoint(packet[12] & 0xff);
			
			zbpacket.setClusterId(XBeeUtil.get16MSBFirst(packet, 13));
			zbpacket.setProfileId(XBeeUtil.get16MSBFirst(packet, 15));
		
			int opts = packet[17];
			
			if ( (opts & 0x40) != 0) {
				zbpacket.setFromEndDevice(true);
			}
			if ( (opts & 0x20) != 0) {
				zbpacket.setEncrypted(true);
			}
			if ( (opts & 0x02) != 0) {
				zbpacket.setBroadcast(true);
			}
			if ( (opts & 0x01) != 0) {
				zbpacket.setAcknowledgement(true);
				
			}
			
			byte[] payload = new byte[packetLen-18];
			System.arraycopy(packet, 18, payload, 0, payload.length);
			zbpacket.setPayload(payload);
			
			// Do callbacks
			
			// Make a copy as these callbacks may want to unregister themselves which
			// would cause a Concurrent modification exception
			ArrayList<ZigBeePacketListener> listeners = new ArrayList<ZigBeePacketListener>(zigbeePacketListeners);
			for (ZigBeePacketListener l : listeners) {
				
				ZigBeePacketFilter filter = filters.get(l);
				
				// If there is a filter for this listener, test the filter
				// before notifying listener. If no filter then always notify.
				if (filter != null) {
					log.debug ("applying filter to listener");
					if (filter.allow(zbpacket)) {
						l.handleZigBeePacket(zbpacket);
						log.debug ("  allowed");
					} else {
						log.debug ("  rejected");
					}
				} else {
					l.handleZigBeePacket(zbpacket);
				}
				
			}
		}
		break;
		
		case 0x97: {
			// Response to AT query. Even queries made through API call 0x11 will be 
			// have the response translated back to this API method. Therefore to
			// interoperate with software that queries XBee modules by sending a
			// ZigBee packet (on end point 0xE6, Cluster 0x21, Profile 0xC105) we
			// must translate this XBee proprietary frame back to what the ZigBee
			// response packet would have looked like had it been passed through.
			
			log.warn ("Received API Frame 0x97 (AT response). Faking regular ZigBee response.");

			ZigBeePacket zbpacket = new ZigBeePacket();
			
			// XBee API uses MSB first for addresses
			boolean lsbFirst = false; 
			Address64 addr64 = new Address64(packet,2,lsbFirst);
			Address16 addr16 = new Address16(packet,10,lsbFirst);
			zbpacket.setSourceAddress64(addr64);
			zbpacket.setSourceAddress16(addr16);
			zbpacket.setSourceEndPoint(0xE6); 
			zbpacket.setDestinationEndPoint(0xE6);
			zbpacket.setClusterId(0x00A1);
			zbpacket.setProfileId(0xC105);
			
			// Payload comprises: apiFrameId, atcmd0, atcmd1, status, response data...
			
			byte[] payload = new byte[packetLen-12+1];
			payload[0] = packet[1]; // apiFrameId
			System.arraycopy(packet, 12, payload, 1, payload.length-1);
			log.info("AT Query Response: " + ByteFormatUtils.byteArrayToString(payload));
			zbpacket.setPayload(payload);
			
			// Do callbacks (TODO: duplicated code)
			
			// Make a copy as these callbacks may want to unregister themselves which
			// would cause a Concurrent modification exception
			ArrayList<ZigBeePacketListener> listeners = new ArrayList<ZigBeePacketListener>(zigbeePacketListeners);
			for (ZigBeePacketListener l : listeners) {
				
				ZigBeePacketFilter filter = filters.get(l);
				
				// If there is a filter for this listener, test the filter
				// before notifying listener. If no filter then always notify.
				if (filter != null) {
					log.debug ("applying filter to listener");
					if (filter.allow(zbpacket)) {
						l.handleZigBeePacket(zbpacket);
						log.debug ("  allowed");
					} else {
						log.debug ("  rejected");
					}
				} else {
					l.handleZigBeePacket(zbpacket);
				}
				
			}
			break;
			
		}
		default:
			log.warn ("unrecognized API frame type " + Integer.toHexString(apiPacketType));
		} // end switch
			
	}
	

	/**
	 * Register a listener for ZigBee ACK packets (XBee API packet type 0x8B)
	 */
	public synchronized void addAcknowledgementListener(AcknowledgementListener listener) {
		xbeeAckListeners.add(listener);
	}

	/**
	 * Register a listener for ZigBee ACK packets (XBee API packet type 0x8B) but only
	 * invoke the listener if frameId matches.
	 */
	public synchronized void addAcknowledgementListener(AcknowledgementListener listener,
			int frameId) {
		ackFrameIdFilter.put (listener, frameId);
		xbeeAckListeners.add(listener);		
	}

	public synchronized void removeAcknowledgementListener(AcknowledgementListener listener) {
		xbeeAckListeners.remove(listener);
	}

	/**
	 * Synchronously execute a XBee AT command and return the response.
	 * 
	 * @param command
	 * @return
	 * @throws IOException
	 */
	public byte[] execATCommand (String command) throws IOException {
		
		log.info("execATCommand(): command=" + command);
		
		if (command.length() < 2) {
			log.error ("AT command must be 2 characters, optionally followed by a parameter");
			// TODO: IOE not appropriate
			throw new IOException ("AT command must be 2 characters, optionally followed by a parameter");
		}
		

		final ATCommand at = new ATCommand (this);
		
		// Separate command from parameters
		String atcmd = command.substring(0,2);
		String paramHex = command.substring(2);
		
		// TODO: Will assume just one byte for the moment
		
		byte[] param;
		if (paramHex.length()>0) {
			param = new byte[1];
			param[0] = (byte)Integer.parseInt(paramHex, 16);
		} else {
			param = new byte[0];
		}
		
		// TODO: must be a better way
		final int[] response = new int[2];
		response[0] = -1;
		
		final byte[] atCommandResponse = new byte[256];
		
		at.setCommand(atcmd);
		at.setParameters(param);
		
		at.setCallback(new ATCommandResponse() {
			
			public void handleResponse(int status, byte[] packet, int packetLen) {
				// Wake the thread blocked with wait() 
				synchronized (at) {
					response[0] = status;
					System.arraycopy(packet, 5, atCommandResponse, 0, packetLen-5);
					response[1] = packetLen - 5;
					at.notifyAll();
				}
				
			}
		});
		
		at.exec();
		
		// Sleep (wait) until callback has been invoked (at.notifyAll())
		synchronized (at) {
			try {
				log.debug("execATCommand(): SLEEP");
				at.wait(30000);
				log.debug("execATCommand(): WAKE");
			} catch (InterruptedException e) {
				// ignore
			}
		}
		
		int atStatus = response[0];
		int responseLen = response[1];
		
		if (atStatus == -1) {
			// This means we got no response
			// TODO: is IOException the right exception?
			throw new IOException ("No response to AT command");
		}
		
		switch (atStatus) {
		case ATCommandResponse.ERROR:
			throw new IOException ("AT command error");
		case ATCommandResponse.INVALID_COMMAND:
			throw new IOException ("Invalid AT command");
		case ATCommandResponse.INVALID_PARAM:
			throw new IOException ("Invalid AT command parameter");
		}
		
		// TODO: messy
		byte[] ret = new byte[responseLen];
		System.arraycopy(atCommandResponse, 0, ret, 0, responseLen);
		return ret;
		
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

	public Date getExpiryTime() {
		// XBeeDriver never stops listening -- so always return null
		return null;
	}

	public void setExpiryTime(Date expire) {
		// ignore
	}

	public UARTAdapter getUARTAdapter() {
		return uartAdapter;
	}

	/**
	 * {@inheritDoc}
	 */
	public void ping() throws IOException {
		// Token AT query that is guaranteed to return immediately. If execAtCommand()
		// fails or timesout it will throw an IOException with is forwarded.
		this.execATCommand("MY");
	}

	public void reset() throws IOException {
		// TODO Auto-generated method stub
		
	}
	
	/**
	 * {@inheritDoc}
	 */
	public void test () throws IOException {
		// AO = 1
		byte[] response = execATCommand("AO");
		if (response.length!=1) {
			throw new IOException ("AO: expecting response length 1, got length " + response.length);
		}
		if (response[0] != 0x01) {
			throw new IOException ("AO: require AO=1 but AO=" + response[0]);
		}
		
	}
	
	public long getLastRxTime () {
		return this.lastRxTime;
	}

	
	/**
	 * Will be called by {@link XBeeReadThread} if an error condition is detected.
	 */
	void receiveNICError () {
		// Experimental: signal to listeners that NIC read thread is now dead
		// by sending a 0 byte packet.
		log.warn("Sending NIC error notification to " + errorListeners.size() + " listeners.");
		for (NICErrorListener l : errorListeners) {
			log.warn ("  sending notification to " + l);
			l.handleNICError(this, 500);
		}
	}
	
	@Override
	public void addListener(Listener listener) {
		
		if (listener instanceof ZigBeePacketListener) {
			log.info("Adding " + listener + " to zigbeePacketListeners");
			zigbeePacketListeners.add((ZigBeePacketListener)listener);
		}
		
		if (listener instanceof APIFrameListener) {
			log.info("Adding " + listener + " to apiListeners");
			apiListeners.add((APIFrameListener)listener);
		}
		
		if (listener instanceof NICErrorListener) {
			log.info("Adding " + listener + " to errorListeners");
			errorListeners.add((NICErrorListener)listener);
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
