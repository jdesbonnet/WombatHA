package ie.wombat.ha.mrf24j40;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import gnu.io.CommPort;

public class SPIDriverImpl2 implements SPIDriver {

	public static final int ESC = 0x02;
	
	CommPort sioPort;
	private InputStream sioIn;
	private OutputStream sioOut;
	private InterruptHandler isr;
	
	private boolean interruptForwardEnable = false;
	private boolean interruptFlag = false;
	
	public SPIDriverImpl2 (CommPort sioPort) throws IOException {
		this.sioPort = sioPort;
		this.sioIn = sioPort.getInputStream();
		this.sioOut = sioPort.getOutputStream();
		
		sioOut.write('I');
		sioOut.flush();
		int s = readStreamByte();
		if (s != 0x00) {
			System.err.println ("Error enabling interrupts on driver");
			//throw new SPIException ("Unexpected status code " + s);
		}
		
	}
	
	public void close () {
		sioPort.close();
	}
	
	public String getVersion () throws SPIException {
		
		try {
			sioOut.write('V');
			System.err.println ("waiting for status...");
			
			// Expecting OK_1DATA status (OK with 1 byte of data, value 0x04)
			int s = readStreamByte();
			System.err.println ("status=" + s);
			if (s != 0x04) {
				throw new SPIException ("Unexpected status code. Expecting OK_1DATA (0x04) but got 0x" 
						+ Integer.toHexString(s)
						);
			}
			System.err.println ("reading data...");
			s = readStreamByte();
			System.err.println ("data=" + s);
			if (s < 16) {
				return "0" + Integer.toHexString(s);
			}
			return Integer.toHexString(s);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw new SPIException (e.getMessage());
		}
	}


	public void chipSelect() {
		//System.err.println ("ChipSelect");
		try {
			sioOut.write('[');
			sioOut.flush();
			int s = readStreamByte();
			if (s != 0x00) {
				throw new SPIException ("Unexpected status code in chipSelect() 0x" 
						+ Integer.toHexString(s) + ", expecting 0x00");
			}
			
		} catch (IOException e) {
			e.printStackTrace();
			throw new SPIException (e.getMessage());
		}
	}

	public void chipUnselect() {
		//System.err.println ("ChipUnselect");
		try {
			sioOut.write(']');
			sioOut.flush();
			int s = readStreamByte();
			if (s != 0x00) {
				throw new SPIException ("Unexpected status code " + s);
			}
			
		} catch (IOException e) {
			e.printStackTrace();
			throw new SPIException (e.getMessage());
		}
	}
	
	/**
	 * Assert RESET line to peripheral.
	 */
	public void assertReset() {
		try {
			sioOut.write('#');
			sioOut.flush();
			int s = readStreamByte();
			if (s != 0x00) {
				throw new SPIException ("Unexpected status code " + s);
			}
			
		} catch (IOException e) {
			e.printStackTrace();
			throw new SPIException (e.getMessage());
		}
	}

	public void spiPut(int v) throws SPIException {
		
		v &= 0xff;
		
		try {
			sioOut.write('P');
			writeStreamByte(v);
			sioOut.flush();
			int s = readStreamByte();
			if (s != 0x00) {
				throw new SPIException ("Unexpected status code " + s);
			}
		} catch (IOException e) {
			e.printStackTrace();
			throw new SPIException (e.getMessage());
		}
		
	}


	public int spiGet() throws SPIException {
		try {
			sioOut.write('G');
			sioOut.flush();
			int s = readStreamByte();
			if (s != 0x04) {
				throw new SPIException ("Unexpected status code " + s);
			}
			return readStreamByte();
		} catch (IOException e) {
			e.printStackTrace();
			throw new SPIException (e.getMessage());
		}
		
	}
	

	public void setDelay(int v) throws SPIException {
		
		try {
			sioOut.write('D');
			writeStreamByte(v);
			sioOut.flush();
			int s = readStreamByte();
			if (s != 0x00) {
				throw new SPIException ("Unexpected status code " + s);
			}
		} catch (IOException e) {
			e.printStackTrace();
			throw new SPIException (e.getMessage());
		}
	
	}
	
	public int readStreamByte () throws IOException {
		int v;
		while (true) {
			v = sioIn.read();
			if ( v == 0x03) {
				interruptFlag = true;
				System.err.println ("%%% INT %%%");
				// trigger interrupt
				if (isr != null && interruptForwardEnable) {
					isr.handleInterrupt();
				}
				continue;
			}
			if ( v == ESC) {
				v = sioIn.read();
				if (v == 'B') {
					return 0x02;
				}
				if (v == 'C') {
					return 0x03;
				}
			}
			return v;
		}
	}
	public void writeStreamByte (int v) throws IOException {
		if (v == 0x02) {
			sioOut.write(ESC);
			sioOut.write('B');
			return;
		}
		if (v == 0x03) {
			sioOut.write(ESC);
			sioOut.write('C');
			return;
		}
		sioOut.write(v);
	}

	public int csPut16Get8(int a0, int a1) throws SPIException {
	
		try {
			sioOut.write('r');
			writeStreamByte(a0);
			writeStreamByte(a1);
			sioOut.flush();
			int s = readStreamByte();
			if (s != 0x04) {
				throw new SPIException ("Unexpected status code " + s);
			}
			s = readStreamByte();
			return s;
		} catch (IOException e) {
			e.printStackTrace();
			throw new SPIException (e.getMessage());
		}
	}

	public void registerInterruptHandler(InterruptHandler h) {
		this.isr = h;
	}

	public byte[] readRxFifo() throws SPIException, IOException {
		//System.err.println("readRxFifo()");
		sioOut.write('x');
		sioOut.flush();
		int len = readStreamByte() & 0xff;
		byte[] buf = new byte[len + 2];
		for (int i = 0; i < len; i++) {
			buf[i] = (byte) readStreamByte();
		}
		return buf;
	}
	
	public void setRelayMode (boolean b) throws IOException {
		sioOut.write( b ? 'p' : ESC);	
		sioOut.flush();
		
		int len,c,i;
		
		byte[] buf = new byte[256];
		
		OutputStream pcapOut = System.out;
		DataOutputStream pcapData = new DataOutputStream(pcapOut);
	
		
		// Write PCAP header
		pcapData.writeInt(PCAP.PCAP_MAGIC);
		pcapData.writeShort(PCAP.PCAP_VERSION_MAJOR);
		pcapData.writeShort(PCAP.PCAP_VERSION_MINOR);
		pcapData.writeInt(PCAP.PCAP_TZ);
		pcapData.writeInt(PCAP.PCAP_SIGFIGS);
		pcapData.writeInt(PCAP.PCAP_SNAPLEN);
		pcapData.writeInt(PCAP.PCAP_LINKTYPE);
		pcapData.flush();
		
		while (true) {
			
			// Wait for 0x03 (start of packet)
			while ( (c=sioIn.read()) != 0x03) {
				System.err.print(" (0x" + Integer.toHexString(c) +")");
				System.err.flush();
			}
			
			len = readStreamByte() & 0xff;
			System.err.print(" len=" + len);
			//buf = new byte[len + 2];
			for (i = 0; i < len; i++) {
				buf[i] = (byte) readStreamByte();
				System.err.print( " " + Integer.toHexString(buf[i]&0xff));
			}
			System.err.println ("");
			
			if (len < 4) {
				System.err.println ("Dropping packet. Too short.");
				continue;
			}
			
			long t = System.currentTimeMillis();
			
			// ts_sec: timestamp seconds
			pcapData.writeInt((int)(t/1000));
			
			// ts_usec: timestamp microseconds
			pcapData.writeInt((int)(t%1000) * 1000);
			
			pcapData.writeInt(len-4);  // Size captured (exclude two FCS bytes)
			pcapData.writeInt(len-2);  // Original size of packet (same as MRF24J40 frame length)
			pcapData.write(buf, 0, len-4);
			pcapData.flush();
			
		}
	}

	/**
	 * Causes driver hardware to reset.
	 */
	public void resetDriver() throws SPIException {
	
		try {
			sioOut.write(0x03);
			sioOut.flush();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw new SPIException (e.getMessage());
		}
	}

	/**
	 * Enables interrupt handling on the driver hardware. Need to also
	 * enable interruptForward to enable interrupt callbacks.
	 */
	public void interruptEnable(boolean b) throws SPIException, IOException {
		sioOut.write( b ? 'I' : 'i');
		sioOut.flush();
		int s = readStreamByte();
		if (s != 0x00) {
			throw new SPIException ("Unexpected status code " + s);
		}	
	}

	public void waitForInterrupt() throws SPIException, IOException {
		// 's' command stalls until peripheral device interrupt is
		// received.
		sioOut.write('s');
		sioOut.flush();
		//long t = -System.currentTimeMillis();
		int s = readStreamByte();
		//t += System.currentTimeMillis();
		//System.err.println ("delay=" + t);
		if (s != 0x00) {
			throw new SPIException ("Unexpected status code " + s);
		}	
	}

	public void interruptForwardEnable(boolean b) {
		this.interruptForwardEnable = b;
	}

	public synchronized boolean interruptReceived() {
		if (interruptFlag) {
			interruptFlag = false;
			return true;
		}
		return false;
	}
	
}
