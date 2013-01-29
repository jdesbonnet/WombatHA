package ie.wombat.ha;

import ie.wombat.zigbee.zdo.NeighborTableEntry;

public interface NetworkDiscoveryListener {
	void networkDiscoveryDeviceFound (NeighborTableEntry rtr);
	void networkDiscoveryComplete ();
}
