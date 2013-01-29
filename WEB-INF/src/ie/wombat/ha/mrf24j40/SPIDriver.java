package ie.wombat.ha.mrf24j40;

import java.io.IOException;

public interface SPIDriver {

	public void resetDriver();
	
	public String getVersion();
	public void chipSelect();
	public void chipUnselect();
	public void assertReset();
	public void spiPut(int v) throws SPIException;
	public int spiGet() throws SPIException;
	public void close();
	
	public void setDelay(int d) throws SPIException;
	public int csPut16Get8 (int a0, int a1) throws SPIException;
	
	public byte[] readRxFifo () throws SPIException, IOException;
	public void setRelayMode(boolean b) throws IOException;
	
	public boolean interruptReceived();
	public void interruptEnable (boolean b) throws SPIException, IOException;
	public void interruptForwardEnable (boolean b);
	
	public void registerInterruptHandler (InterruptHandler h);
	public void waitForInterrupt() throws SPIException, IOException;
	
}
