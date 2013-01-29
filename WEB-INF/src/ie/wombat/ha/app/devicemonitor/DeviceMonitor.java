package ie.wombat.ha.app.devicemonitor;

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
import ie.wombat.ha.app.heating.TaskGetTemperature;
import ie.wombat.ha.devices.CleodeZPlug;
import ie.wombat.ha.devices.CleodeZRC;
import ie.wombat.ha.server.DataLogRecord;
import ie.wombat.ha.ui.client.Data;

import ie.wombat.zigbee.zcl.AttributeResponseDecode;
import ie.wombat.zigbee.zcl.AttributeValue;

public class DeviceMonitor extends AppBase implements ZigBeePacketListener  {

	
	private static Logger log = Logger.getLogger(DeviceMonitor.class);
	
	public DeviceMonitor(HANetwork network, String configuration) {	
		super(network,configuration);
		
		log.info ("Starting " + DeviceMonitor.class.getName());

		TaskPollBatteryStatus pollTask = new TaskPollBatteryStatus(this);
		setRepeatingTask(pollTask, 600);
		
		TaskPollAddress16 pollAddr16Task = new TaskPollAddress16(this);
		setRepeatingTask(pollAddr16Task, 120);
		
		
		network.getNIC().addZigBeePacketListener(this);
		
	}

	
	public Date getExpiryTime() {
		return null;
	}

	public void setExpiryTime(Date expire) {
		// ignore
	}

	public void handleZigBeePacket(ZigBeePacket packet) {

	
		
		log.debug ("received ZigBee packet for evaluation: payload=" 
		+ ByteFormatUtils.byteArrayToString(packet.getPayload()));
		
		byte[] payload = packet.getPayload();
		
		if (payload.length < 3) {
			log.warn ("payload too short. Ignoring.");
			return;
		}
		
		
	}
	
}
