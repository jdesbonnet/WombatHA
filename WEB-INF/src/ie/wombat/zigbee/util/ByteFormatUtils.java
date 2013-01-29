package ie.wombat.zigbee.util;

/**
 * Duplicate of ie.wombat.ha.ByteFormatUtils. Copied here to accommodate compilation under GWT.
 * 
 * @author joe
 *
 */
public class ByteFormatUtils {

	public static byte[] stringToByteArray(String byteString) {
		
		// Remove white space
		byteString = byteString.replaceAll("\\s", "");
		
		
		int n = byteString.length()/2;
		byte[] ret = new byte[n];
		for (int i = 0; i < n; i++) {
			ret[i] = (byte)Integer.parseInt(byteString.substring(i*2,i*2+2), 16);
		}
		return ret;
	}
	public static String byteArrayToString(byte[] bytes) {
		/*
		StringBuffer buf = new StringBuffer();
		for (int i = 0; i < bytes.length; i++) {
			buf.append(formatHexByte(bytes[i]));
			buf.append(" ");
		}
		buf.setLength(buf.length()-1);
		return buf.toString();
		*/
		return byteArrayToString(bytes,0,bytes.length);
	}
	public static String byteArrayToString(byte[] bytes, int offset, int len) {
		StringBuffer buf = new StringBuffer();
		int end = offset + len;
		if (end > bytes.length) {
			end = bytes.length;
		}
		for (int i = offset; i < end; i++) {
			buf.append(formatHexByte(bytes[i]));
			buf.append(" ");
		}
		buf.setLength(buf.length()-1);
		return buf.toString();
	}
	public static String formatHexByte (int b) {
		b &= 0xff;
		if (b < 16) {
			return "0" + Integer.toHexString(b).toUpperCase();
		}
		return Integer.toHexString(b).toUpperCase();
	}
	public static String formatBinaryByte (int b) {
		StringBuffer buf = new StringBuffer();
		for (int i = 0; i < 8; i++) {
			buf.append ( (b&0x80) != 0 ? "1":"0");
			b <<= 1;
		}
		return buf.toString();
	}
}
