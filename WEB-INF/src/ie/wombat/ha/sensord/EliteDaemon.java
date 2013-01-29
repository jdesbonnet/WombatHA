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
public class EliteDaemon extends Thread implements SerialPortEventListener {

	
	private SerialPort sioPort;

	private boolean debugFlag = false;

	private String sioDevName;
	private int baudrate;
	private BroadcastServerListener sensorBroadcaster;
	private CurrentValuesListener currentValuesBroadcaster;
	


	public EliteDaemon(String sioDeviceName, int speed, 
			BroadcastServerListener sensorBroadcaster,
			CurrentValuesListener currentValuesBroadcaster
			) throws IOException {

		this.sioDevName = sioDeviceName;
		this.baudrate = speed;
		this.sensorBroadcaster = sensorBroadcaster;
		this.currentValuesBroadcaster = currentValuesBroadcaster;

		setName("SIOReader-" + this.sioDevName);
	}

	public void run() {

		System.err.println ("EFERGY ELITE DAEMON STARTED");
		
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


		//
		// Listen to USB port for packets and relay to connected sockets
		//
		try {

			InputStreamReader sioInReader;
			sioInReader = new InputStreamReader(sioPort.getInputStream());
			// sioInReader = new InputStreamReader(System.in);

			LineNumberReader lnr = new LineNumberReader(sioInReader);

			String line;
			int power, ts;
			float f;

			while ((line = lnr.readLine()) != null) {

				// For completeness will relay all packets including those that
				// fail the checksum test
				ts = (int) (System.currentTimeMillis() / 1000L);
				String record = ts + "\t" + line;
				System.out.println(record);

				sensorBroadcaster.broadcastPacket(record);
				
				
				// Parse record
				String[] p = line.split("\\s+");
				
				if (p.length != 8) {
					System.err.println ("EfergyElite record != 8 bytes, ignoring");
					continue;
				}
				
				try {
					power = ((Integer.parseInt(p[3],16)&0x0f) << 8) 
					| Integer.parseInt(p[4],16);
					SensorReading reading = new SensorReading(ts, "electricity", "E", power);
					currentValuesBroadcaster.addSensorReading(reading);
				} catch (Exception e) {
					e.printStackTrace();
				}
				
				// Send [TODO]
				// System.err.print ("*"); System.err.flush();
				


			}

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		sioPort.removeEventListener();
		sioPort.close();

	}

	public void serialEvent(SerialPortEvent ev) {

		if (this.debugFlag) {
			System.err.println("Received serialport event: " + ev);
		}
	}


	public void setDebug(boolean b) {
		this.debugFlag = b;
	}

	public boolean isDebug() {
		return this.debugFlag;
	}

}
