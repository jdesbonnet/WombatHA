package ie.wombat.ha.nic.xbee;

public interface XBeeConstants {
	public static final int START_OF_FRAME = 0x7E;
	public static final int XON = 0x11;
	public static final int XOFF = 0x13;
	public static final int ESCAPE = 0x7D;
	
	public static final int SUCCESS = 0;
	
	public static final int TX_SUCCESS = 0x00;
	public static final int TX_ADDRESS_NOT_FOUND = 0x24;
	
}
