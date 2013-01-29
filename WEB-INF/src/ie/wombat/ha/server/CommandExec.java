package ie.wombat.ha.server;

import java.io.IOException;
import java.util.Date;

import org.apache.log4j.Logger;

import ie.wombat.ha.AcknowledgementListener;
import ie.wombat.ha.ZigBeeNIC;
import ie.wombat.ha.ZigBeePacket;
import ie.wombat.ha.ZigBeePacketFilter;
import ie.wombat.ha.ZigBeePacketListener;
import ie.wombat.zigbee.Ack;
import ie.wombat.zigbee.address.Address64;

/**
 * Execute/send a Command. Send only one command per object. Discard and
 * recreate for subsequent commands.
 * 
 * @author joe
 *
 */
public class CommandExec implements ZigBeePacketListener, AcknowledgementListener{

	Logger log = Logger.getLogger(CommandExec.class);
	
	public static final int SEND_SUCCESS = 0x00;
	public static final int TIMEOUT = 20000;
	
	private static final long DEFAULT_EXPIRY_TIME = 30000L;

	// ZigBee NIC
	private ZigBeeNIC nic;
		
	// ACK handler to ignore ACKs unless it matches with this frame ID
	//private int waitForAckWithFrameId;
	
	// ZigBee packet handler to ignore packets unless it matches this ZCL seq
	private int waitForSeq;
	
	private boolean gotAck = false;
	private boolean gotResponse = false;
	
	private Command command;
	
	private Date expiryTime = new Date(System.currentTimeMillis() + DEFAULT_EXPIRY_TIME);

	public CommandExec (ZigBeeNIC nic) {
		this.nic = nic;	
	}
	
	public synchronized void sendCommand (Command command) {
		
		log.debug("sendCommand(): command=" + command);
		
		this.command = command;
		
		long commandStartTime = System.currentTimeMillis();
		
		byte[] payloadBytes = command.getPayloadBytes();

		
		log.debug ("sendCommand(): register myself as ACK/ZigBeePacket listener");
		nic.addZigBeePacketListener(this, new ZigBeePacketFilter() {
			
			public boolean allow(ZigBeePacket packet) {
				return true;
			}
		});
		
		
		
		
		Address64 addr64 = new Address64(command.getDstDevice().getAddress64());
		
		int frameId=-1;
		try {
			frameId = nic.sendZigBeeCommand(addr64, command.getClusterId(), command.getProfileId(),
					command.getSrcEp(), command.getDstEp(), payloadBytes);
			
			// TODO: I would much prefer to register this *before* sending the zigbee 
			// command as there is a possibility that the ack might be missed. However
			// right now, the only way to get the frameId is to send the command. 
			// TODO: what if we pass the callback with the command? Saves register
			// and unregister mess. This will work nicely with the ACKs, but can we
			// neatly associate responses with queries? 
			nic.addAcknowledgementListener(this,frameId);
			
			command.setSendStatus(SEND_SUCCESS);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		log.debug("sendCommand(): frameId=" + frameId);
		
		command.setFrameId(frameId);
		command.setExecTime(new Date());
		
		//waitForAckWithFrameId = frameId;
		waitForSeq = payloadBytes[1] & 0xff;
		
		
		// Loop until we've got and ACK for the command and a response to the query
		// TODO: how do we know we're expecting a response?
		
		while (! (gotAck && gotResponse) ) {
			
			if ( (System.currentTimeMillis() - commandStartTime) >= TIMEOUT ) {
				log.error ("Timeout!");
				nic.removeAcknowledgementListener(this);
				nic.removeZigBeePacketListener(this);
				// TODO: throw exception?
				return;
			}
			try {
				log.debug ("sendCommand(): SLEEP");
				// Wait for a period to coincide with the timeout time
				log.debug ("waiting for " + (TIMEOUT - (System.currentTimeMillis()-commandStartTime) ) + "ms");
				wait(TIMEOUT - (System.currentTimeMillis()-commandStartTime) );
				log.debug ("sendCommand(): WAKE");
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		log.debug ("sendCommand(): unregister myself as ACK/ZigBeePacket listener");
		nic.removeAcknowledgementListener(this);
		nic.removeZigBeePacketListener(this);
		
		log.debug ("sendCommand(): got ACK and Response. We're done here! Bye.");
		
		command.getDstDevice().setLastContactTime(new Date());
		
	}
	public synchronized void handleZigBeePacket(ZigBeePacket packet) {
		log.debug("handleZigBeePacket()");
		
		if (packet.getPayload().length < 2) {
			log.debug("handleZigBeePacket(): packet too short, ignoring");
			return;
		}
		
		int seq = packet.getPayload()[1] & 0xff;
		if (seq == waitForSeq) {
			log.debug("handleZigBeePacket(): yes, this is the response we were waiting for!");
			gotResponse = true;
			
			// TODO: dangerous - should not use the driver thread there I think
			command.setResponseTime(new Date());
			command.setResponsePayload(packet.getPayload());
			// copy packet
			notifyAll();
		}
	}

	public synchronized void handleAcknowledgement(int frameId, Ack ack) {
		
		log.debug("handleAcknowledgement(): frameId=" + frameId + " status=" + ack.deliveryStatus);
		
		// As this listener was registered with a frameId filter we can assume that
		// the frameId is the one we're listening for.
		
		log.debug("handleAcknowledgement(): yes, this is the ACK we were waiting for!");
		gotAck = true;
			
		// TODO: dangerous - should not use the driver thread there I think
		command.setAckTime(new Date());
		command.setAckStatus(ack.deliveryStatus);
			
		notifyAll();
	}

	public Date getExpiryTime() {
		// TODO Auto-generated method stub
		return null;
	}

	public void setExpiryTime(Date expire) {
		// TODO Auto-generated method stub
		
	}

}
