package ie.wombat.ha;

public interface ZigBeePacketFilter {

	/**
	 * Return true if packet matches filter.
	 * 
	 * @param packet
	 * @return
	 */
	public boolean allow (ZigBeePacket packet);
}
