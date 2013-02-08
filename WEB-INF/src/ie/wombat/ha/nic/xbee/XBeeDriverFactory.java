package ie.wombat.ha.nic.xbee;

import gnu.io.PortInUseException;
import gnu.io.SerialPort;
import ie.wombat.ha.ZigBeeNIC;
import ie.wombat.ha.nic.ServletAdapter;
import ie.wombat.ha.sio.SIOUtil;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

import org.apache.log4j.Logger;

public class XBeeDriverFactory {
	
	private static Logger log = Logger.getLogger(XBeeDriverFactory.class);

	public static void setUARTFromArgs (XBeeDriver nic, String[] arg) throws IOException {
		if (arg[0].startsWith("/")) {
			XBeeDriverFactory.setSIOPort(nic,arg[0], 9600);
		} else {
			XBeeDriverFactory.setTCPPort(nic,arg[0], 4000);
		}
	}
	public static void setTCPPort (XBeeDriver nic, String host, int port) throws IOException {
		log.debug("returning TCP port driver host=" + host + " port=" + port);
		Socket sock = new Socket(host, port);
		InputStream sin = sock.getInputStream();
		OutputStream sout = sock.getOutputStream();
		XBeeStreamAdapter adapter = new XBeeStreamAdapter(nic, sin, sout);
		nic.setUARTAdapter(adapter);
	}
	
	public static void setSIOPort (XBeeDriver nic, String sioDeviceName, int speed) throws IOException {
		log.debug("returning Serial IO port driver");
		SerialPort sioPort;
		try {
			sioPort = SIOUtil.openSerialPort(sioDeviceName,9600);
		} catch (PortInUseException e) {
			throw new IOException ("Port " + sioDeviceName + " in use");
		}
		System.err.println ("sioPort=" + sioPort);
		if (sioPort == null) {
			throw new IOException ("Error opening port " + sioDeviceName);
		}
		//return new XBeeDriver(sioPort.getInputStream(), sioPort.getOutputStream());
		XBeeStreamAdapter io = new XBeeStreamAdapter(nic, sioPort.getInputStream(),  sioPort.getOutputStream());
		nic.setUARTAdapter(io);
	}
	
	public static void setHTTP (ZigBeeNIC nic) throws IOException {
		log.debug("returning HTTP driver");
		nic.setUARTAdapter(new ServletAdapter());
	}
	
}
