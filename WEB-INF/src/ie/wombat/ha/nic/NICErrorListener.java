package ie.wombat.ha.nic;

import ie.wombat.ha.Listener;
import ie.wombat.ha.ZigBeeNIC;

public interface NICErrorListener extends Listener {
	public void handleNICError (ZigBeeNIC nic, int errorCode);
}
