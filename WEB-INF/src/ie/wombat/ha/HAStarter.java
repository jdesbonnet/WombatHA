package ie.wombat.ha;


import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;

import ie.wombat.ha.app.AppBase;
import ie.wombat.ha.app.AppFactory;
import ie.wombat.ha.devices.DeviceDriver;
import ie.wombat.ha.devices.DeviceFactory;
import ie.wombat.ha.nic.xbee.RepeaterServerThread;
import ie.wombat.ha.nic.xbee.TCPRelay;
import ie.wombat.ha.server.Application;
import ie.wombat.ha.server.Device;
import ie.wombat.ha.server.Network;
import ie.wombat.ha.ui.server.AddressServiceImpl;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;
import org.jfree.util.Log;

/**
 * Initialize application. Invoked by the container when the Context (app)
 * is started and stopped.
 * 
 * @author joe
 *
 */
public class HAStarter implements ServletContextListener {

	private static Logger log = Logger.getLogger(HAStarter.class);
	
	private static boolean NETWORK_ENUM_EN = true;
	
	public void contextInitialized(ServletContextEvent arg0) {
		BasicConfigurator.configure();
		
		System.err.println ("Starting HA Network");		
		
		// Load configuration file
		File configFile = new File ("/var/tmp/ha.properties");
		Properties properties = new Properties();
		try {
			properties.load(new FileInputStream(configFile));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		// TODO: simple service enable mechanism. Replace with something 
		// better.
		File repeaterEnFile = new File("/var/tmp/repeater_en");
		if (repeaterEnFile.exists()) {
			// Start Repeater 
			//System.err.println ("Starting repeater");
			//RepeaterServerThread repeater = new RepeaterServerThread();
			//repeater.start();
		}
		
		/*
		File relayEnFile = new File("/var/tmp/relay_en");
		if (relayEnFile.exists()) {
			System.err.println ("Starting TCP relay");
			TCPRelay relay = new TCPRelay("67.202.42.132",2002);
			relay.start();
		}
		*/
		

		EntityManager em = HibernateUtil.getEntityManager();
		em.getTransaction().begin();
		List<Network> networks = em.createQuery("from Network order by id").getResultList();
		for (Network network : networks) {
			log.info("Starting Network#" + network.getId());
			HANetwork hanetwork;
			try {
				hanetwork = HANetwork.createNetwork(network);
			} catch (Error e) {
				// Catch Error because RXTX lib link error can occur
				// if library is not installed.
				log.error("Error initializing Network#:" 
						+ network.getId()
						+ ": "
						+e.toString()
						);
				continue;
			}
			//HANetwork hanetwork = HANetwork.getInstance(network.getId());
			
			//
			// Initialize apps
			//
			
			// Get App configurations from database and create App objects
			// and add to HANetwork object.
			log.debug ("Creating apps for Network#" + network.getId() );
			for (Application appRecord : network.getApplications()) {
				// Create app instance
				log.debug ("Creating app " + appRecord.getClassName());
				try {
					AppBase app = AppFactory.getInstance().createApp(hanetwork, appRecord);
					hanetwork.addApplication(app);
				} catch (Error e) {
					log.error ("Error creating app " + appRecord.getClassName() + ": " + e.getMessage());
				}
			}
		}
		em.getTransaction().commit();
		em.close();
		
		
	}
	
	public void contextDestroyed(ServletContextEvent arg0) {
		System.err.println ("***BYE!***");
	}
}
