package ie.wombat.ha;

import javax.crypto.*;
import javax.crypto.spec.*;


public class ZigBeeAES {

	/**
	 * Turns array of bytes into string
	 * 
	 * @param buf
	 *            Array of bytes to convert to hex string
	 * @return Generated hex string
	 */
	public static String asHex(byte buf[]) {
		StringBuffer strbuf = new StringBuffer(buf.length * 2);
		int i;

		for (i = 0; i < buf.length; i++) {
			if (((int) buf[i] & 0xff) < 0x10)
				strbuf.append("0");

			strbuf.append(Long.toString((int) buf[i] & 0xff, 16));
		}

		return strbuf.toString();
	}
	
	public static byte[] hexToBytes (String s) {
		String[] p = s.split("\\s");
		StringBuffer buf = new StringBuffer();
		for (String st : p) {
			buf.append(st);
		}
		int i,v;
		byte[] ret = new byte[buf.length()/2];
		for (i = 0; i < ret.length; i++){
			v = Integer.parseInt(buf.substring(i*2,i*2+2),16);
			ret[i] = (byte)v;
		}
		
		return ret;
		
	}

	public static void main(String[] arg) throws Exception {

		byte[] key = hexToBytes(arg[0]);
		byte[] msg = hexToBytes(arg[1]);
		
		SecretKeySpec skeySpec = new SecretKeySpec(key, "AES");

		// Instantiate the cipher

		Cipher cipher = Cipher.getInstance("AES");

		//cipher.init(Cipher.ENCRYPT_MODE, skeySpec);
		//byte[] encrypted = cipher.doFinal(msg);
		//System.out.println("encrypted string: " + asHex(encrypted));

		cipher.init(Cipher.DECRYPT_MODE, skeySpec);
		byte[] original = cipher.doFinal(msg);
		//String originalString = new String(original);
		System.out.println("Original string: " + asHex(original));
	}
}
