package ie.wombat.ha.ui.server;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import org.apache.log4j.Logger;

import net.zschech.gwt.comet.server.CometSession;

import ie.wombat.ha.ByteFormatUtils;
import ie.wombat.ha.HANetwork;
import ie.wombat.ha.ZigBeeNIC;
import ie.wombat.ha.nic.APIFrameListener;
import ie.wombat.ha.nic.xbee.XBeeDriver;

public class NICListener implements APIFrameListener {
	
	private static Logger log = Logger.getLogger(NICListener.class);
	private static final SimpleDateFormat df = new SimpleDateFormat("yyyyMMdd HH:mm:ss");

	private static NICListener instance = new NICListener();
	
	private NICListener () {
		ZigBeeNIC nic = HANetwork.getInstance(2L).getNIC();
		nic.addAPIPacketListener(this);
	}
	public static NICListener getInstance() {
		return instance;
	}
	
	ArrayList<CometSession> cometSessions = new ArrayList<CometSession>();

	
	public void registerMessageSink (CometSession cometSession) {
		log.debug ("registering message sink" + cometSession);
		cometSessions.add(cometSession);
	}
	
	public Date getExpiryTime() {
		return null;
	}

	public void setExpiryTime(Date expire) {		
	}

	public void handleAPIFrame(byte[] packet, int packetLen) {
		
		//NICAPIPacket apiPacket = new NICAPIPacket();
		//apiPacket.timestamp = df.format(new Date());
		//apiPacket.packetHex = ByteFormatUtils.byteArrayToString(packet,0,packetLen);
		
		//System.err.println ("SENDING " + apiPacket.toString() );
		
		int packetType = (packet[0] & 0xff);

		
		StringBuffer buf = new StringBuffer();
		buf.append ("<tt class='xa_" + Integer.toHexString(packetType) + "'>");
		buf.append (df.format(new Date()));
		buf.append ("    ");
		
		
		if (packetType == 0x91) {
			buf.append (ByteFormatUtils.byteArrayToString(packet,0,1));
			buf.append (" [");
			buf.append (ByteFormatUtils.byteArrayToString(packet,1,8)); // addr64
			buf.append ("] ");
			buf.append (ByteFormatUtils.byteArrayToString(packet,9,4)); // addr16, srcEp, dstEp
			buf.append (" [");
			buf.append (ByteFormatUtils.byteArrayToString(packet,13,2));
			buf.append ("] [");
			buf.append (ByteFormatUtils.byteArrayToString(packet,15,2));
			buf.append ("] ");
			buf.append (ByteFormatUtils.byteArrayToString(packet,17,1));

			buf.append ("  :  ");
			buf.append (ByteFormatUtils.byteArrayToString(packet,18,packetLen-18));
		} else if (packetType == 0x11) {
			//buf.append (ByteFormatUtils.byteArrayToString(packet,0,20));
			
			
			buf.append (ByteFormatUtils.byteArrayToString(packet,0,2));
			buf.append (" [");
			buf.append (ByteFormatUtils.byteArrayToString(packet,2,8));
			buf.append ("] ");
			buf.append (ByteFormatUtils.byteArrayToString(packet,10,4)); // addr16, srcEp, dstEp
			buf.append (" [");
			buf.append (ByteFormatUtils.byteArrayToString(packet,14,2)); // Cluster ID
			buf.append ("] [");
			buf.append (ByteFormatUtils.byteArrayToString(packet,16,2)); // Profile ID
			buf.append ("] ");
			buf.append (ByteFormatUtils.byteArrayToString(packet,18,2)); // Bcast Radius, TX opts

			
			buf.append ("  :  ");
			buf.append (ByteFormatUtils.byteArrayToString(packet,20,packetLen-20));
		} else {
			buf.append (ByteFormatUtils.byteArrayToString(packet,0,packetLen));
		}
		buf.append("</tt>");
		
		String logLine = buf.toString();
		
		for (CometSession cometSession : cometSessions) {
			cometSession.enqueue(logLine);
		}
	}

}
