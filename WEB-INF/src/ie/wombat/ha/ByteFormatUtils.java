package ie.wombat.ha;

public class ByteFormatUtils {

	/**
	 * Convert a string of hex digits into a byte[] array. Spaces 
	 * in string are ignored, but otherwise must be 0-9, a-f, A-F.
	 * 
	 * @param byteString 
	 * @return Byte array of length = number of hex digits / 2.
	 */
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
	
	/**
	 * Convert a byte[] to a string of hex digits. 
	 * 
	 * @param bytes
	 * @return String of hex digits where length = size of byte array * 2
	 */
	public static String byteArrayToString(byte[] bytes) {		
		return byteArrayToString(bytes,0,bytes.length);
	}
	
	/**
	 * Convert a byte[] to a string of hex digits specifying a offset into 
	 * the array and a length. If length exceeds the size of the byte array
	 * then length is reduced so that it corresponds to the end of the array.
	 * 
	 * @param bytes
	 * @return String of hex digits
	 */
	public static String byteArrayToString(byte[] bytes, int offset, int len) {
		if (bytes == null) {
			return "(null)";
		}
		if (bytes.length==0) {
			return "";
		}
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
	
	/**
	 * Return 2 character hex representation of the parameter. 
	 * 
	 * @param b
	 * @return
	 */
	public static String formatHexByte (int b) {
		b &= 0xff;
		if (b < 16) {
			return "0" + Integer.toHexString(b);
		}
		return Integer.toHexString(b);
	}
	
	/**
	 * Return 8 character binary representation of the parameter. 
	 * 
	 * @param b
	 * @return
	 */
	public static String formatBinaryByte (int b) {
		StringBuffer buf = new StringBuffer();
		for (int i = 0; i < 8; i++) {
			buf.append ( (b&0x80) != 0 ? "1":"0");
			b <<= 1;
		}
		return buf.toString();
	}
}
