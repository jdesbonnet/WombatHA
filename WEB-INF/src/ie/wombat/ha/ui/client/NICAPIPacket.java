package ie.wombat.ha.ui.client;

import java.io.Serializable;

import com.google.gwt.user.client.rpc.IsSerializable;


public class NICAPIPacket implements Serializable, IsSerializable {

	public String timestamp;
	public String packetHex;
	
	public String toString () {
		return timestamp + "    " + packetHex;
	}
}
