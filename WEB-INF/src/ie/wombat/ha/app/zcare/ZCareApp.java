package ie.wombat.ha.app.zcare;

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
import ie.wombat.ha.devices.CleodeZCare;
import ie.wombat.ha.devices.CleodeZPlug;
import ie.wombat.ha.devices.CleodeZRC;
import ie.wombat.ha.devices.XBeeSeries2;
import ie.wombat.ha.server.DataLogRecord;
import ie.wombat.ha.ui.client.Data;

import ie.wombat.zigbee.zcl.AttributeResponseDecode;
import ie.wombat.zigbee.zcl.AttributeValue;

public class ZCareApp extends AppBase  {

	
	private static Logger log = Logger.getLogger(ZCareApp.class);
	
	private CleodeZCare zcare;
	
	
	public ZCareApp(HANetwork network, String configuration) {	
		super(network,configuration);
		//System.err.println ("********** Starting " + XBeeIOApp.class.getName());

		log.info ("Starting application " + ZCareApp.class.getName());

		// Register interest in temperature sensor
		String zcareAddress = getParameter("zcare");
		
		zcare = (CleodeZCare)network.getDevice(zcareAddress);
		if (zcare == null) {
			log.error ("ZCare not found");
		}
		log.debug("zcareAddress=" + zcareAddress);

		
		TaskPollZCare temperaturePollTask = new TaskPollZCare(this, zcare);
		setRepeatingTask(temperaturePollTask, 300);
		//network.getNIC().addZigBeePacketListener(this);
	}


	public Date getExpiryTime() {
		return null;
	}


	public void setExpiryTime(Date expire) {		
	}
	
}
