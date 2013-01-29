package ie.wombat.ha.app.xbeesensor;

import java.io.IOException;

import ie.wombat.ha.devices.XBeeSeries2;

/**
 * Implement temperature and humidity queries to a SHT71 and SHT75 sensor by bit banging
 * XBee IO lines. 
 * 
 * Optimization ideas: No need to sample the most significant bits of a reading
 * (first 2 MSB with 14 bit temperature and first 4 with 12bit RH). On falling
 * clock edge any necessary data line transition can be consolidated into one
 * transaction (ie one ATAC for both state changes). Instead of a delay between
 * packets, wait for APS layer acknowledgement before sending the next packet.
 * 
 * @author Joe Desbonnet
 *
 */
public class SHT7x {

	public static final int CMD_TEMPERATURE_READ = 0x03;
	public static final int CMD_HUMIDITY_READ = 0x05;
	
	private int SAMPLE_WAIT_TIMEOUT = 5000;
	
	private XBeeSeries2 xbee;
	
	private int clockPin;
	private int dataPin;
	
	// delay=100, sampleRate=250 does not work
	// delay=150, sampleRate=250 does not work (reliably)
	// delay=200, sampleRate=250 works
	// delay=180, sampleRate=50 works
	// delay=180, sampleRate=100 works

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
	public SHT7x(XBeeSeries2 xbee, int clockPin, int dataPin) {
		this.xbee = xbee;
		this.clockPin = clockPin;
		this.dataPin = dataPin;
	}

	/**
	 * Implement short delay. Usually used to space ZigBee packets apart.
	 */
	private void delay() {
		try {
			Thread.sleep(delay);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void clockHigh() throws IOException {
		byte[] param = new byte[1];
		param[0] = XBeeSeries2.HIGH;
		xbee.atCommand("D" + clockPin, param);
		xbee.atCommand("AC");
		delay();
	}

	private void clockLow() throws IOException {
		byte[] param = new byte[1];
		param[0] = XBeeSeries2.LOW;
		xbee.atCommand("D" + clockPin, param);
		xbee.atCommand("AC");
		delay();
	}

	private void dataHigh() throws IOException {
		byte[] param = new byte[1];
		// Data high is achieved by high impedance state (HIGH_Z) ie digital
		// input mode
		param[0] = XBeeSeries2.HIGH_Z;
		xbee.atCommand("D" + dataPin, param);
		xbee.atCommand("AC");
		delay();
	}

	private void dataLow() throws IOException {
		byte[] param = new byte[1];
		param[0] = XBeeSeries2.LOW;
		xbee.atCommand("D" + dataPin, param);
		xbee.atCommand("AC");
		delay();
	}

	/**
	 * Reset communications with sensor. Required if the previous query did not complete.
	 * 
	 * @param xbee
	 */
	public void resetComms() throws IOException {

		int i;

		dataHigh();

		// Pulse clock 9+ times while data high
		for (i = 0; i < 10; i++) {
			clockHigh();
			clockLow();
		}
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

			// wait for IO sample
			t0 = System.currentTimeMillis();
			while (xbee.getLastIOSampleTime() < t0) {
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					// ignore
				}
				
				if ( (System.currentTimeMillis() - t0) > SAMPLE_WAIT_TIMEOUT) {
					throw new IOException ("timeout waiting for sample from XBee");
				}
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
	/**
	 * Start sequence.
	 * 
	 * @throws IOException
	 * 
	 */
	private void startSequence() throws IOException {
		clockHigh();
		dataLow();
		clockLow();
		clockHigh();
		dataHigh();
		clockLow();
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
		int i;
		boolean lastBit = true;
		boolean currentBit = false;

		// MSB first
		for (i = 0; i < 8; i++) {
			currentBit = ((command & 0x80) != 0);

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
			command <<= 1;
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
	 * Reference datasheet §4.3.
	 * @return Temperature in °C
	 * @throws IOException
	 */
	public float readTemperature() throws IOException {
		int v = makeReading(CMD_TEMPERATURE_READ);
		return -39.7f + 0.01f * (float) v;		
	}
	
	/**
	 * Reference datasheet §4.1.
	 * 
	 * @return Humidity as RH%
	 * @throws IOException
	 */
	public float readHumidity() throws IOException {
		int v = makeReading(CMD_HUMIDITY_READ);
		return (float)(-2.0468 + 0.0367*(double)v - 1.5955e-6*(double)v*(double)v);
	}
	
	/**
	 * Reset comms, send start sequence, command and read 16 bits of data.
	 * 
	 * @param what One of SHT7x.CMD_TEMPERATURE_READ or SHT7x.CMD_HUMIDITY_READ
	 * @return
	 * @throws IOException
	 */
	private int makeReading (int what) throws IOException {

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

		// The last query may not have completed leaving the communications in an undefined
		// state. Reset communications to a known state.
		resetComms();
		delay();
		delay();

		// Start sequence ref §3.2.
		startSequence();
		delay();
		delay();

		// Send 8 bit command (and read ack bit)
		sendCommand(what);

		// Wait for measurement to complete. We could poll the DATA line. It will be pulled
		// low by the sensor when the reading is complete. However the overhead of doing
		// this makes it not worth the effort.
		try {
			Thread.sleep(200);
		} catch (InterruptedException e1) {
			// ignore
		}

		//
		// Read 16 bits + CRC8
		//

		int v0,v1,crc;
		v0 = readByte();
		v1 = readByte();
		crc = readByte();
		
		/*
		System.err.println ("***** " 
				+ what 
				+ " " + Integer.toHexString(v0) 
				+ " " + Integer.toHexString(v1)
				+ " " + Integer.toHexString(crc)
				);
		*/
		
		// reverse bits of CRC
		int bitReversedCrc = 0;
		for (i = 0; i < 8; i++) {
			bitReversedCrc >>= 1;
			if ((crc&0x80) != 0) {
				bitReversedCrc |= 0x80;
			}
			crc <<= 1;
		}
		

		int v = (v0<<8)|v1;

		CRC8 crc8 = new CRC8();
		crc8.addByteTable(what);
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

		if (crc8.getCrc() == bitReversedCrc) {
			System.err.println ("CRC SUCCESS");
		} else {
			System.err.println ("CRC FAIL!!!");
			throw new IOException ("CRC fail, read value = 0x" + Integer.toHexString(v));
		} 
		
		
		return v;

	}

}
