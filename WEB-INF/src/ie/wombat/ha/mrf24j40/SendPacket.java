package ie.wombat.ha.mrf24j40;

public class SendPacket {

	public static void main (String[] arg) throws Exception {
		
		int i;
		int baudRate = Integer.parseInt(arg[1]);
		MRF24J40 rf = new MRF24J40(arg[0], baudRate, 20);
		
		rf.reset();
		rf.setChannel(20);
		
		StringBuffer sbuf = new StringBuffer();
		
		for (i = 2; i < arg.length; i++) {
			sbuf.append(arg[i]);
		}
		
		String s = sbuf.toString();
		
		// Remove spaces
		s = s.replaceAll("\\s", "");
		
		if (s.length() % 2 != 0) {
			System.err.println ("ERROR: packet must be multiples of 2 hex digits");
			return;
		}
		
		System.err.println ("Sending: " + s);
		
		int len = s.length() / 2;
		
		byte[] packet = new byte [len];
		
		for (i = 0; i < len; i++) {
			packet[i]  = (byte)Integer.parseInt(s.substring(i*2,i*2+2), 16);
		}
		
		rf.sendPacket(packet);
		
		rf.close();
	}
}
