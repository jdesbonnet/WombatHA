/*
 *
 */
package ie.wombat.ha.sensord;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TooManyListenersException;

import gnu.io.CommPortIdentifier;
import gnu.io.PortInUseException;
import gnu.io.SerialPort;
import gnu.io.SerialPortEvent;
import gnu.io.SerialPortEventListener;
import gnu.io.UnsupportedCommOperationException;

/**
 * Read lines from serial IO, prefix with time stamp and make available via
 * server which listens on socket and echos packets to connected clients.
 * 
 */
public class FRAMTest  implements SerialPortEventListener {

	
	private SerialPort sioPort;

	private boolean debugFlag = false;

	private String sioDevName;
	private int baudrate;

	

	public FRAMTest (String sioDevName) {
		this.sioDevName = sioDevName;
		this.baudrate = 9600;
	}
	
	public static void main (String[] arg) throws IOException {
		FRAMTest test = new FRAMTest(arg[0]);
		test.run();
	}
	
	public void run() throws IOException {

		
		sioPort = null;

		Enumeration<CommPortIdentifier> ports = CommPortIdentifier
				.getPortIdentifiers();
		while (ports.hasMoreElements()) {
			CommPortIdentifier cpi = (CommPortIdentifier) ports.nextElement();
			if (cpi.getPortType() == CommPortIdentifier.PORT_SERIAL
					&& cpi.getName().equals(this.sioDevName)) {
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

		/*
		 * Set serial port
		 */
		System.err.println("bps=" + this.baudrate);

		try {
			sioPort.setSerialPortParams(this.baudrate, SerialPort.DATABITS_8,
					SerialPort.STOPBITS_1, SerialPort.PARITY_NONE);
			sioPort.setFlowControlMode(SerialPort.FLOWCONTROL_RTSCTS_OUT);
			sioPort.addEventListener(this);
			sioPort.notifyOnDataAvailable(true);

		} catch (UnsupportedCommOperationException e) {
			System.err.println("error: " + e);
			return;
		} catch (TooManyListenersException e) {
			System.err.println("error: " + e);
			return;
		}


		InputStream in = sioPort.getInputStream();
		int c;
		while ( (c = in.read()) != -1) {
			System.out.print(" " + c);	
		}

		sioPort.removeEventListener();
		sioPort.close();

	}

	public void serialEvent(SerialPortEvent ev) {

		if (this.debugFlag) {
			System.err.println("Received serialport event: " + ev);
		}
	}


}
