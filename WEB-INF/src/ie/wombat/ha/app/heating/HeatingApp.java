package ie.wombat.ha.app.heating;

import java.io.IOException;
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
import ie.wombat.ha.devices.CleodeZRC;
import ie.wombat.ha.devices.HeatingDevice;
import ie.wombat.ha.devices.TemperatureSensor;
import ie.wombat.ha.server.DataLogRecord;
import ie.wombat.ha.ui.client.Data;

import ie.wombat.zigbee.zcl.AttributeResponseDecode;
import ie.wombat.zigbee.zcl.AttributeValue;

/**
 * Home heating application. 
 * 
 * Configuration parameters:
 * <ul>
 * <li>heater: heater relay device 64 bit IEEE address
 * <li>sensor_0: Zone[0] temperature sensor 64 bit IEEE address
 * <li>sensor_1: Zone[1] temperature sensor 64 bit IEEE address
 * <li>sensor_2: Zone[2] temperature sensor 64 bit IEEE address
 * <li>sensor_x: external temperature sensor 64 bit IEEE address
 * <li>ext_t_service: URL to retrieve external temperature
 * <li>ext_t_xpath: XPath query to retrieve external temperature in C.
 * </ul>
 * 
 * @author Joe Desbonnet, jdesbonnet@gmail.com
 *
 */
public class HeatingApp extends AppBase implements ZigBeePacketListener  {
    
	private static Logger log = Logger.getLogger(HeatingApp.class);

	
	/** If the temperature sensor does not automatically report temperature, set this
	 * to true to enable active polling of the sensor. */
	private static boolean USE_ACTIVE_POLL = true;
	
	// TODO: this should be read from saved state
	private static final float DEFAULT_TARGET_TEMPERATURE = 10.0f;
	
	/** Number of zones in the system */
	private int NZONE = 3;
	
	
	/** The zone's mode: ON, OFF, AUTO for each mode */
	private ZoneMode[] zoneMode = new ZoneMode[NZONE];
	
	/** The target temperature in C for each zone at the current point in time */
	private float[] targetTemperature  = new float[NZONE];
	
	// A sensor is located in each zone
	private TemperatureSensor[] sensor = new TemperatureSensor[NZONE];
	
	// A sensor outside (optional)
	private TemperatureSensor externalSensor;
	
	// One central heating controller
	private HeatingDevice heater;
	
	/** The current temperature for each time at the current point in time */
	private float[] temperature = new float[NZONE];
	
	/** The time at which the current temperature readings were taken */
	private long[] temperatureTime = new long[NZONE];
	
	/** The current external temperature */
	private float externalTemperature;
	
	/** The time at which the external temperature was last measured */
	private long externalTemperatureTime;
	
	/** The current state of the heater: true = ON, false = OFF */
	private boolean[] heatingState = new boolean[NZONE];
	
	/**
	 * HeatingApp constructor. Read from configuration.
	 * 
	 * @param network
	 * @param configuration
	 */
	public HeatingApp(HANetwork network, String configuration) {	
		super(network,configuration);
		
		log.info ("Starting " + HeatingApp.class.getName());

		String heaterAddress = getParameter("heater");
		if (heaterAddress != null) {
			heater = (HeatingDevice)network.getDevice(heaterAddress);
			log.debug ("heaterAddress=" + heaterAddress + " heater=" + heater);
		}
		
		String externalSensorAddr = getParameter("sensor_x");
		if (externalSensorAddr != null) {
			externalSensor = (CleodeZRC)network.getDevice(externalSensorAddr);
		}
		
		String externalTemperatureService = getParameter("ext_t_service");
		if (externalTemperatureService != null) {
			String xpathQuery = getParameter("ext_t_xpath");
			TaskGetInternetExtTemperature inetWeatherTask = new TaskGetInternetExtTemperature(this,
					externalTemperatureService, xpathQuery);
			setRepeatingTask(inetWeatherTask, 1800);
		}
		
		for (int i = 0; i < NZONE; i++) {
			
			zoneMode[i] = ZoneMode.AUTO;
			targetTemperature[i] = DEFAULT_TARGET_TEMPERATURE;
			
			// Register interest in temperature sensor. The temperature sensor is the Cleode
			// ZRC which features a TI TMP0?? temperature sensor on EP 2.
			String sensorAddress = getParameter("sensor_"+i);
			if (sensorAddress == null) {
				log.info("Skipping zone " + i + " as no sensor defined");
				continue;
			}
		
			sensor[i] = (CleodeZRC)network.getDevice(sensorAddress);
			if (sensor == null) {
				log.error ("temperature sensor not found");
			}
			log.debug("sensorAddress=" + sensorAddress + " sensor=" + sensor);

		
			
			if (USE_ACTIVE_POLL) {
				TaskGetTemperature temperaturePollTask = new TaskGetTemperature(this, i);
				setRepeatingTask(temperaturePollTask, 240);
			}
			
			
		}
		
		// Register the app as a packet listener
		network.getNIC().addZigBeePacketListener(this);

	}

	/**
	 * Return the zone mode (ON,OFF,AUTO).
	 * 
	 * @param zone
	 * @return
	 */
	public ZoneMode getZoneMode(int zone) {
		return zoneMode[zone];
	}
	
	/**
	 * Set the zone mode (ON,OFF,AUTO)
	 * @param zone
	 * @param setting
	 */
	public void setZoneMode(int zone, ZoneMode setting) throws IOException {
		
		log.debug(this + " setting zone " + zone + " mode to " + setting);
		logEvent("mode_zone_" + zone, setting.toString());

		
		zoneMode[zone] = setting;
		
		if (setting == ZoneMode.ON) {
			logEvent("heating_state_zone_" + zone, "1");
			heater.setState(zone, true);
			return;
		}
		if (setting == ZoneMode.OFF) {
			logEvent("heating_state_zone_" + zone, "0");
			heater.setState (zone, false);
		}

	}
	
	// TODO: need to make this abstract
	public TemperatureSensor getSensorDevice (int zone) {
		return sensor[zone];
	}
	public TemperatureSensor getExternalSensorDevice () {
		return externalSensor;
	}
	
	/**
	 * Called whenever a new temperature sensor reading is received.
	 * @param t
	 */
	public void notifyCurrentTemperature (int zone, float t) {
		temperature[zone] = t;
		temperatureTime[zone] = System.currentTimeMillis();
		
		if (zoneMode[zone] != ZoneMode.AUTO) {
			log.debug("Zone " + zone + " is not in AUTO. No actuation required.");
			return;
		}
		
		if (temperature[zone]>=targetTemperature[zone]) {
			log.debug ("temperature on or over target, turning heat off");
			logEvent("heating_state_zone_"+zone, "0");
			try {
				heater.setState(zone,false);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			if (heatingState[zone] == true) {
				heatingState[zone] = false;
				logEvent("heating_state_zone_" + zone, "0");
			}
		} else {
			log.debug ("temperature under target, turning heat on");
			logEvent("heating_state_zone_"+zone, "1");
			try {
				heater.setState(zone,true);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			if ( heatingState[zone] == false) {
				heatingState[zone] = true;
				logEvent("heating_state_zone_" + zone, "1");
			}
		}
		
		if (t < 10) {
			log.warn("!!!!!!!!!!!!!!! t="+t);
		}
	}
	public float getTemperature (int zone) {
		return temperature[zone];
	}
	public long getTemperatureTime (int zone) {
		return temperatureTime[zone];
	}
	public float getExternalTemperature() {
		return externalTemperature;
	}
	public long getExternalTemperatureTime () {
		return externalTemperatureTime;
	}
	
	public void setTargetTemperature (int zone, float t) {
		log.debug(this + " setting target temperature for zone " + zone + " to " + t);
		logEvent("set_target_temperature_zone_" + zone, "" + t);

		this.targetTemperature[zone] = t;
	}
	public float getTargetTemperature (int zone) {
		return this.targetTemperature[zone];
	}

	public Data[] getTemperatureData (int zone, Date startTime, Date endTime) {
		
		log.debug ("getTemperature(" +startTime + "," + endTime + ")");
		EntityManager em = HibernateUtil.getEntityManager();
		em.getTransaction().begin();
		
		List<DataLogRecord> records = em.createQuery("from DataLogRecord where address64=:addr64 "
				+ " and timestamp>=:startTime and timeStamp<:endTime")
				.setParameter("addr64", sensor[zone].getAddress64().toString())
				.setParameter("startTime",startTime)
				.setParameter("endTime",endTime)
				.getResultList();
		Data[] ret = new Data[records.size()];
		int i = 0;
		for (DataLogRecord record : records) {
			Data d = new Data();
			d.timestamp = record.getTimestamp();
			d.value = record.getValue();
			ret[i++] = d;
		}
		em.getTransaction().commit();
		
		log.debug ("returning " + ret.length + " records");
		return ret;
	}
	
	/**
	 * This turns the heater on/off for zone 'zone'. true=on, false=off.
	 * 
	 * @param zone
	 * @param b
	 */
	public void setHeatingState (int zone, boolean b) throws IOException {
		heater.setState(zone,b);
		// TODO: figure out logging
		logEvent("zone_" + zone, b ? "1" : "0");
	}
	
	
	/**
	 * Indicates if the heater is on/off for zone. If the heater is indicated on, it does not 
	 * guarantee that the it is outputting heat (a thermostat may override the flame).
	 * 
	 * @param zone
	 * @return
	 * @throws IOException 
	 */
	public boolean getHeatingState (int zone) throws IOException {
		return heater.getState(zone);
	}
	
	/**
	 * {@inheritDoc}
	 */
	public Date getExpiryTime() {
		return null;
	}

	/**
	 * {@inheritDoc}
	 */
	public void setExpiryTime(Date expire) {
		// ignore
	}

	public void handleZigBeePacket(ZigBeePacket packet) {

		int zone = -999;
		
		// is it the external sensor
		if (externalSensor!= null && packet.getSourceAddress16().equals(externalSensor.getAddress16())) {
			zone = -1;
		}
		for (int i = 0; i < NZONE; i++) {
			if (sensor[i] == null) {
				continue;
			}
			if (packet.getSourceAddress16().equals(sensor[i].getAddress16())) {
				zone = i;
				break;
			}
		}
		
		if (zone == -999) {
			// this packet is not for this app
			return;
		}
		
		log.debug ("received ZigBee packet for evaluation: zone= " + zone + " payload=" 
		+ ByteFormatUtils.byteArrayToString(packet.getPayload()));
		
		byte[] payload = packet.getPayload();
		
		if (payload.length < 3) {
			log.warn ("payload too short. Ignoring.");
			return;
		}
		
		if ( (payload[0] == 0x00 || payload[0] == 0x10) && payload[2] == 0x0a) {
			log.debug("Got AttributeReport (0x0a) packet");
			List<AttributeValue> list = AttributeResponseDecode.decode(payload, 3, payload.length, false);
			log.debug("Found " + list.size() + " attributes");
			if (list.size() != 1) {
				log.warn ("Expecting just one attribute value, received " + list.size());
				return;
			}
			AttributeValue v = list.get(0);
			float t = ((float)v.getIntValue())/100;
			
			
			log.info("zone=" + zone + " t=" + t);
			
			

			if (zone == -1) {
				externalTemperature = t;
				externalTemperatureTime = System.currentTimeMillis();
				logEvent("temperature_external", "" + t);

			} else {
				notifyCurrentTemperature(zone,t);
				logEvent("temperature_zone_" + zone, "" + t);
			}
			
		} else {
			log.debug ("Packet ignored");
		}
	}
	
}
