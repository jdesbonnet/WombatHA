package ie.wombat.ha;


import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;
import java.util.HashSet;

import gnu.io.PortInUseException;
import gnu.io.SerialPort;
import ie.wombat.ha.app.AppBase;
import ie.wombat.ha.app.AppFactory;
import ie.wombat.ha.devices.DeviceDriver;
import ie.wombat.ha.devices.DeviceFactory;
import ie.wombat.ha.devices.XBeeSeries2;
import ie.wombat.ha.nic.UARTAdapter;
import ie.wombat.ha.nic.xbee.RepeaterServerThread;
import ie.wombat.ha.nic.xbee.TCPRelay;
import ie.wombat.ha.nic.xbee.XBeeDriver;
import ie.wombat.ha.nic.xbee.XBeeStreamAdapter;
import ie.wombat.ha.nic.zstack.ZStackStreamAdapter;
import ie.wombat.ha.server.Application;
import ie.wombat.ha.server.Device;
import ie.wombat.ha.server.Network;
import ie.wombat.ha.sio.SIOUtil;
import ie.wombat.ha.ui.server.AddressServiceImpl;
import ie.wombat.zigbee.address.Address16;
import ie.wombat.zigbee.address.Address64;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;
import org.jfree.util.Log;

/**
 * 
 * 
 * @author joe
 *
 */
public class XBeeCmdLine {

	private static Logger log = Logger.getLogger(XBeeCmdLine.class);
	
	private static boolean NETWORK_ENUM_EN = true;
	
	public static void main (String[] arg) throws Exception {
		BasicConfigurator.configure();
		
		System.err.println ("Starting HA Network");


		Network network = new Network();
		network.setId(1L);
		network.setNicDriver("ie.wombat.ha.nic.xbee.XBeeDriver:sio:/dev/ttyUSB0:9600");
		
		
		
		// SIO (UART) adapter expects additional deviceName, speed parameters
		String nicDeviceName = arg[0];
		int nicDeviceSpeed = Integer.parseInt(arg[1]);

		log.info ("SIO Adapter: device=" + nicDeviceName + " speed=" + nicDeviceSpeed);

		SerialPort sioPort;
		try {
			sioPort = SIOUtil.openSerialPort(nicDeviceName,nicDeviceSpeed);
		} catch (PortInUseException e) {
			throw new IOException("Port " + nicDeviceName + " in use");
		} catch (Error e) {
			throw new IOException("Unknown error opening port " + nicDeviceName );
		}
		if (sioPort == null) {
			log.error("sioPort==null, unable to open serial port " + nicDeviceName);
			throw new IOException("Unable to open serial port " + nicDeviceName);
		}
		
		
		XBeeDriver nic = new XBeeDriver();
			
		UARTAdapter uartAdapter = new XBeeStreamAdapter((XBeeDriver)nic,sioPort.getInputStream(),
				sioPort.getOutputStream());
		
		nic.setUARTAdapter(uartAdapter);
		
		nic.sendLocalATCommand(4, "DH", new byte[0]);
		nic.sendLocalATCommand(5, "DL", new byte[0]);

		
		Address64 remoteAddr64 = new Address64("00:13:A2:00:40:31:BD:5E");
		byte[] dh = {0x00,0x13,(byte)0xA2,0x00};
		byte[] dl = {0x40,0x31,(byte)0xBD, 0x5E};
		nic.sendLocalATCommand(6, "DH", dh);
		nic.sendLocalATCommand(7, "DL", dl);

		System.err.println ("******************************************");
		
		nic.sendLocalATCommand(8, "DH", new byte[0]);
		nic.sendLocalATCommand(9, "DL", new byte[0]);
		
		XBeeSeries2 xbee = new XBeeSeries2(new Address64("00:13:A2:00:40:31:BD:5E"), Address16.UNKNOWN, nic);
		//xbee.sendData("?\r\n".getBytes());
		
	
	}
}
