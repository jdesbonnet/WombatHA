package ie.wombat.zigbee.zcl;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

/**
 * Decode a ZigBee read attributes response packet into a list of 
 * {@link AttributeValue} 
 *  
 * @author joe
 *
 */
public class AttributeResponseDecode {
	
	private static Logger log = Logger.getLogger(AttributeResponseDecode.class);

	/**
	 * 
	 * @param packet
	 * @param ptr Point to start of attribute list
	 * @param packetLen Will not attempt to read beyond this point in packet[] array
	 * @return
	 */
	public static List<AttributeValue> decode(byte[] packet,
			int ptr, int packetLen) {
		return decode(packet, ptr, packetLen, true);
	}
	public static List<AttributeValue> decode(byte[] packet,
				int ptr, int packetLen, boolean expectAttrStatus) {
		int  attrStatus;
		
		log.debug("decode(): ptr=" + ptr + " packetLen=" + packetLen);
		
		List<AttributeValue> ret = new ArrayList<AttributeValue>();

		while (ptr <= packetLen-4) {
			
			AttributeValue v = new AttributeValue();

			// LSB first
			int attrId =  packet[ptr + 1] << 8;
			attrId |= (packet[ptr] & 0xff);
			attrId &= 0xffff;
			v.setId(attrId);
			
			log.debug("decode(): attrId=0x" + Integer.toHexString(attrId));
			ptr += 2;
			
			if (expectAttrStatus) {
				attrStatus = packet[ptr++];
				if (attrStatus != 0x00) {
					log.error("decode(): error status " + formatHexByte(attrStatus));
					// ZCL Spec, page 64
					// 0x00 Success
					// 0x01 Fail
					// 0x80 Malformed command
					// 0x85 Invalid field
					// 0x86 Unsupported attribute
					continue;
				}
			}
			

			v.setType(packet[ptr++] & 0xff);
			log.debug("decode(): data type=0x" + formatHexByte(v.type));

			switch (v.type) {
				case ZCLDataType.BINARY: // binary
					log.debug(" (boolean) ");
					v.setIntValue(packet[ptr++]);
					break;
				case ZCLDataType.BITMAP8:
					log.debug(" (bitmap8) ");
					v.setIntValue(packet[ptr++]);
					break;
				case ZCLDataType.BITMAP16: // 16 bit map
					log.debug(" (bitmap16) ");
					v.intValue = packet[ptr + 1] << 8;
					v.intValue |= (packet[ptr] & 0xff);
					ptr += 2;
					break;
				case ZCLDataType.UINT8:
					log.debug(" (uint8) ");
					v.setIntValue(packet[ptr++] & 0xff);
					break;
				case ZCLDataType.UINT16: 
					log.debug(" (uint16) ");
					v.intValue = packet[ptr + 1] << 8;
					v.intValue |= (packet[ptr] & 0xff);
					ptr += 2;
					break;
				case ZCLDataType.UINT48:
					log.debug (" (uint48) ");
					v.intValue = packet[ptr + 1] << 8;
					v.intValue |= (packet[ptr] & 0xff);
					// TODO: not complete. Need long.
					ptr += 6;
					break;
				case ZCLDataType.INT16:
					log.debug(" (int16) ");
					v.setIntValue ( (packet[ptr + 1] << 8) | (packet[ptr] & 0xff) );
					ptr += 2;
					break;
				case ZCLDataType.ENUM8: // 8 bit enum
					log.debug(" (enum8) ");
					v.setIntValue(packet[ptr++] & 0xff);
					break;
				case ZCLDataType.ENUM16: // 8 bit enum
					log.debug(" (enum16) ");
					v.intValue = packet[ptr + 1] << 8;
					v.intValue |= (packet[ptr] & 0xff);
					ptr += 2;
					break;
				case ZCLDataType.STRING: // char string (len = first octet)
				{
					log.debug(" (string) ");
					int len = (int)packet[ptr++] & 0xff;
					String s = new String(packet,ptr,len);
					System.err.println ("value=" + s);
					ptr += len;
					v.setStringValue(s);
					break;
				}
				default:
					log.warn(" (unknown ZCL datatype 0x" + Integer.toHexString(v.type) +") ");
					
			}
			
			log.debug(" value=" + v.intValue);

			ret.add(v);
		}

		return ret;

	}

	public static String formatHexByte(int b) {
		b &= 0xff;
		if (b < 16) {
			return "0" + Integer.toHexString(b);
		}
		return Integer.toHexString(b);
	}

}
