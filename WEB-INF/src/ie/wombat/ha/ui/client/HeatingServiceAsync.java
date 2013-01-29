package ie.wombat.ha.ui.client;

import java.util.Date;

import com.google.gwt.user.client.rpc.AsyncCallback;

/**
 * The async counterpart {@link DataService}
 */
public interface HeatingServiceAsync {
	void setState(Long networkId, boolean b, AsyncCallback<Void> callback)
			throws IllegalArgumentException;
	void setState(Long networkId, int zone, boolean b, AsyncCallback<Void> callback)
			throws IllegalArgumentException;
	void getState(Long networkId, AsyncCallback<Boolean> callback)
			throws IllegalArgumentException;
	void setTargetTemperature(Long networkId, double temperature, AsyncCallback<Void> callback)
			throws IllegalArgumentException;
	void getTargetTemperature(Long networkId, AsyncCallback<Double> callback)
			throws IllegalArgumentException;
	void getSystemStatus(Long networkId, AsyncCallback<Integer> callback)
			throws IllegalArgumentException;
	
}
