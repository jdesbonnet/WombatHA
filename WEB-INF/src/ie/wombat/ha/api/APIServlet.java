package ie.wombat.ha.api;

import java.io.IOException;

import ie.wombat.ha.HANetwork;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

import net.sf.json.util.JSONUtils;

/**
 * ${CONTEXT_PATH}/api/{networkId}/{method}/{param0}/{param1}...
 * 
 * @author joe
 *
 */
public class APIServlet extends HttpServlet {

	private static Logger log = Logger.getLogger(APIServlet.class);
	
	public static final int METHOD_NOT_FOUND = 1025;
	public static final int METHOD_ERROR = 1026;

	public void doGet (HttpServletRequest request, HttpServletResponse response) throws IOException {
		
		log.info("API request received path=" + request.getPathInfo());
		
		String[] params = request.getPathInfo().split("/");
		
		Long networkId = new Long(params[1]);
		log.info("networkId=" + networkId);
		
		//EntityManager em = HibernateUtil.getEntityManager();
		//HANetwork network = em.find(HANetwork.class, networkId);
		HANetwork network = HANetwork.getInstance(networkId);
		
		String methodClass = "ie.wombat.ha.api.method." + params[2];
		
		Method method;
		try {
			method = (Method)Class.forName(methodClass).newInstance();
		} catch (InstantiationException e) {
			returnError(response,METHOD_ERROR);
			return;
		} catch (IllegalAccessException e) {
			returnError(response,METHOD_ERROR);
			return;
		} catch (ClassNotFoundException e) {
			returnError(response,METHOD_NOT_FOUND);
			return;
		}
		
		log.info("method=" + method);
		
		response.setContentType("application/json");
		
		MethodResponse mresp = method.invokeMethod(network, params);
		response.getOutputStream().print("{\"status\":0,\"result\":" + 
				JSONUtils.quote(mresp.getResponse())
				+ "}");
		
	}
		
	private void returnError (HttpServletResponse response, int statusCode) throws IOException {
			response.getOutputStream().print("{\"status\":" + statusCode + "}");
	}
}
