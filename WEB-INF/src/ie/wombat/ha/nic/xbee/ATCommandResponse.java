package ie.wombat.ha.nic.xbee;

public interface ATCommandResponse {

	public static final int OK = 0x00;
	public static final int ERROR = 0x01;
	public static final int INVALID_COMMAND = 0x02;
	public static final int INVALID_PARAM = 0x03;
	public static final int TX_FAIL = 0x04;
	
	public void handleResponse (int status, byte[] packet, int packetLen);
}
