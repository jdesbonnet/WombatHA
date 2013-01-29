package ie.wombat.ha.devices;

import java.io.IOException;
import java.util.Date;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import org.apache.log4j.Logger;

import ie.wombat.ha.ByteFormatUtils;
import ie.wombat.ha.ZigBeeNIC;
import ie.wombat.ha.ZigBeePacket;
import ie.wombat.ha.server.Device;
import ie.wombat.ha.server.Observe;
import ie.wombat.ha.server.ObserveData;
import ie.wombat.zigbee.ZigBeeException;
import ie.wombat.zigbee.address.Address16;
import ie.wombat.zigbee.address.Address64;
import ie.wombat.zigbee.zcl.AttributeResponseDecode;
import ie.wombat.zigbee.zcl.AttributeValue;
import ie.wombat.zigbee.zcl.Cluster;
import ie.wombat.zigbee.zcl.Profile;
import ie.wombat.zigbee.zcl.ZCLException;

public class CleodeZRC extends DeviceDriver implements TemperatureSensor {

	private Logger log = Logger.getLogger (CleodeZRC.class);
	
	public CleodeZRC(Address64 address64, Address16 address16, ZigBeeNIC nic) {
		super(address64, address16, nic);
	}
	

	public boolean isBatteryPowered () {
		return true;
	}

	/**
	 * Get temperature in degrees celsius. 
	 * @return
	 * @throws IOException 
	 * @throws ZCLException 
	 */
	public float getTemperature () throws ZigBeeException, IOException {
		log.debug ("getTemperature()");
		int c = getIntegerAttribute(Profile.HOME_AUTOMATION,
				Cluster.TEMPERATURE_SENSOR, 
				0x0a,  // Source EP 10
				0x02,  // Destination EP 2
				0x0000 // Attribute ID 0 (temperature)
				);
		log.debug ("getTemperature() returned value=" + c);
		return (float)c / 100f;
	}



	@Override
	public void handleZigBeePacket(ZigBeePacket packet) {
		super.handleZigBeePacket(packet);

		byte[] payload = packet.getPayload();
		log.debug (this + " handleZigBeePacket() payload=" + ByteFormatUtils.byteArrayToString(payload));
		
		//if (packet.getSourceEndPoint() != 2) {
		//	log.debug ("Expecting packet from EP=2, this packet from EP=" + packet.getSourceEndPoint());
		//	return;
		//}
		
		if (packet.getClusterId() != Cluster.TEMPERATURE_SENSOR) {
			log.debug ("Expecting packet from temperature sensor cluster, clusterId=0x" 
					+ Integer.toHexString(packet.getClusterId()));
			return;
		}

		// A report
		if (payload[2] == 0x0a) {
			
			log.debug("Got AttributeReport (0x0a) packet");
			List<AttributeValue> list = AttributeResponseDecode.decode(payload, 3, payload.length, false);
			log.debug("Found " + list.size() + " attributes");
		
			
			// Want to record this value. This must be a cheap operation. 
			// A device data logger.
			// Attributes: network, deviceId/addr, timestamp, key, value
			
			// ZRC returns temperature in 1e-2 degrees C, eg 2150 = 21.5C.
			float temperature = (float)(list.get(0).getIntValue()) / 100;
			
			saveData ("temperature", ""+temperature);
			
		}
		// A query ?
		if (payload[2] == 0x01) {
			log.debug("Got QueryResponse (0x01) packet");
			List<AttributeValue> list = AttributeResponseDecode.decode(payload, 3, payload.length);
			log.debug ("found " + list.size() + " attributes");
			for (AttributeValue attrVal : list) {
				log.debug ("attrID " + attrVal.getId());
				if (attrVal.getId()== 0x0000) {
					float temperature = (float)(attrVal.getIntValue()) / 100;
					saveData("temperature",""+temperature);
				}
			}
		}
	}
	
}
