package ie.wombat.zigbee;

import ie.wombat.ha.AcknowledgementListener;
import ie.wombat.ha.ByteFormatUtils;
import ie.wombat.ha.ZCLSequence;
import ie.wombat.ha.ZigBeeNIC;
import ie.wombat.ha.ZigBeePacket;
import ie.wombat.ha.ZigBeePacketFilter;
import ie.wombat.ha.ZigBeePacketListener;
import ie.wombat.zigbee.address.Address16;
import ie.wombat.zigbee.address.Address64;
import ie.wombat.zigbee.zcl.Profile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Date;

import org.apache.log4j.Logger;

/**
 * ZDP/ZDO query. Similar to ZigbeeCommand, but tailored for ZDP/ZDO requests/responses.
 * 
 * @author Joe Desbonnet, jdesbonnet@gmail.com
 *
 */
public class ZDPRequest implements ZigBeePacketListener, AcknowledgementListener {

	private static Logger log = Logger.getLogger(ZDPRequest.class);

	public static final int SUCCESS = 0x00;
	public static final int ADDRESS_NOT_FOUND = 0x01;
	
	/**
	 * NIC will automatically unregister this object as listener after this number of ms.
	 */
	private static final long LISTENER_EXPIRY_TIME = 60000L;
	
	private ZigBeeNIC nic;
	
	private Address16 address16;
	private Address64 address64;
	private int clusterId;
	private int sourceEndpoint;
	private int destinationEndpoint;
	
	private byte[] command;
	
	private ZDPResponseListener callback;
	private ZigBeePacketFilter callbackFilter = null;
	
	/** The ZCL sequence ID associated with this command */
	private int sequenceId;
	
	/** The ACK received in response to the command */
	private Ack commandAck = new Ack();
	
	/** On receiving a suitable response the default behaviour is to end the command (and resources will be 
	 * available for garbage collection). However some commands may require multiple responses so this 
	 * behaviour can be suppressed. However the {@link #discard()} method must be called to release resources
	 * explicitly in this case.
	 */
	private boolean keepAlive = false;
	private boolean clusterSpecific = false;
	
	private Date expiryTime = new Date(System.currentTimeMillis() + LISTENER_EXPIRY_TIME);
	
	public ZDPRequest (ZigBeeNIC nic) {
		this.nic = nic;
		sequenceId = ZCLSequence.getNext();

	}
	
	/**
	 * Query a device attributes asynchronously. When a suitable response is received
	 * the callback is invoked. 
	 */
	public void exec () throws IOException {
		
		log.debug ("exec() command=" + ByteFormatUtils.byteArrayToString(command));
		
		if (command == null) {
			log.error ("exec(): command[] is null");
			// TODO: IOException is not the right exception
			throw new IOException ("command[] is null");
		}
		
		// TODO: Creating byte[] and using System.arrayCopy will be faster
		ByteArrayOutputStream baout = new ByteArrayOutputStream();
		

		
		baout.write (sequenceId); // Sequence number
		
		
		baout.write (command);
		
		nic.addZigBeePacketListener(this);
		int frameId = nic.sendZigBeeCommand(
				address64, 
				address16,
				clusterId, Profile.ZIGBEE_DEVICE_PROFILE, 
				sourceEndpoint, destinationEndpoint, 
				baout.toByteArray());
		
		// TODO: adding ack handler after issuing the command is not idea.
		// We should be able to do this before issuing the command. But we
		// need the frameId. We could pass the ack handler as a parameter
		// to nic.sendZidBeeComand()
		nic.addAcknowledgementListener(this, frameId);		
	}
	
	
	/**
	 * This method is called by the NIC on reception of a ZigBee command
	 * packet as a result of the {@link ZigBeeNIC#addZigBeePacketListener(ZigBeePacketListener)}
	 * method invoked in {@link #exec()}.
	 */
	public void handleZigBeePacket(ZigBeePacket packet) {
		
		log.debug (this + " handleZigBeePacket() payload=" + ByteFormatUtils.byteArrayToString(packet.getPayload()));
		
		byte[] payload = packet.getPayload();
		
		// Some basic filtering. Application Profile ID must always match
		if (packet.getProfileId() != Profile.ZIGBEE_DEVICE_PROFILE) {
			log.trace ("ignoring packet: ZDP profile (0x0000) but got profileId=0x"
					+ Integer.toHexString(packet.getProfileId()));
			return;
		}
		
		
		// Expect from EP 0 (ZDO)
		if (packet.getSourceEndPoint() != 0x00) {
			log.trace ("ignoring packet: expecting srcEp=0x00 (ZDO) but got srcEp="
					+ packet.getSourceEndPoint());
			return;
		}
		
		if (packet.getDestinationEndPoint() != sourceEndpoint) {
			log.debug ("ignoring packet: expecting dstEp=" + sourceEndpoint 
					+ " but got dstEp=" + packet.getDestinationEndPoint());
			return;
		}
		
		
		// We won't use transaction sequence number (TSN) 0. If set to that value
		// don't apply a test for TSN.
		int seqId = payload[0] & 0xff;
		if ((seqId!=0) && (this.sequenceId != seqId)) {
			log.trace ("ignoring packet: expecting seq=" + this.sequenceId 
					+ " but got seq=" + seqId);
			return;
		}
		
		
		// If a callback filter was set use it now
		if (callbackFilter != null) {
			if ( ! callbackFilter.allow(packet)) {
				log.debug ("ignoring packet: rejected by callback filter");
				return;
			}
		}
		
		log.info(this + " handleZigBeePacket(): received expected ZDP response packet.");
		
		// TODO Some commands can have multiple response packets. So what do we do here?
		if (keepAlive) {
			log.info("This command keepAlive flag is set. Leaving NIC listeners in place.");
		} else {
			log.debug ("Unlistening to NIC");
			nic.removeZigBeePacketListener(this);
			nic.removeAcknowledgementListener(this);
		}
		
		// Move this to a protected method so that subclasses can override
		//callback.handleResponse(SUCCESS, null, packet.getPayload());
		//handleCallback (SUCCESS,null,packet);
		if (callback == null) {
			log.warn (this + " callback is null");
			return;
		}
		callback.handleZDPResponse(SUCCESS, null, this, packet.getPayload());
	}
	
	/**
	 * Subclasses can override this to interpret the packet and return 
	 * whatever data structures are required. The default implementation
	 * just returns the entire packet payload.
	 * 
	 * TODO: why bother sending back addr16, why not just send back entire
	 * ZigBee packet?
	 * 
	 * @param status
	 * @param addr16
	 * @param packet
	 */
	protected void handleCallback (int status, Address16 addr16, ZigBeePacket packet) {
		callback.handleZDPResponse(status, addr16, this, packet.getPayload());
	}
	
	
	
	public void handleAcknowledgement(int frameId, Ack ack) {
		// TODO: if status != 0x00 then send error back
		log.info ("handleAck(): got ACK,"
				+ " frameId=" + frameId 
				+ " status=0x" + Integer.toHexString(ack.deliveryStatus)
				+ " addr16=0x" + ack.addr16.toString()
				);
		
		commandAck = ack;
	
		switch (ack.deliveryStatus) {
			case 0x00:
				// Success: no action
				break;
		
			case 0x24:
				// Address not fond. For some reason getting these even though
				// the command is successful. So for the moment, treat
				// this error code as success
				log.warn("Got ADDRESS_NOT_FOUND, however will ignore this error.");
				break;
			default:
				// Error condition. Terminate command early.
				nic.removeZigBeePacketListener(this);
				nic.removeAcknowledgementListener(this);
				callback.handleZDPResponse(ack.deliveryStatus, null, this, null);
		}	
	}
	
	/**
	 * Explicitly release resources associated with this command.
	 */
	public void discard () {
		log.debug ("Unlistening to NIC");
		nic.removeZigBeePacketListener(this);
		nic.removeAcknowledgementListener(this);
	}
	
	//
	// Accessors
	//
	
	public Address16 getAddress16() {
		return address16;
	}

	public void setAddress16(Address16 address16) {
		this.address16 = address16;
	}
	
	public Address64 getAddress64() {
		return address64;
	}
	
	public void setAddress64(Address64 address64) {
		this.address64 = address64;
	}

	public int getClusterId() {
		return clusterId;
	}

	public void setClusterId(int clusterId) {
		this.clusterId = clusterId;
	}

	public int getSourceEndpoint() {
		return sourceEndpoint;
	}

	public void setSourceEndpoint(int sourceEndpoint) {
		this.sourceEndpoint = sourceEndpoint;
	}

	public int getDestinationEndpoint() {
		return destinationEndpoint;
	}

	public void setDestinationEndpoint(int destinationEndpoint) {
		this.destinationEndpoint = destinationEndpoint;
	}

	public byte[] getCommand() {
		return command;
	}

	public void setCommand(byte[] command) {
		this.command = command;
	}

	public ZDPResponseListener getCallback() {
		return callback;
	}

	@Deprecated
	public void setCallback(ZDPResponseListener callback) {
		this.callback = callback;
	}

	public void setCallback(ZDPResponseListener callback, ZigBeePacketFilter callbackFilter) {
		this.callback = callback;
		this.callbackFilter = callbackFilter;
	}
	
	public Ack getCommandAck() {
		return commandAck;
	}

	public void setCommandAck(Ack commandAck) {
		this.commandAck = commandAck;
	}

	public Date getExpiryTime() {
		return expiryTime;
	}

	public void setExpiryTime(Date expire) {
		this.expiryTime = expire;
		
	}

	public int getSequenceId() {
		return sequenceId;
	}

	public void setSequenceId(int sequenceId) {
		this.sequenceId = sequenceId;
	}

	/**
	 * @see {{@link #keepAlive}
	 * @param keepAlive
	 */
	public boolean isKeepAlive() {
		return keepAlive;
	}

	/**
	 * @see {{@link #keepAlive}
	 * @param keepAlive
	 */
	public void setKeepAlive(boolean keepAlive) {
		this.keepAlive = keepAlive;
	}

	public boolean isClusterSpecific() {
		return clusterSpecific;
	}

	/**
	 * TODO: wtf does this do? Need for some stuff to work.
	 * @param clusterSpecific
	 */
	public void setClusterSpecific(boolean clusterSpecific) {
		this.clusterSpecific = clusterSpecific;
	}

}
