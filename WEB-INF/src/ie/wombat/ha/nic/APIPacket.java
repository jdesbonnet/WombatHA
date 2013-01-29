package ie.wombat.ha.nic;

public class APIPacket {
	
	public static final int FROM_UART_TO_SERVER = 1;
	public static final int FROM_SERVER_TO_UART = 2;
	public static final int STATUS_INQUEUE = 1;
	public static final int STATUS_DELIVERED = 2;
	public static final int STATUS_TIMEOUT = 3;
	
	public long timestamp = System.currentTimeMillis();
	public byte[] payload;
	public int direction;
	public int status;
	
	public APIPacket() {
		
	}
	public APIPacket (byte[] payload) {
		this.payload = payload;
	}
}