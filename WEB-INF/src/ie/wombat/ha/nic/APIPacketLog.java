package ie.wombat.ha.nic;


import java.util.ArrayList;
import java.util.List;

public class APIPacketLog {

	private static final int MAX_SIZE = 40;
	
	private static ArrayList<APIPacket> packetLog = new ArrayList<APIPacket>();
	
	public static void addPacket (APIPacket packet) {
		packetLog.add(packet);
		if (packetLog.size() > MAX_SIZE) {
			packetLog.remove(0);
		}
	}
	public static List<APIPacket>getPackets () {
		return packetLog;
	}
}
