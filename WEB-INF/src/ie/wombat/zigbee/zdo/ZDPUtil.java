package ie.wombat.zigbee.zdo;

public class ZDPUtil {

	public static String getStatusName(int zdpStatus) {
		
		switch (zdpStatus) {
		case ZDPStatus.SUCCESS:
			return "SUCCESS";
		case ZDPStatus.INV_REQUESTTYPE:
			return "INV_REQUESTTYPE";
		case ZDPStatus.DEVICE_NOT_FOUND:
			return "DEVICE_NOT_FOUND";
		case ZDPStatus.INVALID_EP:
			return "INVALID_EP";
		case ZDPStatus.NOT_ACTIVE:
			return "NOT_ACTIVE";
		case ZDPStatus.NOT_SUPPORTED:
			return "NOT_SUPPORTED";
		case ZDPStatus.TIMEOUT:
			return "TIMEOUT";
		default:
			return "ZDP_STATUS_0x" + Integer.toHexString(zdpStatus);
		}
	}
}
