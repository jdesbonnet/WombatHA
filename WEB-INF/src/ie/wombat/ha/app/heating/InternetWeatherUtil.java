package ie.wombat.ha.app.heating;

import java.io.IOException;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.methods.GetMethod;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
/**
 * 
 * Utilities related to retrieving weather information from the internet.
 * 
 * @author Joe Desbonnet, jdesbonnet@gmail.com
 *
 */
public class InternetWeatherUtil {

	public static float getTemperature (String url, String xpath) throws IOException {
		HttpClient client = new HttpClient();
		
		GetMethod get = new GetMethod(url);
		try {
			int status = client.executeMethod(get);
			if (status != 200) {
				throw new IOException ("Server returned unexpected response code: " + status);
			}
			
			String response = get.getResponseBodyAsString();
			
			System.err.println ("response=" + response);
			
			// Parse XML
			Document doc = DocumentHelper.parseText(response);
			
			Element el = doc.getRootElement();
			//String temperature = el.valueOf("//yweather:condition/@temp");
		
			return Float.parseFloat(el.valueOf(xpath));
		
		} catch (DocumentException e) {
			throw new IOException ("Unable to parse server response: " + e.getMessage());
		}
	}
	
	public static void main (String[] arg) throws Exception {
		
		System.err.println ("Temperature is " + getTemperature(arg[0],arg[1]) );
	}
}
