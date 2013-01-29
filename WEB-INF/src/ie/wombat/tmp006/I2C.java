package ie.wombat.tmp006;

public interface I2C {

	public static final int START = 0x02;
	public static final int STOP = 0x03;
	public static final int ACK = 0x06;
	public static final int NAK = 0x07;
	
	// 00000100 - I2C read byte
	public static final int READ_BYTE = 0x04;
	
}
