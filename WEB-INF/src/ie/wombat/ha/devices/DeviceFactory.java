package ie.wombat.ha.devices;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import org.apache.log4j.Logger;

import ie.wombat.ha.HANetwork;
import ie.wombat.ha.ZigBeeNIC;
import ie.wombat.ha.server.Device;
import ie.wombat.zigbee.address.Address16;
import ie.wombat.zigbee.address.Address64;

public class DeviceFactory {

	private static Logger log = Logger.getLogger(DeviceFactory.class);
	
	private static final DeviceFactory instance = new DeviceFactory();
	
	private DeviceFactory () {
		
	}
	
	public static DeviceFactory getInstance() {
		return instance;
	}
	
	public DeviceDriver getDeviceDriver (HANetwork network, Device deviceRecord) {
		
		log.debug("Attempting to instantiate object for device record " + deviceRecord);
		
		String deviceDriverClassName = deviceRecord.getDriverClassName().trim();
		if (deviceDriverClassName == null) {
			return null;
		}
		
		log.debug ("Driver class name " + deviceDriverClassName);
		
		// Use reflection to get device constructor as object
		Class[] constructorParamTypes = {Address64.class, Address16.class,ZigBeeNIC.class};
		
		Constructor constructor;
		try {
			Class deviceDriverClass = Class.forName(deviceDriverClassName);
			constructor = deviceDriverClass
				.getConstructor(constructorParamTypes);
		} catch (SecurityException e1) {
			e1.printStackTrace();
			return null;
		} catch (NoSuchMethodException e1) {
			e1.printStackTrace();
			return null;
		} catch (ClassNotFoundException e1) {
			e1.printStackTrace();
			return null;
		} catch (Exception e1) {
			e1.printStackTrace();
			return null;
		} catch (Throwable e1) {
			e1.printStackTrace();
			return null;
		}
		
		log.debug ("Attempting to instantiate object with " + constructor);
		
		try {
			//DeviceDriver deviceDriver = (DeviceDriver)constructor.newInstance(args);
			DeviceDriver deviceDriver = (DeviceDriver)constructor.newInstance(
					new Address64(deviceRecord.getAddress64()),
					new Address16(deviceRecord.getAddress16()),
					network.getNIC()
					);
			log.info("Device driver object successfully instantiated: " + deviceDriver);
			deviceDriver.setId(deviceRecord.getId());
			return deviceDriver;
			
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
			return null;
		} catch (InstantiationException e) {
			e.printStackTrace();
			return null;
		} catch (IllegalAccessException e) {
			e.printStackTrace();
			return null;
		} catch (InvocationTargetException e) {
			e.printStackTrace();
			return null;
		}
		
		 
	}
}
