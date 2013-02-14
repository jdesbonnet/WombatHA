package ie.wombat.ha;

import ie.wombat.ha.nic.xbee.XBeeUtil;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;

public class DebugUtils {

	public static String formatXBeeAPIFrame (byte[] packet, int start, int len) {
		StringWriter sw = new StringWriter();
		try {
			displayXBeeAPIPacket(sw, packet, start, len);
		} catch (IOException e) {
			return "?error?";
		}
		return sw.toString();
	}
	
	public static void displayXBeeAPIPacket (Writer w, byte[] packet, int start, int len) throws IOException {
		
		int packetType = (int)packet[start] & 0xff;
		for (int i = start; i < start+len; i++) {
			w.write (ByteFormatUtils.formatHexByte(packet[i]) + " ");
		}
		w.write ("\n");
		
		switch (packetType) {
		case 0x88: // AT command response
			w.write ("AT command response to AT");
			w.write (packet[start+2]);
			w.write (packet[start+3]);
			break;
		case 0x90:
			w.write ("ReceivePacket");
			w.write (" addr64=");
			DebugUtils.writeAddr64MSBF(w,packet,start+1);
			w.write(" addr16=");
			DebugUtils.writeAddr16MSBF(w,packet,start+9);
			w.write(" opts=" + ByteFormatUtils.formatHexByte(packet[11]));
			XBeeUtil.writeData (w,packet,12,packet.length-12);
			break;
		case 0x91:
			w.write ("ExcplicitRxIndicator");
			w.write (" srcAddr64=");
			DebugUtils.writeAddr64MSBF(w,packet,start+1);
			
			w.write(" srcAddr16=");
			DebugUtils.writeAddr16MSBF(w,packet,start+9);
			//w.write(" a=0x" + Integer.toHexString(XBeeUtil.get16MSBFirst(packet, start+9)));
			
			w.write(" srcEp=" + ByteFormatUtils.formatHexByte(packet[11]));
			w.write(" dstEp=" + ByteFormatUtils.formatHexByte(packet[12]));
			
			int clusterId = XBeeUtil.get16MSBFirst(packet, start+13);
			w.write(" clusterId=0x" + Integer.toHexString(clusterId));
			
			int profileId = XBeeUtil.get16MSBFirst(packet, start+15);
			w.write(" profileId=0x" + Integer.toHexString(profileId));
			
			w.write(" opts=");
			int opts = packet[17];
			w.write(ByteFormatUtils.formatHexByte(packet[17]));
			w.write(" (");
			if ( (opts & 0x40) != 0) {
				w.write("EndDevice ");
			}
			if ( (opts & 0x20) != 0) {
				w.write("Encrypted ");
			}
			if ( (opts & 0x02) != 0) {
				w.write("Broadcast ");
			}
			if ( (opts & 0x01) != 0) {
				w.write("Ack ");
			}
			w.write (")");
			
			// Display first few bytes of data
			w.write("data=");
			int dataLen = len-18;
			if (dataLen>3) {
				XBeeUtil.writeData(w,packet,18,3);
				w.write ("...\n");
			} else {
				XBeeUtil.writeData(w,packet,18,dataLen);
			}
			
			//w.write ("\n");
			
			// Not sure what I'm doing here!
			if (profileId == 0) {
				XBeeUtil.displayZDOResponse(w, clusterId, packet, 18+1);
			}
			
			break;
		case 0x8B:
			w.write ("TransmitStatus ");
			w.write(" frameId=" + ByteFormatUtils.formatHexByte(packet[start+1]));

			w.write (" addr16=");
			DebugUtils.writeAddr16MSBF (w,packet,start+2);
			
			w.write(" retryCount=" + ByteFormatUtils.formatHexByte(packet[start+4]));
			
			int deliveryStatus = packet[start+5] &0xff;
			w.write(" deliveryStatus="
					+ getDeliveryStatusDescription(deliveryStatus)
					+ " (0x"
					+ ByteFormatUtils.formatHexByte(packet[start+5])
					+ ")");
			
			int discoveryStatus = packet[start+6] &0xff;
			w.write(" discoveryStatus="
					+ getDiscoveryStatusDescription(discoveryStatus)
					+ " (0x"
					+ ByteFormatUtils.formatHexByte(packet[start+6])
					+ ")"
					);
			
			break;
		}
		w.write ("\n");
	}

	public static void writeAddr64LSBF(Writer w, byte[]packet, int start) throws IOException {
		w.write (ByteFormatUtils.formatHexByte(packet[start+7]));
		for (int i = 6; i >= 0; i--) {
			w.write (":");
			w.write (ByteFormatUtils.formatHexByte(packet[start+i]));
		}
	}
	public static void writeAddr64MSBF(Writer w, byte[]packet, int start) throws IOException {
		w.write (ByteFormatUtils.formatHexByte(packet[start]));
		for (int i = 1; i < 8; i++) {
			w.write (":");
			w.write (ByteFormatUtils.formatHexByte(packet[start+i]));
		}
	}

	public static void writeAddr16LSBF (Writer w, byte[]packet, int start) throws IOException {
		w.write (ByteFormatUtils.formatHexByte(packet[start+1]));
		w.write (ByteFormatUtils.formatHexByte(packet[start]));
	}
	public static void writeAddr16MSBF (Writer w, byte[]packet, int start) throws IOException {
		w.write (ByteFormatUtils.formatHexByte(packet[start]));
		w.write (ByteFormatUtils.formatHexByte(packet[start+1]));
	}

	/**
	 * Return text description of XBee Transmit Status (0x8B). See page 111
	 * of documentation.
	 * 
	 * @param status
	 * @return
	 */
	public static final String getDeliveryStatusDescription (int status) {
		switch (status) {
		case 0x00:
			return "Success";
		case 0x01:
			return "MAC ACK fail";
		case 0x02: 
			return "CCA fail";
		case 0x15:
			return "Invalid Destination Endpoint";
		case 0x21:
			return "Network MAC fail";
		case 0x22:
			return "Not joined to network";
		case 0x23:
			return "Self-addressed";
		case 0x24:
			return "Address not found";
		case 0x25:
			return "Route not found";
		case 0x26:
			return "Broadcast source failed to hear neighbor relay message";
		case 0x2B:
			return "Invalid binding index";
		case 0x2C:
		case 0x32:
			return "Insufficient resources";
		case 0x2D:
			return "Attempt to broadcast with APS transmission";
		case 0x2E:
			return "Attempt to unicast with APS transmission but EE=0";
		case 0x74:
			return "Data payload too large";
		case 0x75:
			return "Indirect message unrequested";
		}
		return "Unknown";
	}
	public static final String getDiscoveryStatusDescription (int status) {
		switch (status) {
		case 0x00:
			return "No discovery overhead";
		case 0x01:
			return "Address discovery";
		case 0x02:
			return "Route discovery";
		case 0x03:
			return "Address and route discovery";
		case 0x40:
			return "Extended timeout discovery";
		}
		return "Unknown";
	}

	public static final String arrayJoin (int[] array, String sep) {
		if (array.length==0) {
			return "";
		}
		StringBuffer buf = new StringBuffer();
		buf.append("" + array[0]);
		for (int i = 1 ; i < array.length; i++) {
			buf.append(",");
			buf.append(""+array[i]);
		}
		return buf.toString();
	}
	
}
