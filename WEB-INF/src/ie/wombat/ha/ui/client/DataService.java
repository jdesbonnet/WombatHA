package ie.wombat.ha.ui.client;

import java.util.Date;

import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;

/**
 * The client side stub for the Heating App RPC service.
 * 
 * TODO: assuming one app per network
 * 
 */
@RemoteServiceRelativePath("getdata")
public interface DataService extends RemoteService {
	public void setTargetTemperature (Long networkId, double t) throws IllegalArgumentException;
	public Double getTargetTemperature (Long networkId) throws IllegalArgumentException;
	public Double getCurrentTemperature (Long networkId) throws IllegalArgumentException;
	public void setHeatingState (Long networkId, boolean state) throws IllegalArgumentException;
	public Data[] getTemperatureHistory(Long networkId, Date startTime, Date endTime) throws IllegalArgumentException;
}
