package ie.wombat.ha.devices;

import ie.wombat.ha.ZigBeeNIC;
import ie.wombat.ha.nic.xbee.XBeeDriver;
import ie.wombat.ha.nic.xbee.XBeeDriverFactory;
import ie.wombat.zigbee.address.Address16;
import ie.wombat.zigbee.address.Address64;
import ie.wombat.zigbee.zcl.AttributeValue;


import java.io.IOException;
import java.util.List;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;

/**
 * Cleode ZRC (temperature sensor and switch)
 * 
 * @author joe
 *
 */
public class ZRC extends ZigBeeDeviceProxy {
	
	private static Logger log = Logger.getLogger(ZRC.class);

	
	public ZRC (Address64 address, ZigBeeNIC driver) {
		super(address,driver);
	}
	
	public ZRC (Address16 address, ZigBeeNIC driver) {
		super(address,driver);
	}
	
	public static void main (String[] arg) throws Exception {
		
		BasicConfigurator.configure();
		log.info("Running...");
		
		Address64 address = new Address64("10:00:00:50:c2:10:00:63");

		log.debug ("Opening NIC");
		//XBeeDriver driver = XBeeDriverFactory.getDriverFromArgs(arg);
		ZigBeeNIC driver = XBeeDriverFactory.getSIOPortDriver("/dev/ttyUSB4", 9600);
		ZRC zrc = new ZRC(address,driver);
		
		log.debug ("NIC=" + driver);
		
		// Set switch state (last byte of 3 byte command)
		String cmd = arg[2];
		if ("query".equals(cmd)) {
			zrc.getTemperature();
		} 
		
		
		System.err.println ("done!");
		driver.close();	
		System.exit(0);
	}
	
	

	public void getTemperature () throws IOException {
		
		log.debug ("getTemperature()");
		
		int[] attrIds = {0x0000};
		List<AttributeValue> list  = queryAttributes(
				0x0402, // Cluster ID 
				0x0104, // Profile ID 
				0x0A, // Source Endpoint
				0x03, // Destination Endpoint
				attrIds);
		

	}
	
	
	public boolean getState () throws IOException {
		
		byte[] command = {0x14, // ZCL FC
				(byte)0x9f, 0x10, // ?? Manuf specific
				0x00, // Sequence Number 
				0x00, // Command Read Attr 
				0x00, (byte)0x00, // Attr ID 0x0000 (state)
		};
			
		List<AttributeValue> list =  execQuery (0x0006, 0x0104, 0x0A, 0x0A, command);
		return (list.get(0).getIntValue() == 1);
	}
	

	
}
