package ie.wombat.ha.app.xbeesensor;

import java.io.IOException;

import ie.wombat.ha.devices.XBeeSeries2;

/**
 * Implement temperature and humidity queries to a SHT21 and SHT25 sensor by bit baning
 * I2C protocol on XBee IO lines. 
 * 
 * @author joe
 *
 */
public class SHT2x {

	public static final int CMD_TEMPERATURE_READ = 0xE3;
	public static final int CMD_HUMIDITY_READ = 0xE5;
	
	private XBeeSeries2 xbee;
	
	private int clockPin;
	private int dataPin;
	private int debugPin = 0;

	// delay between sending each packet to the NIC for transmission
	private int delay = 220;
	
	// ms between each sample
	private int sampleRate = 100;

	/**
	 * 
	 * @param xbee XBee proxy object
	 * @param clockPin XBee pin used to implement SCK (0 = DIO0, 1 = DIO1 etc)
	 * @param dataPin XBee pin used to implement SDA (0 = DIO0 .. etc)
	 */
	public SHT2x(XBeeSeries2 xbee, int clockPin, int dataPin) {
		this.xbee = xbee;
		this.clockPin = clockPin;
		this.dataPin = dataPin;
	}

	/**
	 * Implement short delay. Usually used to space ZigBee packets apart.
	 */
	private void delay () {
		delay(delay);
	}
	private void delay(int d) {
		try {
			Thread.sleep(d);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void clockHigh() throws IOException {
		byte[] param = new byte[1];
		param[0] = XBeeSeries2.HIGH;
		xbee.atCommand("D" + clockPin, param);
delay(100);
		xbee.atCommand("AC");
		delay();
	}

	private void clockLow() throws IOException {
		byte[] param = new byte[1];
		param[0] = XBeeSeries2.LOW;
		xbee.atCommand("D" + clockPin, param);
delay(100);

		xbee.atCommand("AC");
		delay();
	}

	private void dataHigh() throws IOException {
		byte[] param = new byte[1];
		// Data high is achieved by high impedance state (HIGH_Z) ie digital
		// input mode
		param[0] = XBeeSeries2.HIGH_Z;
		xbee.atCommand("D" + dataPin, param);
delay(100);

		xbee.atCommand("AC");
		delay();
	}

	private void dataLow() throws IOException {
		byte[] param = new byte[1];
		param[0] = XBeeSeries2.LOW;
		xbee.atCommand("D" + dataPin, param);
delay(100);
		xbee.atCommand("AC");
		delay();
	}

	private void pulseDebug () throws IOException {
		byte[] param = new byte[1];
		param[0] = XBeeSeries2.HIGH;
		xbee.atCommand("D" + debugPin, param);
		xbee.atCommand("AC");
		delay();
		param[0] = XBeeSeries2.LOW;
		xbee.atCommand("D" + debugPin, param);
		xbee.atCommand("AC");
		delay();
	}


	/**
	 * Read 8 bits from bus and write ACK bit.
	 * 
	 * @return
	 * @throws IOException
	 */
	private int readByte () throws IOException {
		int i,v = 0, sample;
		long t0;
		
		int dataPinMask = 1<<dataPin;

		
		for (i = 0; i < 8; i++) {

			v <<= 1;

			clockHigh();
delay(100);
			// wait for IO sample
			t0 = System.currentTimeMillis();
			while (xbee.getLastIOSampleTime() < t0) {
				delay(100);
			}
			// sample = xbee.getIOSample();
			sample = xbee.getLastIOSample();
			if ((sample & dataPinMask) != 0) {
				v |= 1;
			}
			clockLow();
		}

		// write ack bit (0)
		dataLow();
		clockHigh();
		clockLow();
		dataHigh();
		
		return v;
	}
	
	private void writeByte (int v) throws IOException {
		
		boolean currentBit=false;
		boolean lastBit=false; // precondition that data is low before starting
		// MSB first
		for ( int i = 0; i < 8; i++) {
			currentBit = ((v & 0x80) != 0);

			// Only change data pin if there is a change. This reduces the number
			// of ZigBee packets transmitted.
			if (currentBit != lastBit) {
				if (currentBit) {
					dataHigh();
				} else {
					dataLow();
				}
				lastBit = currentBit;
			}

			// Pulse clock
			clockHigh();
			clockLow();
			v <<= 1;
		}

		// If data currenly low bring to high_z/input mode
		if (currentBit == false) {
			dataHigh();
		}

		// I don't bother to read the ACK bit, but the clock
		// must still be pulsed for it. 
		clockHigh();
		// don't bother sampling data pin -- will assume all ok
		clockLow();

	}
	/**
	 * Start sequence.
	 * 
	 * @throws IOException
	 * 
	 */
	private void startSequence() throws IOException {
		clockHigh();
		dataHigh();
		dataLow();
		clockLow();
	}
	private void stopSequence() throws IOException {
		clockHigh();
		dataHigh();
	}
	
	/**
	 * A command is 8 bits (MSB first) followed reading an ack bit from the device. 
	 * In this implementation I ignore the result of the ack bit (but there
	 * still must be a 9th clock pulse). Bits are written by setting the
	 * data pin to either 0V (logic 0) or high impedance (logic 1). The data is
	 * read by the sensor during a low to high transition of the clock signal.
	 * 
	 * @param command
	 * @throws IOException
	 */
	private void sendCommand (int command) throws IOException {
	
	}

	/**
	 * Reference datasheet §4.3.
	 * @return Temperature in °C
	 * @throws IOException
	 */
	public float readTemperature() throws IOException {
		int v = makeReading(CMD_TEMPERATURE_READ);
		return -46.85f + 175.72f * ((float)v / 65536f);		
	}
	
	/**
	 * Reference datasheet §4.1.
	 * 
	 * @return Humidity as RH%
	 * @throws IOException
	 */
	public float readHumidity() throws IOException {
		int v = makeReading(CMD_HUMIDITY_READ);
		return -6f + 125f*((float)v / 65536f);		
	}
	
	public void softReset () throws IOException {
		byte[] params2 = new byte[2];
		params2[0] = (byte) (sampleRate >> 8);
		params2[1] = (byte) (sampleRate & 0xff);
		xbee.atCommand("IR", params2);
		delay();
		xbee.atCommand("AC");
		delay();
		
		
		for (int i = 0; i < 11; i++) {
			clockHigh();
			clockLow();
		}
		
		stopSequence();
		
		delay();
		
		
		startSequence();
		
		writeByte(0x80);
		delay();
		writeByte(0xFE);
		delay();
		
		stopSequence();
		delay();
				
	}
	/**
	 * Reset comms, send start sequence, command and read 16 bits of data.
	 * 
	 * @param what One of SHT7x.CMD_TEMPERATURE_READ or SHT7x.CMD_HUMIDITY_READ
	 * @return
	 * @throws IOException
	 */
	public int makeReading (int what) throws IOException {

		int i;

		//
		// Configure XBee to send frequent IO samples. This has two important functions.
		// First it keeps the XBee end device awake. Also an end device transmitting a 
		// packet (which must go via its parent) has the side effect of polling the 
		// parent for any incoming packets. So frequent transmission also means low
		// latency in receiving packets.
		
		byte[] params2 = new byte[2];
		params2[0] = (byte) (sampleRate >> 8);
		params2[1] = (byte) (sampleRate & 0xff);
		xbee.atCommand("IR", params2);
		xbee.atCommand("AC");
		
		// Now wait for the first sample to arrive before proceeding. At this point we'll
		// know the end device awake.
		long t0 = System.currentTimeMillis();
		while (xbee.getLastIOSampleTime() < t0) {
			delay();
		}
		
		// XBee End Device should now be awake and responsive


		pulseDebug();
		startSequence();
		pulseDebug();
		

		// Send 8 bit command (and read ack bit)
		writeByte(0x80);
		pulseDebug();

		writeByte(what);
		pulseDebug();
		
		startSequence();
		pulseDebug();
		writeByte(0x81);
		pulseDebug();

		// Wait for measurement to complete. We could poll the DATA line. It will be pulled
		// low by the sensor when the reading is complete. However the overhead of doing
		// this makes it not worth the effort.
		delay(200);
		

		//
		// Read 16 bits + CRC8
		//

		int v0,v1,crc;
		v0 = readByte();
		pulseDebug();

		v1 = readByte();
		pulseDebug();
		crc = readByte();
		pulseDebug();
		
		stopSequence();
		
		
		System.err.println ("***** 0x" 
				+ Integer.toHexString(what) 
				+ " 0x" + Integer.toHexString(v0) 
				+ " 0x" + Integer.toHexString(v1)
				+ " 0x" + Integer.toHexString(crc)
				);
		
		int v = (v0<<8)|v1;

		CRC8 crc8 = new CRC8();
		crc8.addByteTable(v0);
		crc8.addByteTable(v1);



		//
		// Return end device to normal sleep pattern by disabling sampling (ie by
		// setting sample period to 0).
		//
		params2 = new byte[2];
		params2[0] = 0;
		params2[1] = 0;
		xbee.atCommand("IR", params2);
		xbee.atCommand("AC");

		if (crc8.getCrc() == crc) {
			System.err.println ("CRC SUCCESS");
		} else {
			System.err.println ("CRC FAIL!!!");
			throw new IOException ("CRC fail, read value = 0x" + Integer.toHexString(v));
		} 
		
		
		return v;

	}

}
