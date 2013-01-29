package ie.wombat.ha;

import ie.wombat.zigbee.Ack;

public interface AcknowledgementListener extends Listener {

	/**
	 * When a Acknowledgement packet arrives listeners will have this method invoked.
	 * TODO: should this return an {@link Ack} object ?
	 * @param frameId
	 * @param status
	 */
	public void handleAcknowledgement (int frameId, Ack ack);

}
