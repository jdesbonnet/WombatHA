package ie.wombat.ha.devices;



import java.io.IOException;

import org.apache.log4j.Logger;

import ie.wombat.ha.ZigBeeNIC;
import ie.wombat.ha.ZigBeePacket;

import ie.wombat.zigbee.WriteAttributesCommand;
import ie.wombat.zigbee.ZigBeeCommand;
import ie.wombat.zigbee.ZigBeeException;
import ie.wombat.zigbee.address.Address16;
import ie.wombat.zigbee.address.Address64;
import ie.wombat.zigbee.zcl.Attribute;
import ie.wombat.zigbee.zcl.Cluster;
import ie.wombat.zigbee.zcl.DataType;
import ie.wombat.zigbee.zcl.Profile;
import ie.wombat.zigbee.zcl.ZCLException;


/**
 * Cleode ZCare
 * 
 * @author joe
 *
 */
public class CleodeZCare extends DeviceDriver  {

	private Logger log = Logger.getLogger (CleodeZCare.class);
	
	
	
	public CleodeZCare(Address64 address64, Address16 address16, ZigBeeNIC nic) {
		super(address64, address16, nic);
	}


	public boolean isBatteryPowered () {
		return true;
	}
	
	/**
	 * Bit 0: manual alarm
	 * Bit 1: heart rate
	 * Bit 2: fall monitoring
	 * @param mask
	 */
	public void enableFunctions (int mask) {
		ZigBeeCommand zcmd = new ZigBeeCommand(nic);
		zcmd.setAddress16(address16);
		zcmd.setAddress64(address64);
		zcmd.setProfileId (Profile.HOME_AUTOMATION);
		zcmd.setClusterId (0x981);
		zcmd.setSourceEndpoint(10);
		zcmd.setDestinationEndpoint(1);
		zcmd.setSequenceId(ZigBeeCommand.AUTO_SEQUENCE);
		zcmd.setClusterSpecific(true);
		byte[] command = new byte[2];
		command[0] = 0x00; // Command code to set functionality
		command[1] = (byte)mask;
		zcmd.setCommand(command);
		try {
			zcmd.exec();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Get pulse counter
	 * @return
	 * @throws IOException 
	 * @throws ZCLException 
	 */
	public int getPulseCounter() throws ZigBeeException, IOException {
		return getIntegerAttribute(Profile.HOME_AUTOMATION,
				0x981, // System Status Cluster
				0x0a,  // Source EP 10
				0x01,  // Destination EP 1
				0x0000 // Attribute ID 0 
				);
	}


	/**
	 * Get pulse counter
	 * @return
	 * @throws IOException 
	 * @throws ZCLException 
	 */
	public int getPulseAverage() throws ZigBeeException, IOException {
		return getIntegerAttribute(Profile.HOME_AUTOMATION,
				0x981, // System Status Cluster
				0x0a,  // Source EP 10
				0x01,  // Destination EP 1
				0x0001 // Attribute ID
				);
	}
	

	/**
	 * Get capture period minutes. This seems to be the interval between taking readings.
	 * @return
	 * @throws IOException 
	 * @throws ZCLException 
	 */
	public int getCapturePeriod() throws ZigBeeException, IOException {
		return getIntegerAttribute(Profile.HOME_AUTOMATION,
				0x981, // System Status Cluster
				0x0a,  // Source EP 10
				0x01,  // Destination EP 1
				0x0002 // Attribute ID
				);
	}
	
	public void setCapturePeriod(int minutes)  throws ZigBeeException, IOException {
		/*
		return setIntegerAttribute(Profile.HOME_AUTOMATION,
				0x981, // System Status Cluster
				0x0a,  // Source EP 10
				0x01,  // Destination EP 1
				0x0002 // Attribute ID
				minutes
				);
		*/
	}

	/**
	 * Get poll rate (ms)
	 * @return
	 * @throws IOException 
	 * @throws ZCLException 
	 */
	public int getPollRate() throws ZigBeeException, IOException {
		return getIntegerAttribute(Profile.HOME_AUTOMATION,
				0x981, // System Status Cluster
				0x0a,  // Source EP 10
				0x01,  // Destination EP 1
				0x0003 // Attribute ID
				);
	}
	
	/**
	 * Get function status.
	 * Bit 0: Manual alarm
	 * Bit 1: Heart rate alarm.
	 * Bit 2: Fall alarm.
	 * Bit 3: Standby.
	 * @return
	 * @throws IOException 
	 * @throws ZCLException 
	 */
	public int getFunctionStatus() throws ZigBeeException, IOException {
		return getIntegerAttribute(Profile.HOME_AUTOMATION,
				0x981, // System Status Cluster
				0x0a,  // Source EP 10
				0x01,  // Destination EP 1
				0x0004 // Attribute ID
				);
	}
	
	/**
	 * Get pulse threshold. Threshold(ppm) = (X * Average) + 90.
	 * Default 0x2D.
	 * @return
	 * @throws IOException 
	 * @throws ZCLException 
	 */
	public int getPulseThreshold () throws ZigBeeException, IOException {
		return getIntegerAttribute(Profile.HOME_AUTOMATION,
				0x981, // System Status Cluster
				0x0a,  // Source EP 10
				0x01,  // Destination EP 1
				0x0005 // Attribute ID 
				);
	}
	
	
	/**
	 * Set period between heart rate measurements in seconds. 
	 * @param period Period between heart rate measurements in seconds.
	 * @throws ZigBeeException
	 * @throws IOException
	 */
	public void setHeartRateCapturePeriod (int period) throws ZigBeeException, IOException {
		Attribute attr = new Attribute(0x0002, DataType.UINT16, period/60);
		WriteAttributesCommand wattrCmd = new WriteAttributesCommand(nic);
		wattrCmd.setAddress64(address64);
		wattrCmd.setAddress16(address16);
		wattrCmd.setProfileId(Profile.HOME_AUTOMATION);
		wattrCmd.setClusterId(0x0981);
		wattrCmd.setSourceEndpoint(10);
		wattrCmd.setDestinationEndpoint(1);
		
		Attribute[] attributes = new Attribute[1];
		attributes[0] = attr;
		wattrCmd.setAttributes(attributes);
		wattrCmd.exec();
	}
	@Override
	public void handleZigBeePacket(ZigBeePacket packet) {	
		super.handleZigBeePacket(packet);
	}
	

}
