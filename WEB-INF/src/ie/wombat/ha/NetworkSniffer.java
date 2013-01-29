package ie.wombat.ha;

import java.util.Date;


/**
 * Singleton that represents the home network.
 * 
 * @author joe
 *
 */
public class NetworkSniffer implements ZigBeePacketListener {

	
	private NetworkSniffer () {
	
	}
	
	
	public static void main (String[] arg) throws Exception {
		NetworkSniffer snif = new NetworkSniffer();
		snif.listen();
	}
	
	public void listen () {
		HANetwork han = HANetwork.getInstance(1L);
		han.getNIC().addZigBeePacketListener(this);
	}


	public void handleZigBeePacket(ZigBeePacket packet) {
		System.err.println (packet.toString());
	}


	public void handleAcknowledgement(int frameId, int status) {
		System.err.println ("ACK frameId=" + frameId + " status=" + status);		
	}


	public Date getExpiryTime() {
		// TODO Auto-generated method stub
		return null;
	}


	public void setExpiryTime(Date expire) {
		// TODO Auto-generated method stub
		
	}
	
	
}
