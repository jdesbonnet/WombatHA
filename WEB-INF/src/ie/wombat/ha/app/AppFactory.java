package ie.wombat.ha.app;


import ie.wombat.ha.HANetwork;
import ie.wombat.ha.server.Application;


import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import org.apache.log4j.Logger;

public class AppFactory {
	
	private static Logger log = Logger.getLogger(AppFactory.class);
	
	private static final AppFactory instance = new AppFactory();
	
	private AppFactory () {
		
	}
	
	public static AppFactory getInstance() {
		return instance;
	}
	
	/**
	 * Create a App object from the database app registry. 
	 * 
	 * @param network
	 * @param appRecord
	 * @return
	 */
	public AppBase createApp (HANetwork network, Application appRecord) {
		
		log.debug("Attempting to instantiate object for app " + appRecord.getClassName());
		
		String appClassName = appRecord.getClassName().trim();
		if (appClassName == null) {
			return null;
		}
		
		log.debug ("App class name " + appClassName);
		
		// Use reflection to get device constructor as object
		Class[] constructorParamTypes = {HANetwork.class, String.class};
		
		Constructor constructor;
		try {
			Class deviceDriverClass = Class.forName(appClassName);
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
		}
		
		log.debug ("Attempting to instantiate object with " + constructor);
		
		try {
			//DeviceDriver deviceDriver = (DeviceDriver)constructor.newInstance(args);
			AppBase app = (AppBase)constructor.newInstance(
					network,
					appRecord.getConfiguration()
					);
			app.setId(appRecord.getId());
			log.info("App object successfully instantiated: " + app);
			return app;
			
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
