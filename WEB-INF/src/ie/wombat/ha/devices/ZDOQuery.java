package ie.wombat.ha.devices;

import ie.wombat.ha.nic.xbee.XBeeDriver;
import ie.wombat.ha.nic.xbee.XBeeDriverFactory;
import ie.wombat.zigbee.address.Address64;
import ie.wombat.zigbee.zcl.AttributeValue;


import java.io.IOException;
import java.util.List;


public class ZDOQuery extends ZigBeeDeviceProxy {
	
	public ZDOQuery (Address64 address, XBeeDriver driver) {
		super(address,driver);
	}
	
	public static void main (String[] arg) throws Exception {
						
		//Address64 address = new Address64("00 13 7A 00 00 00 1F C4"); // Z800
		Address64 address = new Address64("00:13:a2:00:40:31:bd:77"); // XBee Coordinator
		//Address64 address = new Address64("FF FF FF FF FF FF FF FF");
		
		XBeeDriver driver = XBeeDriverFactory.getDriverFromArgs(arg);
		
		ZDOQuery zdo = new ZDOQuery(address,driver);
		
		
		zdo.getRoutingTable();
	
		
		System.err.println ("listening...");
		
		Thread.sleep(60000);
		
		System.err.println ("done!");
		driver.close();
		
		//sioPort.close();
			
	}
	
	public void getRoutingTable () throws IOException {
		int profileId = 0x0000; // ZigBee Device Profile (ZDP)
		int clusterId = 0x0031; // LQI Request
		
		int srcEp = 0x00;
		int dstEp = 0x00;
		
		byte[] command = {0x66, 0x00};
		execQuery(clusterId, profileId, srcEp, dstEp, command);
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
