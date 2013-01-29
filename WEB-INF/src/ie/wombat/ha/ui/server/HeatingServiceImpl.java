package ie.wombat.ha.ui.server;


import ie.wombat.ha.HANetwork;
import ie.wombat.ha.app.heating.HeatingApp;
import ie.wombat.ha.ui.client.HeatingService;


import com.google.gwt.user.server.rpc.RemoteServiceServlet;

/**
 * The server side implementation of the improvised heating app RPC service.
 */
@SuppressWarnings("serial")
public class HeatingServiceImpl extends RemoteServiceServlet implements HeatingService {

	public void setState(Long networkId, boolean state) throws IllegalArgumentException {
System.err.println ("setState(" +networkId + "," + state + ")");
		HANetwork hanetwork = HANetwork.getInstance(networkId);
		HeatingApp heatingapp = (HeatingApp)(hanetwork.getApplications().get(0));
		heatingapp.setHeatingState(0,state);
	}
	public void setState(Long networkId, int zone, boolean state) throws IllegalArgumentException {
System.err.println ("setState(networkId=" +networkId + ", zone=" + zone + ", state=" + state + ")");
		HANetwork hanetwork = HANetwork.getInstance(networkId);
		HeatingApp heatingapp = (HeatingApp)(hanetwork.getApplications().get(0));
		heatingapp.setHeatingState(zone,state);
	}
	
	public boolean getState (Long networkId) throws IllegalArgumentException {
		return false;
	}
	
	public void setTargetTemperature (Long networkId, double t) throws IllegalArgumentException {		
		HANetwork hanetwork = HANetwork.getInstance(networkId);
		HeatingApp heatingapp = (HeatingApp)(hanetwork.getApplications().get(0));
		heatingapp.setTargetTemperature(0,(float)t);
	}
	
	public double getTargetTemperature (Long networkId) throws IllegalArgumentException {
		HANetwork hanetwork = HANetwork.getInstance(networkId);
		HeatingApp heatingapp = (HeatingApp)(hanetwork.getApplications().get(0));
		return (double)heatingapp.getTargetTemperature(0);
	}
	
	public int getSystemStatus (Long networkId) throws IllegalArgumentException {
		return 0;
	}
}
