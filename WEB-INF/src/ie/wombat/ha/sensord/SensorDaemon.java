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
public class SensorDaemon extends Thread implements SerialPortEventListener {

	private static final String VERSION = "0.1";
	private static final int DEFAULT_BPS = 19200;
	public static final int LISTEN_PORT = 4444;
	public static final int LISTEN_PORT2 = 4445;

	private SerialPort sioPort;

	private boolean debugFlag = false;

	private String sioDevName;
	private int baudrate = DEFAULT_BPS;

	public static Map<String, SensorReading> lastReadingMap = Collections
			.synchronizedMap(new HashMap<String, SensorReading>());

	public static void main(String[] arg) throws Exception {

		List<String> argList = new ArrayList<String>(arg.length);

		boolean debugFlag = false;
		int baudRate = DEFAULT_BPS;

		for (int i = 0; i < arg.length; i++) {
			if ("-debug".equals(arg[i])) {
				debugFlag = true;
				continue;
			}
			if ("-baudrate".equals(arg[i])) {
				try {
					i++;
					baudRate = Integer.parseInt(arg[i]);
				} catch (NumberFormatException e) {
					System.err.println("baudrate must be an integer");
					return;
				}
				continue;
			}

			if ("-version".equals(arg[i])) {
				System.err.println("Version: " + VERSION);
				return;
			}
			if ("-help".equals(arg[i])) {
				usage();
				return;
			}

			argList.add(arg[i]);
		}

		if (argList.size() != 1) {
			System.err.println("expecting just one arg, got " + argList.size());
			usage();
			return;
		}

		String sioDeviceName = argList.get(0);

		SensorDaemon driver = new SensorDaemon(sioDeviceName, baudRate);
		driver.start();
	}

	/**
	 * Display some usage help.
	 * 
	 */
	private static void usage() {
		String className = SensorDaemon.class.getName();
		System.err.println("java " + className + "\n" + "    [-debug]\n"
				+ "    [-baudrate n]\n" + "    [-version] " + " siodevpath");
		System.err.println("example: java " + className
				+ " -debug -baudrate 19200" + " /dev/ttyS0");
	}

	public SensorDaemon(String sioDeviceName, int speed) throws IOException {

		this.sioDevName = sioDeviceName;
		this.baudrate = speed;

		setName("SIOReader-" + this.sioDevName);
	}

	public void run() {

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
			System.err.println("scanning for ports...");
			scanPorts();
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
		// Start socket listener (will continue to echo raw sensor readings as they come in)
		//
		BroadcastServerListener socketListener = new BroadcastServerListener();
		Thread t = new Thread(socketListener);
		t.start();

		//
		// Start socket listener2 (will dump current values and close socket)
		//
		CurrentValuesListener socketListener2 = new CurrentValuesListener();
		Thread t2 = new Thread(socketListener2);
		t2.start();
		
		//
		// Start Efergy Elite monitor thread
		//
		try {
			EliteDaemon ed = new EliteDaemon("/dev/ttyUSB1", 19200,
					socketListener,
					socketListener2);
			ed.start();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
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
			int i, h, l, v, ts;
			float f;

			while ((line = lnr.readLine()) != null) {

				// For completeness will relay all packets including those that
				// fail the checksum test
				ts = (int) (System.currentTimeMillis() / 1000L);
				String record = ts + "\t" + line;
				System.out.println(record);

				// Parse record
				String[] p = line.split("\\s+");

				// Do not transmit bad packets.
				if (line.endsWith("X")) {
					continue;
				}

				// Send [TODO]
				// System.err.print ("*"); System.err.flush();
				socketListener.broadcastPacket(record);

				try {
					if ("53".equals(p[0])) {
						for (i = 0; i < 7; i++) {
							h = Integer.parseInt(p[4 + i * 2], 16);
							l = Integer.parseInt(p[5 + i * 2], 16);
							v = (h * 256 + l);
							if (v > 0x7fff) {
								v -= 0x10000;
							}
							f = (float) v / 16f;
							synchronized (lastReadingMap) {
								switch (i) {
								case 0:
									lastReadingMap.put("frontdoor",
											new SensorReading(ts, "frontdoor",
													"T", f));
									break;
								case 3:
									lastReadingMap.put("kitchen",
											new SensorReading(ts, "kitchen",
													"T", f));
									break;
								case 4:
									lastReadingMap.put("sittingroom",
											new SensorReading(ts,
													"sittingroom", "T", f));
									break;
								case 6:
									lastReadingMap.put("outdoors",
											new SensorReading(ts, "outdoors",
													"T", f));
									break;
								}
							}
						}

						h = Integer.parseInt(p[18], 16);
						l = Integer.parseInt(p[19], 16);
						v = (h * 256 + l) >> 6;
						synchronized (lastReadingMap) {
							lastReadingMap.put("battery1", new SensorReading(
									ts, "battery1", "B", (float) v));
						}
					}

					if ("54".equals(p[0])) {
						h = Integer.parseInt(p[4], 16);
						l = Integer.parseInt(p[5], 16);
						v = (h * 256 + l);
						if (v > 0x7fff) {
							v -= 0x10000;
						}
						f = (float) v / 16f;
						synchronized (lastReadingMap) {
							lastReadingMap.put("attic", new SensorReading(ts,
									"attic", "T", f));
						}

						h = Integer.parseInt(p[6], 16);
						l = Integer.parseInt(p[7], 16);
						v = (h * 256 + l) >> 6;
						synchronized (lastReadingMap) {
							lastReadingMap.put("battery2", new SensorReading(
									ts, "battery2", "B", (float) v));
						}
					}
				} catch (Exception ee) {
					ee.printStackTrace();
				}

			}

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		sioPort.removeEventListener();
		sioPort.close();

		System.err.println(" *** MAIN THREAD TERMINATED ***");

	}

	public void serialEvent(SerialPortEvent ev) {

		if (this.debugFlag) {
			System.err.println("Received serialport event: " + ev);
		}
	}

	private static void scanPorts() {

		Enumeration<CommPortIdentifier> ports = CommPortIdentifier
				.getPortIdentifiers();
		while (ports.hasMoreElements()) {
			CommPortIdentifier cpi = ports.nextElement();
			if (cpi.getPortType() == CommPortIdentifier.PORT_SERIAL) {
				System.err.println("  found port " + cpi.getName());
			}
		}
	}

	public void setDebug(boolean b) {
		this.debugFlag = b;
	}

	public boolean isDebug() {
		return this.debugFlag;
	}

}
