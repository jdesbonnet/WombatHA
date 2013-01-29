package ie.wombat.ha;

/**
 * Objects that want to receive ZigBee packets received by the NIC must implement
 * this interface and register themselves as listeners with the {@link ZigBeeNIC#addZigBeePacketListener(ZigBeePacketListener)
 * method. These call backs are implemented by the NIC driver listening thread and implementations
 * of this interface should
 * be as quick and short as possible.
 * 
 * @author joe
 *
 */
public interface ZigBeePacketListener extends Listener {

	/**
	 * When a ZigBee packet arrives listeners will have this method invoked.
	 * @param packet
	 */
	public void handleZigBeePacket (ZigBeePacket packet);
	
}
