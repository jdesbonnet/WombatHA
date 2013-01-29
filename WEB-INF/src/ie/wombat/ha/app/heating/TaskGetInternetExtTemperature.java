package ie.wombat.ha.app.heating;

import java.io.IOException;
import org.apache.log4j.Logger;


/**
 * Get temperature from an external (internet) service. A scheduled task that will be run periodically.
 * @author Joe Desbonnet, jdesbonnet@gmail.com
 *
 */
public class TaskGetInternetExtTemperature implements Runnable {

	private static Logger log = Logger.getLogger(TaskGetInternetExtTemperature.class);
	private HeatingApp app;
	private String url;
	private String xpath;
	
	public TaskGetInternetExtTemperature(HeatingApp app, String url, String xpath) {
		this.app = app;
		this.url = url;
		this.xpath = xpath;
	}
	
	public void run() {
	
		
		try {
			float t = InternetWeatherUtil.getTemperature(url,xpath);
			app.logEvent("temperature_external_inet", "" + t);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
		
		
	}

}
