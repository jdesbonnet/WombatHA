package ie.wombat.ha;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;

/**
 * Read from Microchip ZENA (as USB HID device, eg attached to /dev/usb/hiddev0)
 * and write ZigBee packets to pcap file.
 * 
 * I currently do not know how to configure the Zena (eg set radio channel). So
 * will need to power up Zena software in VM, set the channel and then
 * disconnect the USB device from the VM.
 * 
 * cat /dev/usb/hiddev0 | (this program)
 * 
 * Packets from ZENA are formmated as follows:
 * Byte 0: ? always 0x00
 * Byte 1: timestamp bits 0 - 7
 * Byte 2: timestamp bits 8 - 15
 * Byte 3: timestamp bits 16 - 23
 * Byte 4: ? always 0x00
 * Byte 5: 802.15.4 packet payload length
 * Byte 6: 802.15.4 packet byte 0
 * ...
 * 
 * 
 */
public class Zena {

	private static final int PCAP_MAGIC = 0xa1b2c3d4;
	private static final int PCAP_VERSION_MAJOR = 2;
	private static final int PCAP_VERSION_MINOR = 4;

	private static final boolean PCAP_EN = false;

	// ISO8601 date format
	private static SimpleDateFormat df = new SimpleDateFormat(
			"yyyy-MM-dd'T'HH:mm:ssz");
	
	private static long lastByteTimestamp;

	public static void main(String[] arg) throws Exception {

		byte[] buf = new byte[4096];

		DataOutputStream dout = new DataOutputStream(System.out);

		if (PCAP_EN) {
			// Write pcap header
			dout.writeInt(PCAP_MAGIC);
			dout.writeShort(PCAP_VERSION_MAJOR);
			dout.writeShort(PCAP_VERSION_MINOR);
			dout.writeInt(0); /* thiszone: GMT to local correction */
			dout.writeInt(0); /* sigfigs: accuracy of timestamps */
			dout.writeInt(4096); /* snaplen: max len of packets, in octets */
			dout.writeInt(0xc3); /* data link type */
		}

		DataInputStream din = new DataInputStream(System.in);

		
		int d,ts,len,i;
		while (true) {
			
			// First byte is 0x00
System.err.println ("looking for start of packet");

			//while ( (d = readZenaByte(din) ) != 0) ;
			readZenaByte(din);
			

//System.err.println ("found possible start of packet");
			ts = readZenaByte(din);
			ts |= readZenaByte(din)<<8;
			ts |= readZenaByte(din)<<16;
			
//System.err.println ("ts=" + ts);
			
			d = readZenaByte(din);
			if (d != 0x00) {
				System.err.println ("Expecting 0x00, but got " + Integer.toHexString(d&0xff));
				//continue;
			}
			
			len = readZenaByte(din);
			System.err.print ("len=" + len + " ");
			
			for (i = 0; i < len; i++) {
				buf[i] = (byte)readZenaByte(din);
			}
			
			writePacket(dout, buf, len);

		}

	}
	private static int readZenaByte(DataInputStream din) throws IOException {
		
		int c = din.readInt();
		
		// If c is 0x030000ff then next 4 bytes from HID device has one 
		// byte of packet data in most significant 8 bits (bits 31->24)
		if (c != 0x030000ff) {
			throw new IOException ("Expecting 0x030000ff");
		}
System.err.print ("*");
System.err.flush();
		//return (byte) (din.readInt() >> 24);

		lastByteTimestamp = System.currentTimeMillis();
		return (din.readInt() >> 24);
	}

	private static void writePacket(DataOutputStream dout, byte[] buf, int n)
			throws IOException {
		long t = System.currentTimeMillis();

		String ts = df.format(t);
		// Strip trailing "GMT" from ts string
		ts = ts.substring(0, 19) + ts.substring(22, ts.length()); 
		
		
		System.err.print(ts + " ");
		System.err.print(Integer.toHexString(n) + " ");

		//for (int i = 6; i < len + 6; i++) {
		for (int i = 0; i < n; i++) {
			if ((buf[i] & 0xff) < 16) {
				System.err.print("0");
			} else {
				System.err.print("");
			}
			System.err.print(Integer.toHexString(buf[i] & 0xff));
		}
		System.err.println("");
		System.err.flush();

		if (PCAP_EN) {
			
			// ts_sec: timestamp seconds
			dout.writeInt((int) (t / 1000L));
			
			// ts_usec: timestamp microseconds
			dout.writeInt((int) ((t % 1000L) * 1000L));

			// incl_len: number of octets of packet saved in file
			dout.writeInt(n);

			// orig_len: actual length of packet
			dout.writeInt(n);

			dout.write(buf, 0, n);
		}
	}
}
