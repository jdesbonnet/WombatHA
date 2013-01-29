package ie.wombat.ha.mrf24j40;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;

import gnu.io.CommPort;

public class SPIDriverImpl implements SPIDriver {

	CommPort sioPort;
	private InputStream sioIn;
	private OutputStream sioOut;
	private BufferedReader sioReader;
	private Writer sioWriter;
	
	public SPIDriverImpl (CommPort sioPort) throws IOException {
		this.sioPort = sioPort;
		
		this.sioIn = sioPort.getInputStream();
		this.sioReader = new BufferedReader( new InputStreamReader(this.sioIn));
		
		this.sioOut = sioPort.getOutputStream();
		this.sioWriter = new OutputStreamWriter(sioOut);
		
	}
	public void close () {
		//sioReader.close();
		//sioWriter.close();
		sioPort.close();
	}
	public String getVersion () {
		
		try {
			sioWriter.write("V\r");
			sioWriter.flush();
			return sioReader.readLine();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

	public void chipSelect() {
		//System.err.println ("ChipSelect");
		try {
			sioWriter.write("[\r");
			sioWriter.flush();
			
			String ok = sioReader.readLine();
			if (!"OK".equals(ok)) {
				throw new SPIException ("Expecting OK in response to chipSelect, got " + ok);
			}
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void chipUnselect() {
		//System.err.println ("ChipUnselect");
		try {
			sioWriter.write("]\r");
			sioWriter.flush();
			
			String ok = sioReader.readLine();
			if (!"OK".equals(ok)) {
				throw new SPIException ("Expecting OK in response to chipUnselect, got " + ok);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void assertReset() {
		try {
			sioWriter.write("S\r");
			sioWriter.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * ChipSelect, spi_put v, spi_get, ChipUnselect and return result of spi_get
	 * 
	 * @param v
	 * @return
	 */
	public int spiCSPutGet (int v) {
		v &= 0xff;		
		try {
			String cmd = "[P" + ( v < 16 ? "0":"") + Integer.toHexString(v) + "G]\r";
			//System.err.println(cmd);
			sioWriter.write(cmd);
			sioWriter.flush();
			
			String vs= sioReader.readLine();
			//System.err.println("vs=" + vs);
			if (vs.length() != 2) {
				throw new SPIException (vs);
			}
			return Integer.parseInt(vs,16);
		} catch (IOException e) {
			e.printStackTrace();
			throw new SPIException (e.getMessage());
		}
	}

	public void spiPut(int v) throws SPIException {
		
		v &= 0xff;
		//System.err.println ("spiPut(" + Integer.toHexString(v) + "):");
		
		
		try {
			String cmd = "P" + ( v < 16 ? "0":"") + Integer.toHexString(v) + "\r";
			//System.err.println(cmd);
			
			sioWriter.write(cmd);
			sioWriter.flush();
			
			String ok = sioReader.readLine();
			if (!"OK".equals(ok)) {
				throw new SPIException ("Expecting OK in response to spi_put, got " + ok);
			}
		} catch (IOException e) {
			e.printStackTrace();
			throw new SPIException (e.getMessage());
		}
	}


	public int spiGet() throws SPIException {
		try {
			//System.err.println ("spiGet():");
			
			sioWriter.write("G\r");
			sioWriter.flush();
			//System.err.println ("R");
			String vs= sioReader.readLine();
			//System.err.println("vs=" + vs);
			if (vs.length() != 2) {
				throw new SPIException (vs);
			}
			return Integer.parseInt(vs,16);
		} catch (IOException e) {
			e.printStackTrace();
			throw new SPIException(e.getMessage());
		}
	}
	
	public void relay () throws Exception {
		System.err.println ("Relay Mode");
		while (true) {
			sioOut.write('V'); sioOut.write('\r'); 
			System.err.print("."); System.err.flush();
			System.err.println(sioReader.readLine());
			Thread.sleep(250);
		}
		/*
		while (true) {
			sioWriter.write("\r");
			System.err.println ("R: " + sioReader.readLine());
		}
		*/
		
	}
	public void setDelay(int v) throws SPIException {
		String cmd = "D" + ( v < 16 ? "0":"") + Integer.toHexString(v) + "\r";
		try {
			sioWriter.write(cmd);
			sioWriter.flush();
		} catch (IOException e) {
			throw new SPIException(e.getMessage());
		}
	}
	public int csPut16Get8(int a0, int a1) throws SPIException {
		// TODO Auto-generated method stub
		return 0;
	}
	public void registerInterruptHandler(InterruptHandler h) {
		// TODO Auto-generated method stub
		
	}
	public byte[] readRxFifo() throws SPIException {
		// TODO Auto-generated method stub
		return null;
	}
	public void resetDriver() {
		// TODO Auto-generated method stub
		
	}
	public void interruptEnable(boolean b) throws SPIException, IOException {
		// TODO Auto-generated method stub
		
	}
	public void waitForInterrupt() throws SPIException, IOException {
		// TODO Auto-generated method stub
		
	}
	public void setRelayMode(boolean b) throws IOException {
		// TODO Auto-generated method stub
		
	}
	public boolean interruptReceived() {
		// TODO Auto-generated method stub
		return false;
	}
	public void interruptForwardEnable(boolean b) {
		// TODO Auto-generated method stub
		
	}
}
