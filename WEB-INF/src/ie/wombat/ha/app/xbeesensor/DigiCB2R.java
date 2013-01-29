package ie.wombat.ha.app.xbeesensor;

import java.io.IOException;

import ie.wombat.ha.devices.XBeeSeries2;

/**
 * Digi XS-Z16-CB2R 
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
 * @author joe
 *
 */
public class DigiCB2R {

	
	private XBeeSeries2 xbee;
	
	/**
	 * 
	 * @param xbee XBee proxy object
	 * @param clockPin XBee pin used to implement SCK (0 = DIO0, 1 = DIO1 etc)
	 * @param dataPin XBee pin used to implement SDA (0 = DIO0 .. etc)
	 */
	public DigiCB2R(XBeeSeries2 xbee) {
		this.xbee = xbee;
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
}
