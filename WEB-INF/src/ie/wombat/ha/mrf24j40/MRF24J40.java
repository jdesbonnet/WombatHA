package ie.wombat.ha.mrf24j40;

import gnu.io.CommPortIdentifier;
import gnu.io.PortInUseException;
import gnu.io.SerialPort;
import gnu.io.SerialPortEvent;
import gnu.io.SerialPortEventListener;
import gnu.io.UnsupportedCommOperationException;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PipedOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.TooManyListenersException;

/**
 * Run with params -baudrate 38400 /dev/ttyUSB2
 * @author joe
 *
 */
public class MRF24J40 implements SerialPortEventListener, InterruptHandler {
	private static final String VERSION = "0.1";
	private static final int DEFAULT_BPS = 38400;
	
	public static final byte[] MAC_ADDRESS64 = {
		0x00, 0x13, 0x7A, 0x00, 
		0x00, 0x00, 0x34,(byte)0xDF
	};
	
	// "ZigBeeAlliance09"
	public static final byte[] KEY = {
		0x5A, 0x69, 0x67, 0x42,
		0x65, 0x65, 0x41, 0x6C,
		0x6C, 0x69, 0x61, 0x6E,
		0x63, 0x65, 0x30, 0x39
	};

	public static final byte MRF_RXMCR = 0x00;
	public static final byte MRF_PANIDL = 0x01;
	public static final byte MRF_PANIDH = 0x02;
	public static final byte MRF_SADRL = 0x03;
	public static final byte MRF_SADRH = 0x04;
	public static final byte MRF_ORDER = 0x10;
	public static final byte MRF_PACON0 = 0x16;
	public static final byte MRF_PACON1 = 0x17;
	public static final byte MRF_PACON2 = 0x18;
	public static final byte MRF_TXNCON = 0x1B;

	public static final byte MRF_TXSTAT = 0x24; 
	
	public static final byte MRF_SECCON0 = 0x2C;
	public static final byte MRF_SECCON1 = 0x2D;
	
	
	public static final byte MRF_TXSTBL = 0x2E;

	public static final byte MRF_SOFTRST = 0x2A;
	public static final byte MRF_RXSR = 0x30; 
	public static final byte MRF_INTSTAT = 0x31; // Interrupt status
	public static final byte MRF_INTCON = 0x32;
	public static final byte MRF_RFCTL = 0x36;
	public static final byte MRF_SECCR2 = 0x37;
	public static final byte MRF_BBREG1 = 0x39; // Only one bit: bit2: RXDECINV
	public static final byte MRF_BBREG2 = 0x3A;
	public static final byte MRF_BBREG6 = 0x3E;
	public static final byte MRF_CCAEDTH = 0x3F;

	//
	// Long register addresses
	//
	public static final int MRF_L_TXFIFO = 0x000; // Base address of TX FIFO
	public static final int MRF_L_RXFIFO = 0x300; // Base address of RX FIFO
	public static final int MRF_L_UPNONCE = 0x240; // Base of UPNONCE (13 bytes)
	public static final int MRF_L_TXKEY = 0x280; // Base of TX security key
	public static final int MRF_L_RXKEY = 0x2B0; // Base of RX security key
	

	public static final int MRF_L_RFCON0 = 0x200;
	public static final int MRF_L_RFCON1 = 0x201;
	public static final int MRF_L_RFCON2 = 0x202;
	public static final int MRF_L_RFCON3 = 0x203;

	public static final int MRF_L_RFCON5 = 0x205;
	public static final int MRF_L_RFCON6 = 0x206;
	public static final int MRF_L_RFCON7 = 0x207;
	public static final int MRF_L_RFCON8 = 0x208;

	public static final int MRF_L_RFSTATE = 0x20F;

	public static final int MRF_L_RSSI = 0x210;
	public static final int MRF_L_SLPCON0 = 0x211;
	public static final int MRF_L_SLPCON1 = 0x220;

	public static final int MRF_L_ASSOSADR0 = 0x238;

	// MRF24J40 Data sheet, Figure 3-3, page 95 and Table 3-8, page 96.
	// RSSI dBm = rssiDbmLookup[rssi] * -1 
	public static final byte[] rssiDbmLookup = { 90, 89, 88, 88, 88, 87, 87, 87,
			87, 86, 86, 86, 86, 85, 85, 85, 85, 85, 84, 84, 84, 84, 84, 83, 83,
			83, 83, 82, 82, 82, 82, 82, 81, 81, 81, 81, 81, 80, 80, 80, 80, 80,
			80, 79, 79, 79, 79, 79, 78, 78, 78, 78, 78, 77, 77, 77, 77, 77, 76,
			76, 76, 76, 76, 75, 75, 75, 75, 75, 74, 74, 74, 74, 74, 73, 73, 73,
			73, 73, 72, 72, 72, 72, 72, 71, 71, 71, 71, 71, 71, 70, 70, 70, 70,
			70, 70, 69, 69, 69, 69, 69, 68, 68, 68, 68, 68, 68, 68, 67, 67, 67,
			67, 66, 66, 66, 66, 66, 66, 65, 65, 65, 65, 64, 64, 64, 64, 63, 63,
			63, 63, 62, 62, 62, 62, 61, 61, 61, 61, 61, 60, 60, 60, 60, 60, 59,
			59, 59, 59, 59, 58, 58, 58, 58, 58, 57, 57, 57, 57, 57, 57, 56, 56,
			56, 56, 56, 56, 55, 55, 55, 55, 55, 54, 54, 54, 54, 54, 54, 53, 53,
			53, 53, 53, 53, 53, 52, 52, 52, 52, 52, 51, 51, 51, 51, 51, 50, 50,
			50, 50, 50, 49, 49, 49, 49, 49, 48, 48, 48, 48, 47, 47, 47, 47, 47,
			46, 46, 46, 46, 45, 45, 45, 45, 45, 44, 44, 44, 44, 43, 43, 43, 42,
			42, 42, 42, 42, 41, 41, 41, 41, 41, 41, 40, 40, 40, 40, 40, 40, 39,
			39, 39, 39, 39, 38, 38, 38, 37, 36, 35 };

	private SerialPort sioPort;
	private SPIDriver driver;
	private int channel;

	private String sioDevName;

	public static void main(String arg[]) throws Exception {

		List<String> argList = new ArrayList<String>(arg.length);

		boolean debugFlag = false;
		int baudRate = DEFAULT_BPS;
		int channel = 12;

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

			if ("-channel".equals(arg[i])) {
				try {
					i++;
					channel = Integer.parseInt(arg[i]);
				} catch (NumberFormatException e) {
					System.err.println("channel must be an integer");
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

		// TODO: should I hand it the opened streams instead?
		MRF24J40 me = new MRF24J40(sioDeviceName, baudRate, channel);
	
		
		try { 
			me.run();
		} catch (SPIException e) {
			e.printStackTrace();
		}
		//me.close();
		
		System.err.println ("Exiting.");
		System.exit(0);
	
	}

	public MRF24J40(String sioDevName, int baudRate, int channel) {

		this.sioDevName = sioDevName;
		this.channel = channel;

		sioPort = null;


		//System.err.println ("DONE");
		//System.exit(0);
	
		// 25 Sep 2011: This following line causes Java VM to fail with
		// stack trace with __fortify_fail symbol at the top under 
		// certain conditions. Eg with FTDI mappings to /dev/tty50,51
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
		System.err.println("bps=" + baudRate);

		try {
			sioPort.setSerialPortParams(baudRate, SerialPort.DATABITS_8,
					SerialPort.STOPBITS_1, SerialPort.PARITY_NONE);
			// sioPort.setFlowControlMode(SerialPort.FLOWCONTROL_RTSCTS_OUT);
			sioPort.setFlowControlMode(SerialPort.FLOWCONTROL_NONE);
			sioPort.addEventListener(this);
			// sioPort.notifyOnDataAvailable(true);

			sioPort.enableReceiveTimeout(5000);

			this.driver = new SPIDriverImpl2(sioPort);
			this.driver.registerInterruptHandler(this);

		} catch (UnsupportedCommOperationException e) {
			System.err.println("error: " + e);
			return;
		} catch (TooManyListenersException e) {
			System.err.println("error: " + e);
			return;
		} catch (IOException e) {
			System.err.println("error: " + e);
			return;
		}
		
		
		// Driver hardware reset
		driver.resetDriver();
		
		try {
			Thread.sleep(3000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		
		// Test driver
		System.err.print ("Testing SPI driver hardware...");
		try {
			String v = this.driver.getVersion();
			if (! "51".equals(v)) {
				System.err.println ("ERROR: unknown driver version " + v);
			} else {
				System.err.println ("HWDRIVER version " + v);
			}
		} catch (Exception e) {
			System.err.println ("ERROR: unable to communicate to SPI driver hardware");
		}

	}

	public void run() throws Exception {

		// System.err.println ("Version: ");
		// System.err.println (driver.getVersion());
		// Thread.sleep(5000);
		// getSPIDriver().relay();

		// Show all short address registers
		int i, j, v;
		
		// Reset driver hardware
		//System.err.println ("Reset SPI driver");
		//driver.reset();
		
		// Reset MRF24J40
		//System.err.println ("Reset SPI driver");
		//driver.resetDriver();
		
		System.err.println ("Reset MR24J40");
		reset();
	

		// scanChannels();

		driver.setDelay(10);
		
		// Test packet
		byte[] packet = {
				// xxxx x001 : Frame Type = Command
				// xxxx 0xxx : Security Enabled = false
				// xxx0 xxxx : Frame Pending = false
				// xx0x xxxx : Ack Request = false
				// x0xx xxxx : IntraPAN = false
				// 0xxx xxxx : Reserved
				0x01,
				// xxxx xx00 : Reserved
				// xxxx 10xx : Dest addr mode = Short(16bit)
				// xx00 xxxx : Reserved
				// 00xx xxxx : Src addr mode = PAN identifier and address field
				// are not present.
				0x08,
				// Sequence number (?)
				(byte) 0xC4,
				// Destination PAN
				(byte) 0xFF, (byte) 0xFF,
				// Destination Address
				(byte) 0xFF, (byte) 0xFF, 0x07, 0x00, 0x01, 0x02, 0x03, 0x04 };

		byte[] packet2 = { 0x01, 0x08, 0x44, (byte) 0xFF, (byte) 0xFF, 0x00,
				0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09 };
		
		// Attempting to send command to Z-800 with encryption off.
		byte[] packet3 = {
				0x61, (byte)0x88, // MAC FC 
				(byte)0x92, // Sequence
				0x10, 0x52, // PAN ID 
				// 0x51, 0x6c, // 802.15.4 Destination 
				0x78, 0x56, // 802.15.4 Destination 
				0x34, 0x12, // 802.15.4 Source
				0x09, 0x10, // NWK FC  
				0x51, 0x6c, // ZigBee Destination
				0x34, 0x12, // ZigBee Source
				0x1e, 0x4f,
				// APS
				0x40,   0x0a,    0x06, 0x00,    0x04, 0x01,    0x0a, (byte)0xf3,     0x11, 0x00, 0x01
				
		};
		
		

		//int channel = 12;
		System.err.println ("Set channel to " + channel);
		setChannel(channel);
		
		
		
		//sendEncryptedPacket(packet3, 17, KEY);
	
		//driver.reset();
		System.err.println ("Set promiscuous mode...");
		// bit 0 PROMI = 1 (enable promiscuous mode)
		shortWrite (MRF_RXMCR, 0x01);
		
		//driver.interruptEnable(true);
		//driver.setRelayMode(true);
	
		
	
		System.err.println ("Enabling interrupt...");
		driver.interruptEnable(true);
		
		System.err.println ("Listening...");
		driver.setRelayMode(true);

		OutputStream pcapOut = System.out;
		DataOutputStream pcapData = new DataOutputStream(pcapOut);
	
		
		// Write PCAP header
		
		//fwrite(&PCAP_MAGIC, sizeof(int), 1, stdout);    
		pcapData.writeInt(PCAP.PCAP_MAGIC);
		
		//fwrite(&PCAP_VERSION_MAJOR, sizeof(short), 1, stdout);
		pcapData.writeShort(PCAP.PCAP_VERSION_MAJOR);
		
		//fwrite(&PCAP_VERSION_MINOR, sizeof(short), 1, stdout);
		pcapData.writeShort(PCAP.PCAP_VERSION_MINOR);
		
		//fwrite(&PCAP_TZ, sizeof(int), 1, stdout);				// thiszone: GMT to local correction
		pcapData.writeInt(PCAP.PCAP_TZ);

		//fwrite(&PCAP_SIGFIGS, sizeof(int), 1, stdout);			// sigfigs: accuracy of timestamps
		pcapData.writeInt(PCAP.PCAP_SIGFIGS);

		//fwrite(&PCAP_SNAPLEN, sizeof(int), 1, stdout);			// snaplen: max len of packets, in octets
		pcapData.writeInt(PCAP.PCAP_SNAPLEN);

		//fwrite(&PCAP_LINKTYPE, sizeof(int), 1, stdout);		// data link type
		pcapData.writeInt(PCAP.PCAP_LINKTYPE);
		
		pcapData.flush();
		
		int len, lqi, rssi;

		

		
		
/*
		while (true) {
			System.err.print ("waiting ");
			driver.waitForInterrupt();
			System.err.print (" ! ");
			while ( (shortRead(MRF_INTSTAT) & 0x08) != 0) {
				
				long t = -System.currentTimeMillis();
				
				// Read MRF24J40 frame. There is potential for confusion here: The 
				// MRF24J40 does not return the 16 FCS bits. However in its place
				// is packet metadata (lqi etc). So the full 802.15.4 frame has the
				// same length as the MRF24J40 frame, but the last two bytes are
				// used for a different purpose.
				byte[] buf = driver.readRxFifo();
				len = buf.length;
				lqi = buf[len-2];
				rssi = buf[len-1];				
				t += System.currentTimeMillis();
				
				//for (i = 0; i < len-2; i++) {
					//System.err.print(formatHexByte(buf[i]) + " ");
				//}
				//System.err.println(" len=" + len + " lqi=" + lqi + " rssi=" + rssi + " t=" + t);
				
				t = System.currentTimeMillis();
				
				// ts_sec: timestamp seconds
				pcapData.writeInt((int)(t/1000));
				
				// ts_usec: timestamp microseconds
				pcapData.writeInt((int)(t%1000) * 1000);
				
				// 5 Dec 2011: comparing ZENA with this, it seems there are 4 extra bytes
				// being sent to PCAP vs the ZENA dump. Try reducing the size by 4 bytes:
				
				//pcapData.writeInt(len-2);  // Size captured (exclude two FCS bytes)
				//pcapData.writeInt(len);  // Original size of packet (same as MRF24J40 frame length)
				//pcapData.write(buf, 0, len-2);
				
				pcapData.writeInt(len-6);  // Size captured (exclude two FCS bytes)
				pcapData.writeInt(len-4);  // Original size of packet (same as MRF24J40 frame length)
				pcapData.write(buf, 0, len-6);
				
				
				pcapData.flush();
			}
		}
*/
		//pcapData.close();
		
		//return;
		
	}

	public void close() {
		driver.close();
	}

	public SPIDriver getSPIDriver() {
		return this.driver;
	}

	/**
	 * 1 A9 A8 A7 A6 A5 A4 A3 A2 A1 A0 RW=0 X X X X D7 .. D0
	 * 
	 * @param address
	 * @return
	 */
	public int longRead(int address) {
		//driver.chipSelect();
		/*
		driver.spiPut((address >> 3) | 0x80);
		driver.spiPut(((address << 5) & 0xe0));
		int v = driver.spiGet();
		*/
		//int v = driver.csPut16Get8 ((address >> 3) | 0x80, ((address << 5) & 0xe0));
		// This command automatically asserts CS
		//int addr0 = (address >> 3) | 0x80;
		//int addr1 = (address << 5) & 0xe0;
		int v = driver.csPut16Get8 (address>>8, address & 0xff);
		//int v = driver.csPut16Get8 (addr0, addr1);
		//driver.chipUnselect();
		return v;
	}

	/**
	 * Short read 0aaaaaa0 vvvvvvvv
	 * 
	 * @param address
	 * @return
	 */
	public int shortRead(int address) throws MRFException {
		try {
			driver.chipSelect();
			driver.spiPut((byte) (address << 1));
			int v = driver.spiGet();
			return v;
			//return driver.spiCSPutGet(address << 1);
		} catch (SPIException e) {
			e.printStackTrace();
			throw new MRFException(e.getMessage());
		} finally {
			driver.chipUnselect();
		}
	}

	/*
	public int shortRead(int address) throws MRFException {
		return shortRead((byte) address);
	}
	*/

	/**
	 * 1 A9 A8 A7 A6 A5 A4 A3 A2 A1 A0 RW=1 X X X X D7 .. D0
	 * 
	 * @param address
	 * @return
	 */
	public void longWrite(int address, byte value) {
		driver.chipSelect();
		driver.spiPut((address >> 3) | 0x80);
		driver.spiPut(((address << 5) & 0xe0) | 0x10);
		driver.spiPut(value);
		driver.chipUnselect();
	}

	public void longWrite(int address, int value) {
		longWrite(address, (byte) value);
	}

	/**
	 * Short write 0aaaaaa1 vvvvvvvv
	 * 
	 * @param address
	 * @param value
	 */
	public void shortWrite(byte address, int value) {
		driver.chipSelect();
		driver.spiPut((address << 1) | 0x01);
		// delay();
		driver.spiPut(value);
		// delay();
		driver.chipUnselect();
	}

	private void delay() {
		try {
			Thread.sleep(30);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private static void usage() {

	}

	public void serialEvent(SerialPortEvent arg0) {
		System.err.println("*** SIO_EVENT:" + arg0);
	}

	/**
	 * Reset and initialize MRF24J40. Data sheet section 3.2, page 90.
	 * 
	 * @throws MRFException
	 */
	public void reset() throws MRFException {
		int v,i;

		
	
		
		// Hard reset
		//System.err.println ("  assert RESET to MRF20J40");
		//driver.assertReset();
		//delay();

		
		// Soft reset of MRF24J40. Ref datasheet section 3.1, page 89.
		System.err.println ("  soft reset of MRF24J40");
		// bit 2 RSTPWR: Power Management Reset bit
		// bit 1 RSTBB: Baseband Reset bit
		// bit 0 RSTMAC: MAC Reset bit
		shortWrite(MRF_SOFTRST, (byte) 0x07);  // 0x55,0x07 on the wire

		// Wait for soft reset to complete
		i = 0;
		do {
			v = shortRead(MRF_SOFTRST);  // 0x54, (0x00) on the wire
			System.err.println("reset(): MRF_SOFTRST register=" + v);
			if (++i > 8) {
				throw new MRFException ("SOFTRST taking too long");
			}
		} while ((v & 0x07) != 0);

		System.err.println ("SOFTRST OK");
		delay();
		
		// MRF_ORDER = 0x10
		System.err.println ("Testing for MRF24J40. Reading MRF_ORDER.");
		v = shortRead(MRF_ORDER);  // 0x20, (0xFF) on the wire
		System.err.println ("MRF_ORDER=0x" + Integer.toHexString(v));
		if (v != 0xff) {
			driver.chipUnselect(); // TODO: is this unselect necessary?
			throw new MRFException("MRF24J40 not found. Expecting MRF_ORDER=0xFF, but got 0x" 
					+ Integer.toHexString(v));
		}

		//
		// Initialize MRF24J40 according to datasheet section 3.2, page 90.
		//
		
		// MRF_PACON2=0x18, expect 0x31, 0x98 on the wire
		// PACON2 = 0x98, Initialize FIFOEN=1 and TXONTS = 0x6
		// 1xxx xxxx FIFOEN FIFO enable
		// xx01 10xx TXONTS<3:0> TX on time before transmitting. Units: symbol
		// period (16μs).
		// xxxx xx00 TXONT<8:7> ?
		System.err.println ("PACON2=0x98");
		shortWrite(MRF_PACON2, 0x98);
		// read back
		v = shortRead(MRF_PACON2);
		if ( v != 0x98) {
			System.err.println("Warning: writing to PACON2, readback: 0x" + Integer.toHexString(v));
			//throw new MRFException("Error writing to PACON2, readback: 0x" + Integer.toHexString(v));
		}

		// TXSTBL: TX STABILIZATION REGISTER (Data sheet, page 51)
		// 1001 xxxx RFSTBL<3:0>: VCO Stabilization Period bits. Units: symbol
		// period (16μs).
		// Default value: 0x7. Recommended value: 0x9.
		// xxxx 0101 MSIFS<3:0>: Minimum Short Interframe Spacing bits
		// Default value: 0x5.
		System.err.println ("TXSTBL=0x95");
		shortWrite(MRF_TXSTBL, 0x95);
		v = shortRead(MRF_TXSTBL);
		if ( v != 0x95) {
			System.err.println("Warning: writing to TXSTBL, readback: 0x" + Integer.toHexString(v));
			//throw new MRFException("Error writing to PACON2, readback: 0x" + Integer.toHexString(v));
		}
		
		// Initialize RFOPT=3
		System.err.println ("RFCON0=0x03");
		longWrite(MRF_L_RFCON0, 0x03);
		v = longRead(MRF_L_RFCON0);
		if ( v != 0x03) {
			System.err.println("Warning: writing to L_RFCON0, readback: 0x" + Integer.toHexString(v));
			//throw new MRFException("Error writing to PACON2, readback: 0x" + Integer.toHexString(v));
		}

		// Initialize VCOOPT=1
		System.err.println ("RFCON1=0x01");
		longWrite(MRF_L_RFCON1, 0x01);

		// Enable PLL (PLLEN=1)
		System.err.println ("RFCON2=0x80");
		longWrite(MRF_L_RFCON2, 0x80);

		// Initialize TXFIL = 1 and 20MRECVR = 1.
		System.err.println ("RFCON6=0x90");
		longWrite(MRF_L_RFCON6, 0x90);

		// Item #8: Initialize SLPCLKSEL = 0x2 (100 kHz Internal oscillator).
		// SLPCLKSEL<1:0>: Sleep Clock Selection bits
		// 10 = 100 kHz internal oscillator
		// 01 = 32 kHz external crystal oscillator
		System.err.println ("RFCON7=0x80");
		longWrite(MRF_L_RFCON7, 0x80);

		// Item #9: Initialize RFVCO = 1
		System.err.println ("RFCON8=0x10");
		longWrite(MRF_L_RFCON8, 0x10);

		// Item #10: Initialize CLKOUTEN = 1 and SLPCLKDIV = 0x01.
		System.err.println ("SLPCON1=0x21");
		longWrite(MRF_L_SLPCON1, 0x21);

		//
		// BBREG2 (0x3A) = 0x80 – Set CCA mode to ED.
		// shortWrite(MRF_BBREG2,0x95);

		// MRF_BBREG2=0x3A, expect 0x6
		// Set CCA mode to ED
		// bit 7-6 CCAMODE<1:0>: Clear Channel Assessment (CCA) Mode bits
		// 01 = CCA Mode 2: Carrier sense only. CCA shall report a busy medium
		// only upon the detection of a
		// signal with the modulation and spreading characteristics of IEEE
		// 802.15.4. This signal may be
		// above or below the Energy Detection (ED) threshold (default).
		System.err.println ("BBREG2=0x80");
		shortWrite(MRF_BBREG2, 0x80);

		// Set CCA-ED Threshold
		// bit 7-0 CCAEDTH<7:0>: Clear Channel Assessment (CCA) Energy Detection
		// (ED) Mode bits
		// If the in-band signal strength is greater than the threshold, the
		// channel is busy. The 8-bit value can be
		// mapped to a power level according to RSSI. Refer to Section 3.6
		// Received Signal Strength Indicator
		// (RSSI)/Energy Detection (ED).
		// Default value: 0x00. Recommended value: 0x60 (approximately -69 dBm).
		System.err.println ("CCAEDTH=0x60");
		shortWrite(MRF_CCAEDTH, 0x60);

		// Set appended RSSI value to RX FIFO
		System.err.println ("BBREG6=0x40");
		shortWrite(MRF_BBREG6, 0x40);

		// Set interrupt control register 
		// INTCON (0x32) = 0xF6 = 0b11110110, enable TXNIE and RXIE interrupts
		// Clearing bit to 0 enables an interrupt. All disabled by default.
		// bit 7: SLPIE: Sleep Alert Interrupt Enable bit
		// bit 6: WAKEIE: Wake-up Alert Interrupt Enable bit
		// HSYMTMRIE: Half Symbol Timer Interrupt Enable bit
		// SECIE: Security Key Request Interrupt Enable bit
		// bit 3: RXIE: RX FIFO Reception Interrupt Enable bit
		// bit 2: TXG2IE: TX GTS2 FIFO Transmission Interrupt Enable bit
		// bit 1: TXG1IE: TX GTS1 FIFO Transmission Interrupt Enable bit
		// bit 0: TXNIE: TX Normal FIFO Transmission Interrupt Enable bit
		System.err.println ("INTCON=0xF6");
		shortWrite(MRF_INTCON, 0xF6);

		
		// INTEDGE=1 : want interrupts on low->high transition
		// Ref datasheet section 3.3, page 91.
		longWrite(MRF_L_SLPCON0,0x02);
		
		System.err.println ("set channel 11");
		setChannel(11);

		// Wait until the RFSTATE machine indicates RX state
		// bit 7-5 RFSTATE<2:0>: RF State Machine bits
		// 111 = RTSEL2
		// 110 = RTSEL1
		// 101 = RX
		// 100 = TX
		// 011 = CALVCO
		// 010 = SLEEP
		// 001 = CALFIL
		// 000 = RESET

		System.err.println ("Reading L_RFSTATE");
		while ((longRead(MRF_L_RFSTATE) & 0xA0) != 0xA0) {
			System.err
					.println("... waiting for RF state machine to indicate RX state "
							+ Integer.toHexString(longRead(MRF_L_RFSTATE)));
		}

		// Program the short MAC Address to 0xffff
		shortWrite(MRF_SADRL, 0xFF);
		shortWrite(MRF_SADRH, 0xFF);

		// load the short address of the device with 0xffff which means that it
		// will be ignored upon receipt
		shortWrite(MRF_PANIDL, 0xFF);
		shortWrite(MRF_PANIDH, 0xFF);
		// load the pan address also with 0xffff;
		

		checkBatteryLevel();
		//scanChannels();

	}

	public void checkBatteryLevel () throws MRFException {
		int i,v;
		
		// Check power/battery voltage. Ref RFCON5, RFCON6 (2.63, page 65)
		v = longRead(MRF_L_RFCON6);
		v |= 0x08; // BATEN (RFCON6<3>) = 1
		longWrite(MRF_L_RFCON6,v);
		
		String batv[] = {"2.5","2.6","2.7","2.8","3.1","3.2","3.3","3.5"};
		for (i = 7; i < 15; i++) {
			longWrite(MRF_L_RFCON5, i << 4);
			v = shortRead (MRF_RXSR);
			if ( (v & 0x20) != 0) {
				System.err.println ("Power supply/battery: " + batv[i-7] + "V");
				break;
			}
		}
	}
	
	/**
	 * Ref datasheet section §3.5, p93. In particular §3.5.1 (CCA Mode 1: Energy Above Threshold). 
	 * @throws MRFException
	 */
	public void spectrumScan ()  throws MRFException {
		
		int i,j,v;
		for (i = 11; i < 26; i++) {
			setChannel(i);
			
			// set CCAMODE bbreg2 0x3A<7:6> = 10
			/*
			v = shortRead (MRF_BBREG2);
			v |= 0x80;
			v &= ~(0x40);
			shortWrite (MRF_BBREG2, v);
			*/	
		}
	}
	/**
	 * See MRF24J40 data sheet section 3.4 "Channel Selection", page 92. A delay
	 * of 192µs after calling this is required to allow RF circuitry to
	 * calibrate.
	 * 
	 * @param channel
	 *            11 .. 26
	 */

	public void setChannel(int channel) {

		// RFCON0 bit 7-4 CHANNEL<3:0>: Channel Number bits
		// 0000 = Channel 11 (2405 MHz) (default)
		// 0001 = Channel 12 (2410 MHz)
		// 0010 = Channel 13 (2415 MHz)
		// ..
		// 1111 = Channel 26 (2480 MHz)
		//
		// RFCON0 bit 3-0 RFOPT<3:0>: RF Optimize Control bits
		// Default value: 0x0. Recommended value: 0x3.
		// What is rfopts about??

		int v=(((channel - 11) << 4) | 0x03);
		debug ("RFCON0=0xF6 (set channel " + channel +")");
		longWrite(MRF_L_RFCON0, v);

		delay();

		// RFCTL bit 2 is RFRST. Reset RF State machine.
		debug ("RFCTL=0x04 (assert bit 2 RFRST)");
		shortWrite(MRF_RFCTL, 0x04);
		delay();
		debug ("RFCTL=0x00");
		shortWrite(MRF_RFCTL, 0x00);
		delay();
		// delay of 192µs required to allow RF circuitry to calibrate.

	}

	/**
	 * Scan through all channels and report RSSI. Verified to work.
	 * 
	 * @throws MRFException
	 */
	public void scanChannels() throws MRFException {
		int i, j, rssi, max_rssi = 0;
		for (i = 11; i <= 26; i++) {

			System.err.print("Channel #" + i + ": ");
			System.err.flush();

			setChannel(i);
			delay();

			max_rssi = 0;
			for (j = 0; j < 132; j++) {

				// BBREG6 bit 0: RSSIRDY RSSI value ready
				// bit 7: RSSIMODE1 Initiate RSSI calculation, cleared
				// automatically
				// bit 0: RSSIRDY: RSSI Ready Signal for RSSIMODE1
				shortWrite(MRF_BBREG6, 0x80);

				// Wait for RSSI to become available
				while ((shortRead(MRF_BBREG6) & 0x01) == 0) {
					System.err.println("...waiting... BBREG6="
							+ shortRead(MRF_BBREG6));
				}

				rssi = longRead(MRF_L_RSSI);

				if (rssi > max_rssi) {
					max_rssi = rssi;
				}
				delay();
			}
			// convert to rssi dB units

			System.err.println(" rssi= -" + rssiDbmLookup[max_rssi] + " dB");
		}
	}
	
	/**
	 * Set RX and TX encryption key
	 * @param key
	 */
	public void setSecurityKey (byte[] key) throws MRFException {
		if (key.length != 16) {
			throw new MRFException ("key must be 16 bytes");
		}
		for (int i = 0; i < 16; i++) {
			longWrite(MRF_L_TXKEY+i,key[i]);
			longWrite(MRF_L_RXKEY+i,key[i]);
		}
	}

	/**
	 * Send packet with upper layer encrypted.
	 * See MRF24J40 data sheet, section 3.17.3, page 131.
	 * 
	 * Also see ZigBee standard: Annex A CCM* Mode of Operation
	 * 
	 * Z-202 MAC: 00137A00000034DF
	 * NONCE = Source Address (8 bytes) 
	 * + Frame Counter (4 bytes) 
	 * + Security Control (1 byte)
	 * 
	 * 
	 * @param packet
	 * @param headerLen: the header part of the packet that is not encrypted. This is limited to 31.
	 * @param key 16 byte encryption key
	 * 
	 */
	public void sendEncryptedPacket(byte[] packet, int headerLen, byte[] key) throws MRFException {

		// Header is implemented as a 5 bit field. See note on DS page 131.
		if (headerLen > 31) {
			throw new MRFException("Header length too long. Max is 31. Received " + headerLen);
		}
		
	
		// Reference MRF24J40 Data sheet, section 3.12, page 111
		// fifo[0] = header length (not encrypted)
		// fifo[1] = frame length (header + data payload)
		
		longWrite(MRF_L_TXFIFO + 0x000, headerLen); // header length
		longWrite(MRF_L_TXFIFO + 0x001, packet.length); // length of the full
														// packet

		int i;
		// Load packet into TXFIFO
		long t = -System.currentTimeMillis();
		for (i = 0; i < packet.length; i++) {
			longWrite(MRF_L_TXFIFO + i + 2, packet[i]);
		}
		t+=System.currentTimeMillis();
		System.err.println ("Load t=" + t);

		//
		// Item #2: NONCE? Source address (8 bytes) + frame counter (4 bytes)
		// + security header (1 byte)
		//
		// Source address (64 bit)
		for (i = 0; i < 8; i++) {
			longWrite (MRF_L_UPNONCE + i, MAC_ADDRESS64[7-i]);
		}
		// Frame counter
		for (i = 8; i < 12; i++) {
			longWrite (MRF_L_UPNONCE + i, 0);
		}
		longWrite (MRF_L_UPNONCE+12, 0x40);
		
		// Item #3: Program the 128-bit security key
		setSecurityKey(key);
		
		// Item #4: Select the security suite. 
		// Set TXNCIPHER = 0b100 (AES-CCM-32)
		shortWrite(MRF_SECCON0, 0x40);

		// Item #5: Enable upper layer security encryption mode
		// Set UPENC (SECCR2 0x37<6>) bit = 1.
		i = shortRead(MRF_SECCR2);
		shortWrite (MRF_SECCR2, i|0x40);
		
		// Item #6: Encrypt the frame by setting the TXNTRIG
		// (TXNCON 0x1B<0>) bit and TXNSECEN
		// (TXNCON 0x1B<1>) to 1.
		shortWrite(MRF_TXNCON, 0x03);
	
		
		// Wait for INTSTAT bit TXNIF = 1
		while ( (shortRead(MRF_INTSTAT)&0x01) == 0) {
			System.err.print (".");
		}
		System.err.println ("Sent.");
		
		// Check TXSTAT bit 0 TXNSTAT for 0 (success) or 1 (failure)
		i = shortRead(MRF_TXSTAT);
		if ((i&0x01)==0) {
			System.err.println ("Success.");
		} else {
			// bit 7,6: TXNRETRY
			System.err.print ("Failure. nretries=" + (i>>6));
			// bit 5: CCAFAIL: 1 indicates failure due to channel busy
			if ((i & 0x20) != 0) {
				System.err.println ("Channel busy.");
			}
		}
		
		// Read back encrypted packet
		System.err.print("SENT: ");
		int encPacketLen = longRead(MRF_L_TXFIFO + 1);
		
		for (i = 0; i < encPacketLen; i++) {
			System.err.print(formatHexByte(longRead(MRF_L_TXFIFO+2+i)) + " ");
		}
		System.err.println("");

	}
	
	/**
	 * Return byte as zero padded (two hex digit) hex number. 
	 * @param v
	 * @return
	 */
	private static String formatHexByte (int v) {
		v &= 0xff;
		if (v < 16) {
			return "0" + Integer.toHexString(v);
		}
		return Integer.toHexString(v);
	}
	/**
	 * Reference MRF24J40 data sheet, section 3.11, page 107.
	 * 
	 * @throws MRFException
	 */
	public void receivePacket () throws MRFException, IOException {
		int i, j;
		
		// bit 0 PROMI = 1 (enable promiscuous mode)
		shortWrite (MRF_RXMCR, 0x01);
		
		
		// Without interrupts we need to fast poll MRF_INTSTAT looking for
		// bit RXIF = 1
		while ( (i=shortRead(MRF_INTSTAT) & 0x08) == 0) {
			delay();
		}

		// Disable receiving packets off air
		// RXDECINV = 1; BBREG1 (0x39) bit 2
		// See example 3-2, page 109
		shortWrite(MRF_BBREG1,0x04);
		
		// See MRF24J40 Data sheet, Figure 3-9, page 107
		// m = MHR header length
		// n = MSDU payload length

		
		downloadPacket();
		
		// Re-enable reception of packets
		shortWrite(MRF_BBREG1,0x00);
		
	}

	public void downloadPacket () throws IOException {
		
		int i, j, len, lqi, rssi;
		
		long t = -System.currentTimeMillis();
		
		/*
		len = longRead(MRF_L_RXFIFO); // m + n + 2
		fcs0 = longRead(MRF_L_RXFIFO + 1 + len - 2);
		fcs1 = longRead(MRF_L_RXFIFO + 1 + len - 1);
		lqi = longRead(MRF_L_RXFIFO + 1 + len + 0);
		rssi = longRead(MRF_L_RXFIFO + 1 + len + 1);
		byte[] buf = new byte[len+2];
		for (i = 0; i < len+2; i++) {
			buf[i] = (byte)longRead(MRF_L_RXFIFO + i + 1);
		}
		*/
		byte[] buf = driver.readRxFifo();
		len = buf.length;
		lqi = buf[len-2];
		//lqi = 0;
		rssi = buf[len-1];
		//rssi = 0;
		
		t += System.currentTimeMillis();
		
		for (i = 0; i < len; i++) {
			System.err.print(formatHexByte(buf[i]) + " ");
		}
		System.err.println(" len=" + len + " lqi=" + lqi + " rssi=" + rssi + " t=" + t);
		
	}
	/**
	 * See MRF24J40 data sheet, section 3.12, page110.
	 * 
	 * @param packet
	 */
	public void sendPacket(byte[] packet) throws MRFException {
	
		int i;
	
		// Long address 0x000 - 0x07F is TX Normal FIFO
	
		// Reference MRF24J40 Data sheet, section 3.12, page 111
		// fifo[0] = header length
		// fifo[1] = frame length (header + data payload)
		// Header:
		// * Frame Control 2 bytes
		// * Sequence Number 1 byte
		// * Addressing fields 4 - 20 bytes
	
		longWrite(MRF_L_TXFIFO + 0x000, packet.length); // header length
		longWrite(MRF_L_TXFIFO + 0x001, packet.length); // length of the full
														// packet
	
		// Load packet into TXFIFO
		long t = -System.currentTimeMillis();
		for (i = 0; i < packet.length; i++) {
			longWrite(MRF_L_TXFIFO + i + 2, packet[i]);
		}
		t+=System.currentTimeMillis();
		System.err.println ("Load t=" + t);
	
		// Transmit packet without ACK requested
	
		// bit 4: FPSTAT: Frame Pending Status bit(1)
		// Status of the frame pending bit in the received Acknowledgement
		// frame.
		// bit 3: INDIRECT: Activate Indirect Transmission bit (coordinator
		// only)(4)
		// bit 2: TXNACKREQ: TX Normal FIFO Acknowledgement Request bit(2,4)
		// Transmit a frame with Acknowledgement frame expected. If
		// Acknowledgement is not received,
		// retransmit.
		// bit 1: TXNSECEN: TX Normal FIFO Security Enabled bit(3,4)
		// bit 0 TXNTRIG: Transmit Frame in TX Normal FIFO bit
		// 1 = Transmit the frame in the TX Normal FIFO; bit is automatically
		// cleared by hardware
		shortWrite(MRF_TXNCON, 0x01);
		
		// Wait for INTSTAT bit TXNIF = 1
		while ( (shortRead(MRF_INTSTAT)&0x01) == 0) {
			System.err.print (".");
		}
		System.err.println ("Sent.");
		
		// Check TXSTAT bit 0 TXNSTAT for 0 (success) or 1 (failure)
		i = shortRead(MRF_TXSTAT);
		if ((i&0x01)==0) {
			System.err.println ("Success.");
		} else {
			// bit 7,6: TXNRETRY
			System.err.print ("Failure. nretries=" + (i>>6));
			// bit 5: CCAFAIL: 1 indicates failure due to channel busy
			if ((i & 0x20) != 0) {
				System.err.println ("Channel busy.");
			}
		}
	
	}

	public void handleInterrupt() throws IOException {
		System.err.println ("*** INTERRUPT ***");
		downloadPacket();
	}
	
	private void debug (String s) {
		//System.err.println (s);
	}

}
