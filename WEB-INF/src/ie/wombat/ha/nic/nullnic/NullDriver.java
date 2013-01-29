package ie.wombat.ha.nic.nullnic;

import java.io.IOException;

import ie.wombat.ha.AcknowledgementListener;
import ie.wombat.ha.ZigBeeNIC;
import ie.wombat.ha.ZigBeePacketFilter;
import ie.wombat.ha.ZigBeePacketListener;
import ie.wombat.ha.nic.APIFrameListener;
import ie.wombat.ha.nic.UARTAdapter;
import ie.wombat.zigbee.address.Address16;
import ie.wombat.zigbee.address.Address64;

public class NullDriver implements ZigBeeNIC {

	public int sendZigBeeCommand(Address64 addr64, int clusterId,
			int profileId, int srcEp, int dstEp, byte[] command)
			throws IOException {
		// TODO Auto-generated method stub
		return 0;
	}

	public int sendZigBeeCommand(Address16 addr16, int clusterId,
			int profileId, int srcEp, int dstEp, byte[] command)
			throws IOException {
		// TODO Auto-generated method stub
		return 0;
	}

	public int sendZigBeeCommand(Address64 addr64, Address16 addr16,
			int clusterId, int profileId, int srcEp, int dstEp, byte[] command)
			throws IOException {
		// TODO Auto-generated method stub
		return 0;
	}

	public void addZigBeePacketListener(ZigBeePacketListener listener) {
		// TODO Auto-generated method stub
		
	}

	public void addZigBeePacketListener(ZigBeePacketListener listener,
			ZigBeePacketFilter filter) {
		// TODO Auto-generated method stub
		
	}

	public void removeZigBeePacketListener(ZigBeePacketListener listener) {
		// TODO Auto-generated method stub
		
	}

	public void addAcknowledgementListener(AcknowledgementListener listener) {
		// TODO Auto-generated method stub
		
	}

	public void addAcknowledgementListener(AcknowledgementListener listener,
			int frameId) {
		// TODO Auto-generated method stub
		
	}

	public void removeAcknowledgementListener(AcknowledgementListener listener) {
		// TODO Auto-generated method stub
		
	}

	public void sendAPIFrame(byte[] frame, int frameLen) throws IOException {
		// TODO Auto-generated method stub
		
	}

	public void handleAPIFrame(byte[] frame, int frameLen) {
		// TODO Auto-generated method stub
		
	}

	public void addAPIPacketListener(APIFrameListener listener) {
		// TODO Auto-generated method stub
		
	}

	public void removeAPIPacketListener(APIFrameListener listener) {
		// TODO Auto-generated method stub
		
	}

	public UARTAdapter getUARTAdapter() {
		// TODO Auto-generated method stub
		return null;
	}

	public void ping() throws IOException {
		// TODO Auto-generated method stub
		
	}

	public void reset() throws IOException {
		// TODO Auto-generated method stub
		
	}
	
	/**
	 * {@inheritDoc}
	 */
	public void test () throws IOException {
		
	}

	public long getLastRxTime() {
		// TODO Auto-generated method stub
		return 0;
	}

	public void close() {
		// TODO Auto-generated method stub
		
	}

}
