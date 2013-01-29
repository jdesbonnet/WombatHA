package ie.wombat.ha.devices;



import java.io.IOException;

import org.apache.log4j.Logger;

import ie.wombat.ha.ByteFormatUtils;
import ie.wombat.ha.ZigBeeNIC;
import ie.wombat.ha.ZigBeePacket;

import ie.wombat.zigbee.ZigBeeCommand;
import ie.wombat.zigbee.address.Address16;
import ie.wombat.zigbee.address.Address64;
import ie.wombat.zigbee.zcl.Cluster;
import ie.wombat.zigbee.zcl.Profile;


/**
 * Cleode ZPlug driver.
 * 
 * Cluster 0x702:
 * AddrId 0x200: Status ?
 * AddrId 0x300: Unit of Measure? (SE spec, D.3.2.2.4)
 * AddrId 0x301: ?
 * AddrId 0x302: ?
 * 
 * AddrId 0x303: Summation formatting? See
 * http://www.jennic.com/files/support_files/JN-UG-3059-ZigBee-PRO-Smart-Energy.pdf
 * Section 8.7.4
 * 
 * AddrId 0x305
 * AddrId 0x306: MeteringDeviceType (SE spec, D.3.2.2.4)
 * 
 * 
 * @author joe
 *
 */
public class CleodeZPlug extends DeviceDriver implements Relay, HeatingDevice {

	private Logger log = Logger.getLogger (CleodeZPlug.class);
	
	public CleodeZPlug(Address64 address64, Address16 address16, ZigBeeNIC nic) {
		super(address64, address16, nic);
	}


	public boolean isBatteryPowered () {
		return false;
	}

	public void setState(boolean b) {	
		
		ZigBeeCommand zcmd = new ZigBeeCommand(nic);
		zcmd.setAddress16(address16);
		zcmd.setAddress64(address64);
		zcmd.setProfileId (Profile.HOME_AUTOMATION);
		zcmd.setClusterId (Cluster.ON_OFF);
		zcmd.setSourceEndpoint(10);
		zcmd.setDestinationEndpoint(1);
		zcmd.setSequenceId(ZigBeeCommand.AUTO_SEQUENCE);
		zcmd.setClusterSpecific(true);
		byte[] command = new byte[1];
		command[0] = (byte)(b ? 1 : 0);
		zcmd.setCommand(command);
		try {
			zcmd.exec();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}


	public boolean getState () {
		return getState(0);
	}
	public boolean getState(int zone) {
		return false;
	}


	@Override
	public void handleZigBeePacket(ZigBeePacket packet) {
		super.handleZigBeePacket(packet);

		byte[] payload = packet.getPayload();
		log.debug ("handleZigBeePacket() payload=" + ByteFormatUtils.byteArrayToString(payload));

	}


	public void setState(int zone, boolean b) {
		setState(b);
	}
	

}
