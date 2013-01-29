package ie.wombat.ha.ui.server;

import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;

import ie.wombat.ha.HANetwork;
import ie.wombat.ha.app.AppBase;
import ie.wombat.ha.app.heating.HeatingApp;
import ie.wombat.ha.ui.client.Data;
import ie.wombat.ha.ui.client.DataService;


import com.google.gwt.user.server.rpc.RemoteServiceServlet;

/**
 * The server side implementation of the Heating App RPC service.
 */
@SuppressWarnings("serial")
public class DataServiceImpl extends RemoteServiceServlet implements
		DataService {

	private static Logger log = Logger.getLogger(DataServiceImpl.class);
	
	public Data[] getTemperatureHistory(Long networkId, Date startTime, Date endTime) throws IllegalArgumentException {
		log.debug ("networkId=" + networkId + " getTemperature(" + startTime + "," + endTime + ")");
		if (networkId == null) {
			log.error("no networkId parameter");
		}
		HeatingApp app = getApp(networkId);
		log.debug ("heatingApp=" + app);
		return app.getTemperatureData(0,startTime, endTime);
	}

	public void setTargetTemperature(Long networkId, double t)
			throws IllegalArgumentException {
		getApp(networkId).setTargetTemperature(0,(float)t);
		
	}

	public Double getTargetTemperature(Long networkId)
			throws IllegalArgumentException {
		return (double)getApp(networkId).getTargetTemperature(0);
	}

	public Double getCurrentTemperature(Long networkId)
			throws IllegalArgumentException {
		return (double)getApp(networkId).getTemperature(0);

	}

	public void setHeatingState(Long networkId, boolean state)
			throws IllegalArgumentException {
		getApp(networkId).setHeatingState(0,state);
	}
	
	private HeatingApp getApp (Long networkId) {
		HANetwork network = HANetwork.getInstance(networkId);
		List<AppBase> apps = network.getApplications();
		log.debug("getApp(): found " + apps.size() + " apps in Network#" + networkId);
		for (AppBase app : apps) {
			if (app instanceof HeatingApp) {
				return (HeatingApp)app;
			}
		}
		return null;
	}
}
