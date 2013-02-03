package ie.wombat.ha.app.xbee;

import java.util.Date;
import java.util.List;

import javax.persistence.EntityManager;

import org.apache.log4j.Logger;

import ie.wombat.ha.ByteFormatUtils;
import ie.wombat.ha.HANetwork;
import ie.wombat.ha.HibernateUtil;
import ie.wombat.ha.ZigBeePacket;
import ie.wombat.ha.ZigBeePacketListener;
import ie.wombat.ha.app.AppBase;
import ie.wombat.ha.devices.CleodeZPlug;
import ie.wombat.ha.devices.CleodeZRC;
import ie.wombat.ha.devices.XBeeSeries2;
import ie.wombat.ha.server.DataLogRecord;
import ie.wombat.ha.ui.client.Data;

import ie.wombat.zigbee.zcl.AttributeResponseDecode;
import ie.wombat.zigbee.zcl.AttributeValue;

public class XBeeIOApp extends AppBase implements ZigBeePacketListener  {

	
	private static Logger log = Logger.getLogger(XBeeIOApp.class);
	
	private XBeeSeries2 xbee;
	
	private byte[] buf = new byte[128];
	private int bufptr = 0;
	private int last_tbc = 0;
	private long lastTbcWritten = 0;
	
	public XBeeIOApp(HANetwork network, String configuration) {	
		super(network,configuration);
		//System.err.println ("********** Starting " + XBeeIOApp.class.getName());

		log.info ("Starting application " + XBeeIOApp.class.getName());

		// Register interest in temperature sensor
		String xbeeAddress = getParameter("xbee");
		
		xbee = (XBeeSeries2)network.getDevice(xbeeAddress);
		if (xbee == null) {
			log.error ("XBee not found");
		}
		log.debug("xbeeAddress=" + xbeeAddress + " zrc=" + xbee);

		
		TaskPollXBee temperaturePollTask = new TaskPollXBee(this, xbee);
		setRepeatingTask(temperaturePollTask, 120);
		network.getNIC().addZigBeePacketListener(this);
		
	}


	public void handleZigBeePacket(ZigBeePacket packet) {

		// Disable check for Address16 because we cannot rely on it.
		/*
		if ( ! packet.getSourceAddress16().equals(xbee.getAddress16())) {
			log.debug ("ignoring packet, expecting src=" + xbee.getAddress16() + " but got " + packet.getSourceAddress16());
			return;
		}
		*/
		
		log.debug ("received ZigBee packet for evaluation: payload=" 
		+ ByteFormatUtils.byteArrayToString(packet.getPayload()));
		
		byte[] payload = packet.getPayload();
		
		int srcEp = packet.getSourceEndPoint();
		int clusterId = packet.getClusterId();
		
		// Is this response to AT%V?
		if (srcEp == 230 && clusterId==0xa1 && payload.length >= 6) {
			
			if (payload[1] == 0x25 && payload[2] == 0x56) {
				int vi =  ((payload[4]&0xff)<<8) | (payload[5]&0xff);
				double vmv = Math.floor((double)vi * 1200 / 1024);
				double v = vmv/1000;
				System.err.println ("******** v=" + v + "V");
				logEvent("V_ADC",""+vi);
				logEvent("V",""+v);

			}
			return;
		}
		
		
		if (srcEp != 232) {
			log.debug("ignoring packet because not from EP 232, srcEp=" + srcEp);
			return;
		}
		
		if (clusterId != 0x11) {
			log.debug("ignoring packet because not from cluster 0x11 (transparent IO mode), clusterId=" + clusterId);
			return;
		}
		//System.err.println ("******************* DATA PACKET RECEIVED  ***********");

		
		
		
	
		// add to buffer
		if ( (bufptr + payload.length) > buf.length) {
			log.error ("buffer overflow error, buffer=" + new String(buf,0,bufptr));
			bufptr=0;
		} else {
			System.err.println ("**** adding " + new String(payload) + " to buffer");
			System.arraycopy(payload, 0, buf, bufptr, payload.length);
			bufptr += payload.length;
			if ( (bufptr > 0) && (buf[bufptr-1] == 0x10) ) {
				String record =  new String(buf,0,bufptr-1);
				System.err.println ("*** record=" + record + " bufptr=" + bufptr);
				String[] p = record.split(" ");
				if (p.length == 3) {	
					logEvent(p[0]+"_ADC", p[1]+ " " + p[2]);
				}
				
		
				if ("SHT75_T".equals(p[0])) {
					//String hex = new String(buf,1,4);
					int t = Integer.parseInt(p[1], 16);
					double tf = -39.6 + 0.01*t;
					logEvent("SHT75_T",""+tf);
					System.err.println ("***SHT75 t=" + tf);
				}
				if ("SHT75_H".equals(p[0])) {
					//String hex = new String(buf,1,4);
					int h = Integer.parseInt(p[1], 16);
					double hf = -2.0468 + 0.0367*h - 1.5955e-6*h*h;
					logEvent("SHT75_H",""+hf);
					System.err.println ("***SHT75 h=" + hf);
				}
				if ("TBC".equals(p[0])) {
					//String hex = new String(buf,1,4);
					int tbc = Integer.parseInt(p[1], 16);
					if (tbc != last_tbc  || ((System.currentTimeMillis()-lastTbcWritten)>3600000L) ) {
						logEvent("TBC",""+tbc);
						System.err.println ("***TBC="+tbc);
						last_tbc = tbc;
						lastTbcWritten = System.currentTimeMillis();
					} else {
						System.err.println ("***TBC="+tbc+ " (same as before)");
					}
				}
				
				bufptr = 0;

			}
				
			
		}
		
		
	}


	public Date getExpiryTime() {
		// TODO Auto-generated method stub
		return null;
	}


	public void setExpiryTime(Date expire) {
		// TODO Auto-generated method stub
		
	}
	
}
