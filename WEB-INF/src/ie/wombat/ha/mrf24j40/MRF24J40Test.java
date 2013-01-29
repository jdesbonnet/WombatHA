package ie.wombat.ha.mrf24j40;

import static org.junit.Assert.*;

import org.junit.Test;

public class MRF24J40Test {
	private static final String SIO_DEVICE_NAME = "/dev/ttyUSB2";
	private static final int BAUDRATE = 38400;

	@Test
	public void testVersion() {
		MRF24J40 rf = new MRF24J40(SIO_DEVICE_NAME, BAUDRATE, 20);
		String version = rf.getSPIDriver().getVersion();
		System.err.println("version=" + version);
		assertTrue("01".equals(version));
		rf.close();
	}

	@Test
	public void testShortRead() throws Exception {
		int v;
		MRF24J40 rf = new MRF24J40(SIO_DEVICE_NAME, BAUDRATE, 20);
		for (int i = 0; i < 16; i++) {
			v = rf.shortRead((byte) 0x11);
			// v = rf.shortRead(MRF24J40.MRF_RXMCR);
			System.err.println("i=" + i + " v=" + v);
			assertTrue(v == 0x1c);
		}
		rf.close();
	}

	@Test
	public void testLongRead() throws Exception {
		int v=0;
		MRF24J40 rf = new MRF24J40(SIO_DEVICE_NAME, BAUDRATE, 20);
		
		// Scan entire register range looking for one non-zero
		// value. If all zeros we probably have a problem.
		for (int i = 0x200; i < 0x24D; i++) {
			v = rf.longRead(i);
			System.err.println("i=" + i + " v=" + v);
			if (v != 0) {
				System.err.println ("LReg[" + Integer.toHexString(i) + "]=" 
						+ Integer.toHexString(v));
				break;
			}
		}
		assertFalse(v == 0);
		rf.close();
	}
	
	@Test
	public void testLongWrite() throws Exception {
		int i,v;
		MRF24J40 rf = new MRF24J40(SIO_DEVICE_NAME, BAUDRATE, 20);
		
		// Save value
		v = rf.longRead(MRF24J40.MRF_L_ASSOSADR0);
		
		// Scan entire register range looking for one non-zero
		// value. If all zeros we probably have a problem.
		for (i = 0; i < 256; i+=3) {
			rf.longWrite(MRF24J40.MRF_L_ASSOSADR0, i);
			assertTrue (rf.longRead(MRF24J40.MRF_L_ASSOSADR0) == i);
		}
		
		// Restore pre-test value
		rf.longWrite(MRF24J40.MRF_L_ASSOSADR0, v);
		
		rf.close();
	}
	
	
	@Test
	public void testShortWrite() throws Exception {
		MRF24J40 rf = new MRF24J40(SIO_DEVICE_NAME, BAUDRATE, 20);

		// Will toggle bit 0 (PROMI flag) of RXMCR register
		// to verify short write (and read) operation.

		int v;

		for (int i = 0; i < 16; i++) {
			
			System.err.println ("iteration " + i);
			
			v = rf.shortRead(MRF24J40.MRF_RXMCR);
			// System.err.println ("RXMCR=" + v);

			v ^= 0x01;

			// shortWrite does not work.
			// System.err.println ("Writing modified value to RXMCR");
			rf.shortWrite(MRF24J40.MRF_RXMCR, v);

			// Read back
			// System.err.println ("Read back...");
			int v1 = rf.shortRead(MRF24J40.MRF_RXMCR);
			System.err.println("RXMCR'=" + v);

			assertTrue(v1 == v);

		}

		rf.close();
	}
	
	@Test
	public void testFifoWrite() throws Exception {
		MRF24J40 rf = new MRF24J40(SIO_DEVICE_NAME, BAUDRATE, 20);

		// Will toggle bit 0 (PROMI flag) of RXMCR register
		// to verify short write (and read) operation.

		int i,j,v;
		
		byte[] buf = new byte[127];

		for (i = 0; i < 4; i++) {
			
			System.err.println ("iteration " + i);
			
			// Populate buf[] with random numbers
			for (j = 0; j < buf.length; j++) {
				buf[j] = (byte)(Math.random()*255);
			}
			
			// Write to FIFO
			for (j = 0; j < buf.length; j++) {
				rf.longWrite(MRF24J40.MRF_L_TXFIFO + j, buf[j]);
			}
			
			// Read back and verify
			for (j = 0; j < buf.length; j++) {
				v = rf.longRead(MRF24J40.MRF_L_TXFIFO + j);
				//System.err.println ("v=" + v + " buf[j]=" + ((int)buf[j]&0xff));
				assertTrue (v == ((int)buf[j]&0xff));
			}

		}

		rf.close();
	}
	

	@Test
	public void testReset() throws MRFException {
		MRF24J40 rf = new MRF24J40(SIO_DEVICE_NAME, BAUDRATE, 20);
		rf.reset();
		rf.close();
	}
	
	@Test
	public void testScanChannels() throws MRFException {
		MRF24J40 rf = new MRF24J40(SIO_DEVICE_NAME, BAUDRATE, 20);
		rf.scanChannels();
		rf.close();
	}

}
