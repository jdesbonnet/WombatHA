package ie.wombat.ha.app.xbeesensor;

/**
 * The polynomial used in the SHT1x/7x sensor generation is 
 * the following: x^8 + x^5 + x^4 + 1. This is the same
 * as that used in the Dallas/Maxim 1-wire protocol.
 * Advantage of this checksum over other version of CRC8 is 
 * there is no need for bit shifting.
 * 
 * http://www.sensirion.com/en/pdf/product_information/CRC_Calculation_Humidity_Sensor_E.pdf
 * @author joe
 *
 */
public class CRC8 {
	private int crc=0;
	
	int[] crc8Table = {0, 49, 
	98, 83, 196, 245, 166, 151, 185, 136, 219, 
	234, 125, 76, 31, 46, 67, 114, 33, 16, 135, 
	182, 229, 212, 250, 203, 152, 169, 62, 15, 
	92, 109, 134, 183, 228, 213, 66, 115, 32, 
	17, 63, 14, 93,108, 251, 202, 153, 168, 
	197, 244, 167, 150, 1, 48, 99, 82, 124, 77, 
	30, 47, 184, 137,  218, 235, 61, 12, 95, 
	110, 249, 200, 155, 170, 132, 181, 230, 
	215, 64, 113, 34, 19, 126, 79, 28, 45, 186, 
	139, 216, 233, 199, 246, 165, 148, 3, 50, 
	97, 80, 187, 138, 217, 232, 127, 78, 29, 
	44, 2, 51, 96, 81, 198, 247, 164, 149, 248, 
	201, 154, 171, 60, 13,  94, 111, 65, 112, 
	35, 18, 133, 180, 231, 214, 122, 75, 24,
	41, 190, 143, 220, 237, 195, 242, 161, 144,
	7, 54, 101, 84, 57, 8, 91, 106, 253, 204,
	159, 174, 128, 177, 226, 211, 68, 117, 38,
	23, 252, 205, 158, 175, 56, 9, 90, 107, 69,
	116, 39, 22, 129, 176, 227, 210, 191, 142,
	221, 236, 123, 74, 25, 40, 6, 55, 100, 85,
	194, 243, 160, 145, 71, 118, 37, 20, 131,
	178, 225, 208, 254, 207, 156, 173, 58, 11,
	88, 105, 4, 53, 102, 87, 192, 241, 162,
	147, 189, 140, 223, 238, 121, 72, 27, 42,
	193, 240, 163, 146, 5, 52, 103, 86, 120,
	73, 26, 43, 188, 141, 222, 239, 130, 179,
	224, 209, 70, 119, 36, 21, 59, 10, 89, 104,
	255, 206, 157, 172};
	
	public void addByteTable(int data) {
		int i = data^crc;
		System.err.println ("add byte 0x" + Integer.toHexString(data) 
				+ " data^crc=0x" + Integer.toHexString(i)
				+ " LUT[0x" + Integer.toHexString(i) + "]=" + Integer.toHexString(crc8Table[i])
				+ " (" + crc8Table[i] + " dec)"
				);	
		crc = crc8Table[data ^ crc];
	}
	
	
	public int addByte (int data) {
		  int i = (data ^ crc) & 0xff;
		  crc = 0;
		  if ( (i & 0x01) != 0) crc ^= 0x5e;
		  if ( (i & 0x02) != 0) crc ^= 0xbc;
		  if ( (i & 0x04) != 0) crc ^= 0x61;
		  if ( (i & 0x08) != 0) crc ^= 0xc2;
		  if ( (i & 0x10) != 0) crc ^= 0x9d;
		  if ( (i & 0x20) != 0) crc ^= 0x23;
		  if ( (i & 0x40) != 0) crc ^= 0x46;
		  if ( (i & 0x80) != 0) crc ^= 0x8c;
		  return crc;
	}
	
	public int getCrc() {
		return crc;
	}
	
	public void resetCrc() {
		crc = 0;
	}
}
