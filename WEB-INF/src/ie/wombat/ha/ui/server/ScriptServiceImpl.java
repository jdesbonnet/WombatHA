package ie.wombat.ha.ui.server;


import org.apache.log4j.Logger;

import ie.wombat.ha.HANetwork;
import ie.wombat.ha.ui.client.ScriptService;

import bsh.EvalError;
import bsh.Interpreter;

import com.google.gwt.user.server.rpc.RemoteServiceServlet;

/**
 * The server side implementation of {@link ScriptService}.
 * 
 * 
 * @author Joe Desbonnet, jdesbonnet@gmail.com
 */
@SuppressWarnings("serial")
public class ScriptServiceImpl extends RemoteServiceServlet implements ScriptService{

	private static final Logger log = Logger.getLogger(ScriptServiceImpl.class);
	
	public String eval (Long networkId, String script) throws IllegalArgumentException {
	
		log.info("networkId=" + networkId + " script=" + script);
		
		SecurityManager defaultSecurityManager = System.getSecurityManager();
		
		ScriptSecurityManager ssm = new ScriptSecurityManager();
		
		Interpreter interpreter = new Interpreter();
		String response;
		try {
			interpreter.set("network", HANetwork.getInstance(networkId));
			
			// TODO: need to restrict what can be done during eval() method. Only want to
			// give access to classes in the ie.wombat.ha.* hierarchy. 
			
			//String threadName = Thread.currentThread().getName();
			//Thread.currentThread().setName("bsh-thread");
			//System.setSecurityManager(ssm);
			response = interpreter.eval(script).toString();
			//System.setSecurityManager(defaultSecurityManager);
			//Thread.currentThread().setName(threadName);
			
		} catch (EvalError e) {
			e.printStackTrace();
			response = e.getMessage();
		}
		log.info("response=" + response);
		
		
		return response;
		
	}
}
