package ie.wombat.ha.mrf24j40;

import gnu.io.CommPort;
import gnu.io.CommPortIdentifier;
import gnu.io.PortInUseException;
import gnu.io.SerialPort;
import gnu.io.SerialPortEvent;
import gnu.io.SerialPortEventListener;
import gnu.io.UnsupportedCommOperationException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.TooManyListenersException;

public class SIOTest  implements SerialPortEventListener {
	
	
	
	public static void main (String arg[]) throws Exception {
		
		String sioDevName = "/dev/ttyUSB1";
		int baudRate = 4800;
		
		SerialPort sioPort = null;
		

		Enumeration<CommPortIdentifier> ports = CommPortIdentifier
				.getPortIdentifiers();
		while (ports.hasMoreElements()) {
			CommPortIdentifier cpi = (CommPortIdentifier) ports.nextElement();
			if (cpi.getPortType() == CommPortIdentifier.PORT_SERIAL
					&& cpi.getName().equals(sioDevName)) {
				try {
					sioPort = (SerialPort) cpi.open("SIOReader", 2000);
				} catch (PortInUseException e) {
					System.err.println("error: port in use");
					return;
				}
			}
		}

		if (sioPort == null) {
			System.err.println("error: serial port " + sioDevName
					+ " not found.");
			
			return;
		}

	
		sioPort.setSerialPortParams(baudRate, SerialPort.DATABITS_8,
					SerialPort.STOPBITS_1, SerialPort.PARITY_NONE);
		sioPort.setFlowControlMode(SerialPort.FLOWCONTROL_NONE);
		//sioPort.addEventListener(this);
		//sioPort.notifyOnDataAvailable(true);
			
		//sioPort.enableReceiveTimeout(5000);
			
		
		OutputStream sioOut = sioPort.getOutputStream();
		while (true) {
			System.err.print("."); System.err.flush();
			sioOut.write('\r');
			Thread.sleep(200);
		}
	}

	public void serialEvent(SerialPortEvent arg0) {
		System.err.println ("*** SIO_EVENT:" + arg0);
	}
}
