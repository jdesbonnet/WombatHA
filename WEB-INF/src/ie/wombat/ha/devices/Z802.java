package ie.wombat.ha.devices;

import ie.wombat.ha.nic.xbee.XBeeDriver;
import ie.wombat.ha.nic.xbee.XBeeDriverFactory;
import ie.wombat.zigbee.address.Address64;
import ie.wombat.zigbee.zcl.AttributeValue;

import java.io.IOException;
import java.util.List;

public class Z802 extends ZigBeeDeviceProxy {
	
	public Z802 (Address64 address, XBeeDriver driver) {
		super(address,driver);
	}
	
	public static void main (String[] arg) throws Exception {
		
		Address64 address = new Address64("00 13 7A 00 00 00 38 50");
		XBeeDriver driver;
		if (arg[0].startsWith("/")) {
			driver = XBeeDriverFactory.getSIOPortDriver(arg[0], 9600);
		} else {
			driver = XBeeDriverFactory.getTCPPortDriver(arg[0], 4000);
		}
	
		Z802 z802 = new Z802(address,driver);
		
		
		
		// Set switch state (last byte of 3 byte command)
		int zone = Integer.parseInt(arg[1]);
		String swState = arg[3];
		int state=0;
		if ("on".equals(swState)) {
			state=0x01;
		} else if ("off".equals(swState)) {
			state=0x00;
		} else if ("toggle".equals(swState)) {
			state=0x02;
		}
		
		z802.setState(zone,state);
	
	}
	
	public int setState (int zone, int state) throws IOException {
		byte[] command = {0x11, 0x01, 0x00};
		command[2] = (byte)state;
		return execCommand(0x0006, 0x0104, 0x0A, zone, command);
	}
	
	public boolean getState (int zone) throws IOException {
		
		byte[] command = {0x00, // ZCL FC
				//(byte)0x9f, 0x10, // ?? Manuf specific
				0x00, // Sequence Number 
				0x00, // Command Read Attr 
				0x00, (byte)0x00, // Attr ID 0x0000 (state)
		};
			
		List<AttributeValue> list =  execQuery (0x0006, 0x0104, 0x0A, zone, command);
		return (list.get(0).getIntValue() == 1);
	}
	
	
	public void getPowerReading (int zone, int[] mAV) throws IOException {
		
		int[] attrIds = {0xe000,0xe001};
		List<AttributeValue> list  = queryAttributes(0x0702, 0x0104, 0x0A, zone, attrIds);
		mAV[0] = list.get(0).getIntValue();
		mAV[1] = list.get(1).getIntValue();

	}
	

}
