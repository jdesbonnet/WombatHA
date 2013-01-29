package ie.wombat.sht21;

import gnu.io.CommPortIdentifier;
import gnu.io.PortInUseException;
import gnu.io.SerialPort;
import gnu.io.UnsupportedCommOperationException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;

/**
 * A program to use the Dangerous Prototypes Bus Pirate to query the SHT21
 * temperature and humidity sensor. Tested with Bus Pirate V3B hardware 
 * running firmware V5.9.
 * 
 * Requires RXTX Java IO library.
 * 
 * See this blog post for more information:
 * http://jdesbonnet.blogspot.com/
 * 
 * Version 0.1, 27 October 2011.
 *
 * 
 * TODO: Remove System.err.println() debugging 
 * 
 * 
 * @author Joe Desbonnet, jdesbonnet@gmail.com
 * 
 */

public class SHT21 {
	
	public static final int I2C_START = 0x02;
	public static final int I2C_STOP = 0x03;
	public static final int I2C_READ_BYTE = 0x04;
	public static final int I2C_ACK = 0x06;
	public static final int I2C_NAK = 0x07;
	
	public static final int SHT21_MEAS_T_HM =  0xE3; // 1110’0011;
	public static final int SHT21_MEAS_RH_HM = 0xE5;  // 1110’0101
	public static final int SHT21_MEAS_T = 0xF3; // 1111’0011
	public static final int SHT21_MEAS_RH = 0xF5;
	public static final int SHT21_WRITE_REG = 0xE6; // 1110’0110
	public static final int SHT21_READ_REG = 0xE7; // 1110’0110
	public static final int SHT21_SOFT_RST = 0xFE;
	
	private String sioDevName;
	private SerialPort sioPort;
	private OutputStream sioOut;
	private InputStream sioIn;
	
	private void setDeviceName (String sioDevName) throws IOException {
		this.sioDevName = sioDevName;

		Enumeration<CommPortIdentifier> ports = CommPortIdentifier
				.getPortIdentifiers();
		while (ports.hasMoreElements()) {
			CommPortIdentifier cpi = (CommPortIdentifier) ports.nextElement();
			if (cpi.getPortType() == CommPortIdentifier.PORT_SERIAL
					&& cpi.getName().equals(this.sioDevName)) {
				try {
					sioPort = (SerialPort) cpi.open("SIOReader", 2000);
				} catch (PortInUseException e) {
					e.printStackTrace();
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

		try {
			sioPort.setSerialPortParams(115200, SerialPort.DATABITS_8,
					SerialPort.STOPBITS_1, SerialPort.PARITY_NONE);
			sioPort.setFlowControlMode(SerialPort.FLOWCONTROL_NONE);
		} catch (UnsupportedCommOperationException e) {
			e.printStackTrace();
		}
		
		
		try {
			sioOut = sioPort.getOutputStream();
			sioIn = sioPort.getInputStream();
		} catch (IOException e) {
			e.printStackTrace();
		}

		try {
			sioPort.enableReceiveTimeout(200);
		} catch (UnsupportedCommOperationException e) {
			e.printStackTrace();
		}
	
	}
	
	/**
	 * Set the Bus Pirate in I2C mode.
	 * 
	 * @throws IOException
	 */
	public void enterI2CMode () throws IOException {
		int i;
		
		// First test to see if already in I2C mode
		clearInputStream();
		sioOut.write (0x01);
		sioOut.flush();
		
		if (  sioIn.read()=='I' && sioIn.read()=='2' && sioIn.read()=='C' && sioIn.read()=='1' ) {
			// Already in I2C mode
			System.err.println ("Already in I2C mode I2C");
			return;
		}
		
		System.err.println ("Not already in I2C mode. Attempting to enter.");
		
		// Send commands to Bus Pirate to first enter BigBang mode and then
		// I2C binary mode. Documentation recommends the following:
		// (quote)
		// The Bus Pirate user terminal could be stuck in a configuration menu 
		// when your program attempts to enter binary mode. One way to ensure 
		// that you’re at the command line is to send <enter> at least 10 times, 
		// and then send ‘#’ to reset. Next, send 0×00 to the command line 20+ 
		// times until you get the BBIOx version string.
		// (end quote)
		
		// Back to command line
		System.err.println ("RETURN x 10");
		for (i = 0; i < 10; i++) {
			sioOut.write(13); // RETURN
		}
		sioOut.flush();
		clearInputStream();
		
		// Reset
		System.err.println ("RESET");
		sioOut.write ('#');
		sioOut.flush();
		clearInputStream();
		
		// Enter BigBang mode
		System.err.println ("Enter BBIO");
		for (i = 0; i < 20; i++) {
			sioOut.write(0);
		}
		sioOut.flush();
		
		while ( ! (sioIn.read()=='B' && sioIn.read()=='B' && sioIn.read()=='I' && sioIn.read() =='O')) {
			// empty loop
		}
		System.err.println ("BBIO" + (char)sioIn.read());
		
		// Enter I2C mode
		System.err.println ("Enter I2C mode");
		sioOut.write(0x02);
		sioOut.flush();
		clearInputStream();
		
		// Get I2C version
		System.err.println ("Get I2C version");
		sioOut.write (0x01);
		sioOut.flush();
		while ( ! (sioIn.read()=='I' && sioIn.read()=='2' && sioIn.read()=='C') ) {
			// empty loop
			System.err.print (".");
			System.err.flush();
		}
		System.err.println ("I2C" + (char)sioIn.read());
	}
	
	/**
	 * Send Bus Pirate command to apply power to the 3.3V line
	 * 
	 * @throws IOException
	 */
	public void powerOn () throws IOException {
		// 0100wxyz – Configure peripherals w=power, x=pullups, y=AUX, z=CS
		// Write 0b0100 1000
		System.err.println ("POWER ON");
		sioOut.write (0x48);
		
		//011000xx - Set I2C speed, 3=~400kHz, 2=~100kHz, 1=~50kHz, 0=~5kHz (updated in v4.2 firmware)
		System.err.println ("SPEED 50kHz");
		sioOut.write (0x61);
		System.err.println ("response=" + sioIn.read());

	}
	
	/**
	 * Send I2C sequence to initiate a measure and download result.
	 * 
	 * @param command
	 * @return
	 * @throws IOException
	 */
	public int measure (int command) throws IOException {
		
		clearInputStream();
		
		// Test I2C mode
		sioOut.write (0x01);
		sioOut.flush();
		
		if ( ! (sioIn.read()=='I' && sioIn.read()=='2' && sioIn.read()=='C' && sioIn.read()=='1') ) {
				throw new IOException ("Not in I2C mode");
		}
		
		
		// Write I2C START
		//System.err.print ("START");
		sioOut.write (I2C_START);
		sioOut.flush();
		//System.err.println (" response=" + sioIn.read());
		sioIn.read();
		
		// Write I2C address + write 0b10000000
		//System.err.print ("Writing 0x80");
		sioOut.write (0x10); // BusPirate write one byte command
		//System.err.print (" response=" + sioIn.read());
		sioOut.write (0x80);
		sioOut.flush();
		//System.err.println ( sioIn.read() == 0 ? " ACK" : " NAK");
		sioIn.read();		sioIn.read();

		
		// Write SHT21 command
		//System.err.print ("Writing 0x" + Integer.toHexString(command));
		sioOut.write (0x10); // BusPirate write one byte command
		//System.err.print (" response=" + sioIn.read());
		sioOut.write (command);
		sioOut.flush();
		//System.err.println ( sioIn.read() == 0 ? " ACK" : " NAK");
		sioIn.read(); 		sioIn.read();


		
		clearInputStream();

		
		//System.err.print ("START");
		sioOut.write (I2C_START);
		sioOut.flush();
		//System.err.println (" response=" + sioIn.read());
		sioIn.read();

		//System.err.print ("Writing 0x81");
		sioOut.write (0x10); // write one byte
		//System.err.print (" response=" + sioIn.read());
		sioOut.write (0x81);
		sioOut.flush();
		//System.err.println ( sioIn.read() == 0 ? " ACK" : " NAK");
		sioIn.read();		sioIn.read();


				
		clearInputStream();

		
		int value=0;
		int data;
		
		//System.err.print ("READ");
		sioOut.write (I2C_READ_BYTE);
		sioOut.flush();
		data = sioIn.read() & 0xff;
		//System.err.print (" data=" + Integer.toHexString(data));
		//System.err.print (" ACK");
		sioOut.write (I2C_ACK);
		sioOut.flush();
		//System.err.println (" response=" + sioIn.read());
		sioIn.read();
		value |= data;
		
		clearInputStream();

		value <<= 8;
		sioOut.write (I2C_READ_BYTE);
		sioOut.flush();
		data = sioIn.read() & 0xff;
		sioOut.write (I2C_ACK);
		sioOut.flush();
		//System.err.println (" response=" + sioIn.read());
		sioIn.read();
		value |= data;
		
		clearInputStream();

		
		
		//System.err.print ("READ");
		sioOut.write (I2C_READ_BYTE);
		sioOut.flush();
		data = sioIn.read() & 0xff;
		//System.err.print (" data=" + Integer.toHexString(data));
		//System.err.print (" NAK");
		sioOut.write (I2C_NAK);
		sioOut.flush();
		//System.err.println (" response=" + sioIn.read());
		sioIn.read();

		int crc8 = data ;

		sioOut.write (I2C_STOP);
		sioOut.flush();
				
		return value;
	}
	
	
	/**
	 * Get SHT21 ID. This is not covered in the datasheet. Ref 
	 * Electronic Identification Code How to read-out the serial number of SHT2x
	 * http://www.sensirion.com/en/pdf/product_information/Electronic_Identification_Code_SHT2x_V1-1_C2.pdf
	 * 
	 * @return
	 * @throws IOException
	 */
	public int getID () throws IOException {
		
		clearInputStream();
		
		// Write I2C START
		//System.err.print ("START");
		sioOut.write (I2C_START);
		sioOut.flush();
		sioIn.read();
		
		// Write START 0x80 0xFA 0xFF
		sioOut.write (0x10); // BusPirate write one byte command
		sioOut.write (0x80);
		sioOut.flush(); sioIn.read();		sioIn.read();
		

		sioOut.write (0x10); // BusPirate write one byte command
		sioOut.write (0xFA);

		sioOut.flush(); sioIn.read(); 		sioIn.read();
		
		sioOut.write (0x10); // BusPirate write one byte command
		sioOut.write (0x0F);
		sioOut.flush(); sioIn.read(); 		sioIn.read();

		
		clearInputStream();

		
		//System.err.print ("START");
		sioOut.write (I2C_START);
		sioOut.flush();
		sioIn.read();

		//System.err.print ("Writing 0x81");
		sioOut.write (0x10); // write one byte
		sioOut.write (0x81);
		sioOut.flush(); sioIn.read();		sioIn.read();
		
		clearInputStream();
		int crc;
		
		int snb3 = i2cReadByteAndAck();
		crc = i2cReadByteAndAck();
		int snb2 = i2cReadByteAndAck();
		crc = i2cReadByteAndAck();
		int snb1 = i2cReadByteAndAck();
		crc = i2cReadByteAndAck();
		int snb0 = i2cReadByteAndAck();
		crc = i2cReadByteAndNak();
		
		
	
		sioOut.write (I2C_STOP);
		sioOut.flush();
		
		return ( (snb3<<24) | (snb2<<16) | (snb1<<8) | snb0);
	}
	
	public int getID_AC () throws IOException {
		
		clearInputStream();
		
		// Write I2C START
		//System.err.print ("START");
		sioOut.write (I2C_START);
		sioOut.flush();
		sioIn.read();
		
		// Write START 0x80 0xFA 0xFF
		sioOut.write (0x10); // BusPirate write one byte command
		sioOut.write (0x80);
		sioOut.flush(); sioIn.read();		sioIn.read();
		

		sioOut.write (0x10); // BusPirate write one byte command
		sioOut.write (0xFC);

		sioOut.flush(); sioIn.read(); 		sioIn.read();
		
		sioOut.write (0x10); // BusPirate write one byte command
		sioOut.write (0xC9);
		sioOut.flush(); sioIn.read(); 		sioIn.read();

		
		clearInputStream();

		
		//System.err.print ("START");
		sioOut.write (I2C_START);
		sioOut.flush();
		sioIn.read();

		//System.err.print ("Writing 0x81");
		sioOut.write (0x10); // write one byte
		sioOut.write (0x81);
		sioOut.flush(); sioIn.read();		sioIn.read();
		
		clearInputStream();
		int crc;
		
		int snc1 = i2cReadByteAndAck();
		int snc0 = i2cReadByteAndAck();
		crc = i2cReadByteAndAck();
		int sna1 = i2cReadByteAndAck();
		int sna0 = i2cReadByteAndAck();
		crc = i2cReadByteAndNak();
		
		
	
		sioOut.write (I2C_STOP);
		sioOut.flush();
		
		return ( (snc1<<24) | (snc0<<16) | (sna1<<8) | sna0);
	}
	
	
	private int i2cReadByteAndAck () throws IOException {
		sioOut.write (I2C_READ_BYTE);
		sioOut.flush();
		int data = sioIn.read() & 0xff;
		
		sioOut.write (I2C_ACK);
		sioOut.flush();
		sioIn.read();
		
		return data;
	}
	private int i2cReadByteAndNak () throws IOException {
		sioOut.write (I2C_READ_BYTE);
		sioOut.flush();
		int data = sioIn.read() & 0xff;
		
		sioOut.write (I2C_NAK);
		sioOut.flush();
		sioIn.read();
		
		return data;
	}
	/**
	 * Clear input buffer.
	 * 
	 * @throws IOException
	 */
	public void clearInputStream () throws IOException {
		int c;
		while ( (c = sioIn.read()) != -1) {
			//System.err.print ((char)c + " ");
		}
	}
	
	
	
	public static void main (String[] arg) throws IOException, Exception {
		SHT21 sht21 = new SHT21();
		
		sht21.setDeviceName (arg[0]);
		
		// Apply power to power pins
		System.err.println ("Power pins on");
		sht21.powerOn();
		
		System.err.println ("Entering I2C mode");
		sht21.enterI2CMode();
		
		int id_b = sht21.getID();
		System.err.println ("ID_B=" + Integer.toHexString(id_b));
		int id_ac = sht21.getID_AC();
		System.err.println ("ID_AC=" + Integer.toHexString(id_ac));
		
		long id;
		id = (long)id_ac <<48;
		id |= (long)id_b << 16;
		id |= (long)id_ac >> 16;
		System.err.println ("ID=" + Long.toHexString(id));

		int t,rh;
		double rhd, td;
		while (true) {
			t = sht21.measure(SHT21_MEAS_T_HM);
			rh = sht21.measure(SHT21_MEAS_RH_HM);
			
			rhd = -6 + 125*((double)rh / 65536);
			td = -46.85 + 175.72 * ((double)t / 65536);

			//System.out.println ("t=" + Integer.toHexString(t) + " rh=" + Integer.toHexString(rh));
			System.out.println ((System.currentTimeMillis()/1000) + " " + td + " " + rhd);
			
			Thread.sleep(5000);

		}
		//System.exit(0);
	
	}
}
