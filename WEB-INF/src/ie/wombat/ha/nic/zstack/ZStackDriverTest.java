package ie.wombat.ha.nic.zstack;

import gnu.io.PortInUseException;
import gnu.io.SerialPort;
import ie.wombat.ha.HANetwork;
import ie.wombat.ha.ZigBeeNIC;
import ie.wombat.ha.sio.SIOUtil;
import ie.wombat.ha.ui.client.DeviceInfo;
import ie.wombat.ha.ui.server.ZDOServiceImpl;
import ie.wombat.zigbee.ZigBeeCommand;
import ie.wombat.zigbee.address.Address16;
import ie.wombat.zigbee.address.Address64;
import ie.wombat.zigbee.zcl.Cluster;
import ie.wombat.zigbee.zcl.Profile;


import java.io.IOException;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;

public class ZStackDriverTest {

	private static Logger log = Logger.getLogger(ZStackDriverTest.class);
	
	public static void main (String[] arg) throws Exception {
		
		BasicConfigurator.configure();
		
		String sioDeviceName = arg[0];
		int speed = Integer.parseInt(arg[1]);
		
		log.debug("returning Serial IO port driver");
		SerialPort sioPort;
		try {
			sioPort = SIOUtil.openSerialPort(sioDeviceName,speed);
		} catch (PortInUseException e) {
			throw new IOException ("Port " + sioDeviceName + " in use");
		}
		System.err.println ("sioPort=" + sioPort);
		if (sioPort == null) {
			throw new IOException ("Error opening port " + sioDeviceName);
		}
		//return new XBeeDriver(sioPort.getInputStream(), sioPort.getOutputStream());
		ZStackStreamAdapter io = new ZStackStreamAdapter(sioPort.getInputStream(),  sioPort.getOutputStream());
		ZStackDriver nic = new ZStackDriver();
		nic.setUARTAdapter(io);
		
		
		Address16 addr16 = new Address16("F298");
		identify(nic, addr16);
		
	}
	
	private static void identify (ZStackDriver nic, Address16 addr16) throws IllegalArgumentException {
		
		ZigBeeCommand zcmd = new ZigBeeCommand(nic);
		//cmd.setSequenceId(ZigBeeCommand.NO_SEQUENCE);
		zcmd.setSequenceId(ZigBeeCommand.AUTO_SEQUENCE);
				
		zcmd.setAddress64(Address64.UNKNOWN);
		zcmd.setAddress16(addr16);
		zcmd.setSourceEndpoint(0x0a);
		zcmd.setDestinationEndpoint(0xff);
		zcmd.setProfileId(Profile.HOME_AUTOMATION);
		zcmd.setClusterId(Cluster.IDENTIFY);
		
		byte[] command = {0x00,0x3c,0x00};
		zcmd.setCommand(command);
		zcmd.setClusterSpecific(true);
		
		try {
			zcmd.exec();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
}
