package ie.wombat.zigbee.zcl;

public class ZCLUtil {

	public static String getStatusName(int zclStatus) {
		switch (zclStatus) {
		case ZCLStatus.SUCCESS:
			return "SUCCESS";
		case ZCLStatus.MALFORMED_COMMAND:
			return "MALFORMED_COMMAND";
		case ZCLStatus.UNSUP_CLUSTER_COMMAND:
			return "UNSUP_CLUSTER_COMMAND";
		case ZCLStatus.UNSUP_MANUF_GENERAL_COMMAND:
			return "UNSUP_MANUF_GENERAL_COMMAND";
		case ZCLStatus.INVALID_FIELD:
			return "INVALID_FIELD";
		default:
			return "UNKNOWN_STATUS_0x" + Integer.toHexString(zclStatus);
		}
	}
}
