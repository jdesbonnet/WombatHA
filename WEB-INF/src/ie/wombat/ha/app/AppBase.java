package ie.wombat.ha.app;

import ie.wombat.ha.HANetwork;
import ie.wombat.ha.HibernateUtil;
import ie.wombat.ha.server.LogRecord;

import java.util.HashMap;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.persistence.EntityManager;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

public abstract class AppBase {

	private static final Logger log = Logger.getLogger(AppBase.class);
	
	private Long id;
	private HANetwork network;
	private HashMap<String,String> config = new HashMap<String,String>();
	
	private ScheduledThreadPoolExecutor stpe;

	public  AppBase (HANetwork network, String configuration) {
		this.network = network;

		String[] params = configuration.trim().split(";");
		for (String param : params ) {
			String[] p = param.split("=");
			// Can have '=' in value, so concat p elements after the first with '=' sep
			if (p.length < 2) {
				continue;
			}
			String key=p[0], value=null;
			if (p.length == 2) {
				value = p[1];
			} else {
				StringBuffer buf = new StringBuffer();
				buf.append (p[1]);
				for (int i = 2; i < p.length; i++) {
					buf.append("=");
					buf.append(p[i]);
				}
				value = buf.toString();
			}
			
			config.put(key, value);
			log.debug ("config param " + key + "=" + value);
			
		}
		stpe = new ScheduledThreadPoolExecutor(1);
	}
	
	public HANetwork getNetwork() {
		return this.network;
	}
	
	public void setRepeatingTask(Runnable task, int period) {
		 stpe.scheduleAtFixedRate(task, 10, period, TimeUnit.SECONDS);
	}
	
	public String getParameter(String paramName) {
		return config.get(paramName);
	}

	public void logEvent (String eventName, String eventData) {
		
		String eventNamePrefix = this.getClass().getPackage().getName() + ".";
		
		LogRecord logRecord = new LogRecord();
		logRecord.setDevice(null);
		logRecord.setNetworkId(network.getNetworkId());
		
		logRecord.setType(LogRecord.APP_EVENT);
		logRecord.setName(eventNamePrefix + eventName);
		logRecord.setData(eventData);
		
		EntityManager em = HibernateUtil.getEntityManager();
		em.getTransaction().begin();
		em.persist(logRecord);
		em.getTransaction().commit();
		
	}
	
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}
	
	
}
