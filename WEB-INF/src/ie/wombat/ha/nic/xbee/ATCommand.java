package ie.wombat.ha.nic.xbee;


import ie.wombat.ha.nic.APIFrameListener;


import java.io.IOException;
import java.util.Date;

import org.apache.log4j.Logger;

/**
 * XBee AT command
 * 
 * @author joe
 *
 */
public class ATCommand implements APIFrameListener {

	private static Logger log = Logger.getLogger(ATCommand.class);

	/**
	 * NIC will automatically unregister this object as listener after this number of ms.
	 */
	private static final long LISTENER_EXPIRY_TIME = 30000L;
	
	private XBeeDriver xbee;
	private String command;
	private byte[] parameters;
	private int frameId;
	
	Date expiryTime = new Date (System.currentTimeMillis() + LISTENER_EXPIRY_TIME);
	
	private ATCommandResponse callback;
	
	public ATCommand (XBeeDriver xbee) {
		this.xbee = xbee;
	}
	
	/**
	 * Send AT command asynchronously. When a suitable response is received
	 * the callback is invoked. 
	 * 
	 * @param profileId
	 * @param clusterId
	 * @param srcEp
	 * @param dstEp
	 * @param attrIds
	 * @param callback
	 * @throws IOException
	 */
	public void exec () throws IOException {
		
		log.debug ("exec()");
	
		xbee.addAPIPacketListener(this);
		frameId = xbee.getNextXBeeFrameId();
		xbee.sendLocalATCommand(frameId,command, parameters);
	}
	
	/**
	 * This method will be called by {@link XBeeDriver} on reception of a
	 * XBee API packet. Need to ignore those that are not relevant to the
	 * AT command.
	 */
	public void handleAPIFrame (byte[] packet, int packetLen) {
		
		log.debug ("handleXBeeAPIPacket()");
		
		int frameType = packet[0] & 0xff;
		if (frameType != 0x88) {
			log.debug ("handleXBeeAPIPacket(): ignoring packet because not AT command response (type 0x88)");
			return;
		}
		
		int packetFrameId = packet[1] & 0xff;
		if (packetFrameId != frameId) {
			log.debug ("handleXBeeAPIPacket(): ignoring packet because frameId " 
					+ packetFrameId + " != " + frameId);
			return;
		}
		
		// Got response to AT Command. 
		
		// Check status.
		int atStatus = packet[4] & 0xff;
		if (atStatus != 0x00) {
			callback.handleResponse(atStatus, packet, packetLen);
		} else {
			callback.handleResponse(atStatus,packet, packetLen);
		}
		log.debug ("handleXBeeAPIPacket(): Unlistening to NIC");
		xbee.removeAPIPacketListener(this);
	
	}


	
	
	//
	// Accessors
	//

	public String getCommand() {
		return command;
	}

	public void setCommand(String command) {
		this.command = command;
	}

	public ATCommandResponse getCallback() {
		return callback;
	}

	public void setCallback(ATCommandResponse callback) {
		this.callback = callback;
	}

	public Date getExpiryTime() {
		return expiryTime;
	}

	public void setExpiryTime(Date expire) {
		this.expiryTime = expire;
		
	}

	public byte[] getParameters() {
		return parameters;
	}

	public void setParameters(byte[] parameters) {
		this.parameters = parameters;
	}
	
	

}
