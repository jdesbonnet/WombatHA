package ie.wombat.ha.devices;

import java.io.IOException;
import java.util.Date;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import org.apache.log4j.Logger;

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

/**
 * The new (April 2011) version of the Cleode ZRC.
 * 
 * @author joe
 *
 */
public class CleodeZRC2 extends CleodeZRC implements TemperatureSensor {

	private Logger log = Logger.getLogger (CleodeZRC2.class);
	
	public CleodeZRC2(Address64 address64, Address16 address16, ZigBeeNIC nic) {
		super(address64, address16, nic);
	}
	
	
	/**
	 * Get temperature in degrees celsius. 
	 * @return
	 * @throws IOException 
	 * @throws ZCLException 
	 */
	public float getTemperature () throws ZigBeeException, IOException {
		int c = getIntegerAttribute(Profile.HOME_AUTOMATION,
				Cluster.TEMPERATURE_SENSOR, 
				0x0a,  // Source EP 10
				0x06,  // Destination EP 6
				0x0000 // Attribute ID 0 (temperature)
				);
		return (float)c / 100f;
	}
	
}
