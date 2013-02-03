package ie.wombat.ha.devices;

import java.io.IOException;

import ie.wombat.ha.ZigBeeNIC;
import ie.wombat.zigbee.ZigBeeException;
import ie.wombat.zigbee.address.Address16;
import ie.wombat.zigbee.address.Address64;

/**
 * Device driver for Digi XBee Sensor XS-Z16-CB2R.
 * 
 *
 * Temperature:
 * temp_C = (mVanalog - 500.0)/ 10.0
 * mVanalog = (ADC2/1023.0) * 1200
 *
 * Humidity:
 * hum = (((mVanalog * 108.2 / 33.2) / 5000 - 0.16) / 0.0062)
 * mVanalog = (ADC3/1023.0) * 1200
 *
 * Light:
 * lux = (ADC1) /1023.0) * 1200
 * 
 * @author Joe Desbonnet, jdesbonnet@gmail.com
 *
 */
public class DigiCB2R extends XBeeSeries2 implements TemperatureSensor, HumiditySensor, LightSensor {

	public static final int SENSOR_LIGHT=0;
	public static final int SENSOR_HUMIDITY=1;
	public static final int SENSOR_TEMPERATURE=2;
	

	public DigiCB2R(Address64 address64, Address16 address16,
			ZigBeeNIC nic) {
		super(address64, address16, nic);
	}

	@Override
	public boolean isBatteryPowered() {
		return true;
	}
	
	public int[] getADCReadings () throws IOException {
		byte[] response = execATQuery("IS");
		
		// Byte 0: API frame sequence ID
		// Byte 1,2: AT command ie 0x49 ('I'), 0x53('S')
		// Byte 3: always 0x00
		if (response[1] != 0x49 || response[2] != 0x53 || response[3] != 0x00) {
			throw new IOException ("Unexpected start to ATIS query response");
		}
		
		// AT Query response follows:
		// Byte 4: #sets(=0x01) 
		// Byte 5,6: DigitalIOMask
		// Byte 7: AnalogMask
		// Byte 8,9 (if DigitalIOMask!=0) else omitted
		// Byte 10,11 (or 8,9 if no digital IO): first ADC sample
		// Byte 12,13 (or 10,11 if no digital IO): second ADC sample 
		// etc
		if (response.length != 16) {
			throw new IOException ("Unexpected length ATIS query response. Expecting 16 bytes, received "
					 + response.length);
		}
		
		int[] v = new int[3];
		v[0] = (((int)response[10]&0xff)<<8) | (response[11]&0xff);
		v[1] = (((int)response[12]&0xff)<<8) | (response[13]&0xff);
		v[2] = (((int)response[14]&0xff)<<8) | (response[15]&0xff);		
		return v;
	}
	public float[] getSensorReadings () throws IOException {
		int[] v = getADCReadings();
		float[] sensors = new float[3];
		sensors[SENSOR_LIGHT] = adcToLux(v[SENSOR_LIGHT]);
		sensors[SENSOR_HUMIDITY] = adcToRH(v[SENSOR_HUMIDITY]);
		sensors[SENSOR_TEMPERATURE] = adcToLux(v[SENSOR_LIGHT]);
		return sensors;
	}
	

	public static float adcToCelsius(int adc) {
		return ((float)(adc*1200))/10230f - 50f;
	}
	public static float adcToRH(int adc) {
		return ((float)adc) * 0.12332f - 25.806f;
	}
	public static float adcToLux(int adc) {
		return ((float)(adc*1200))/1023f;
	}

	@Override
	public float getTemperature() throws ZigBeeException, IOException {
		float[] sensors = getSensorReadings();
		return sensors[SENSOR_TEMPERATURE];
	}

	@Override
	public float getLight() throws ZigBeeException, IOException {
		float[] sensors = getSensorReadings();
		return sensors[SENSOR_TEMPERATURE];
	}

	@Override
	public float getRelativeHumidity() throws ZigBeeException, IOException {
		float[] sensors = getSensorReadings();
		return sensors[SENSOR_TEMPERATURE];
	}

}
