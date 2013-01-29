package ie.wombat.ha.nic;

import ie.wombat.ha.Listener;

/**
 * Objects wishing to receive a NIC API frames must implement this interface. 
 * 
 * @author joe
 *
 */
public interface APIFrameListener extends Listener {

	public void handleAPIFrame (byte[] packet, int packetLen);
}
