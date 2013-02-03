package ie.wombat.ha.devices;

import ie.wombat.ha.ZigBeeNIC;
import ie.wombat.zigbee.address.Address16;
import ie.wombat.zigbee.address.Address64;

import java.io.IOException;

import org.apache.log4j.Logger;

/**
 * A device driver for a 4 zone heating system driving by a XBee Series 2 where D0 .. D3 control
 * the valves to heating zones 0 .. 3.
 * 
 * @author joe
 *
 */
public class XBeeHeater extends XBeeSeries2 implements HeatingDevice {

	private static Logger log = Logger.getLogger(XBeeHeater.class);
	
	private boolean[] state = new boolean[4];
	
	public XBeeHeater(Address64 address64, Address16 address16, ZigBeeNIC nic) {
		super(address64, address16, nic);
	}
	
	public void setState(int zone, boolean b) throws IOException {
		log.info ("setState(zone=" + zone + ", state=" + b + ")");
		setDigitalOutput(zone, b);
		this.state[zone] = b;
	}
	
	public boolean getState (int zone) throws IOException {
		int state;
		state = getDigitalOutput(zone);
		log.info("zone state read as " + state);
		return false;
	}
	
}
