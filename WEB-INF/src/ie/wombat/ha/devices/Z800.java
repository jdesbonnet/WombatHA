package ie.wombat.ha.devices;

import ie.wombat.ha.nic.xbee.XBeeDriver;
import ie.wombat.ha.nic.xbee.XBeeDriverFactory;
import ie.wombat.zigbee.address.Address64;
import ie.wombat.zigbee.zcl.AttributeValue;


import java.io.IOException;
import java.util.List;


public class Z800 extends ZigBeeDeviceProxy {
	
	public Z800 (Address64 address, XBeeDriver driver) {
		super(address,driver);
	}
	
	public static void main (String[] arg) throws Exception {
						
		Address64 address = new Address64("00 13 7A 00 00 00 1F C4");
		XBeeDriver driver = XBeeDriverFactory.getDriverFromArgs(arg);
	
		Z800 z800 = new Z800(address,driver);
		
		// Set switch state (last byte of 3 byte command)
		String swState = arg[2];
		int state=0;
		if ("on".equals(swState)) {
			state=0x01;
		} else if ("off".equals(swState)) {
			state=0x00;
		} else if ("toggle".equals(swState)) {
			state=0x02;
		} else if ("query".equals(swState)) {
			state=0x03;
		} else if ("av".equals(swState)) {
			state=0x04;
		}
		
		
		switch (state) {
		case 0x00:
		case 0x01:
		case 0x02:
		try {
			System.err.println ("Setting state to " + swState);
			z800.setState(state);
			System.err.println ("State set.");
		} catch (IOException e) {
			e.printStackTrace();
		}
		break;
		case 0x03:
			try {
				boolean on = z800.getState();
				System.err.println ("state=" + on);
			} catch (IOException e) {
				e.printStackTrace();
			}
			break;
		case 0x04:
			int[] mAV = new int[2];
		
			try {
				z800.getPowerReading(mAV);
				System.err.println ("mA=" + mAV[0] + " V=" + mAV[1]);
			} catch (IOException e) {
				e.printStackTrace();
			}
			break;
		}
		
		System.err.println ("done!");
		driver.close();	
		System.exit(0);
	}
	
	public void setState (int state) throws IOException {
		byte[] command = {0x11, 0x01, 0x00};
		command[2] = (byte)state;
		int status = execCommand(0x0006, 0x0104, 0x0A, 0x0A, command);
		if (status != SUCCESS) {
			throw new IOException ("command fail, status=" + status);
		}
	}
	

	public void getPowerReading (int[] mAV) throws IOException {
		
		int[] attrIds = {0xe000,0xe001};
		List<AttributeValue> list  = queryAttributes(0x0702, 0x0104, 0x0A, 0x0A, attrIds);
		mAV[0] = list.get(0).getIntValue();
		mAV[1] = list.get(1).getIntValue();

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
