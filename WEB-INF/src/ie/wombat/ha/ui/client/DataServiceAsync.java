package ie.wombat.ha.ui.client;

import java.util.Date;

import com.google.gwt.user.client.rpc.AsyncCallback;

/**
 * The async counterpart {@link DataService}
 */
public interface DataServiceAsync {
	void setTargetTemperature (Long networkId, double t, AsyncCallback<Void> callback) throws IllegalArgumentException;
	void getTargetTemperature (Long networkId, AsyncCallback<Double> callback) throws IllegalArgumentException;
	void getCurrentTemperature (Long networkId, AsyncCallback<Double> callback) throws IllegalArgumentException;
	void setHeatingState (Long networkId, boolean state, AsyncCallback<Void> callback) throws IllegalArgumentException;
	void getTemperatureHistory(Long networkId, Date startTime, Date endTiem, AsyncCallback<Data[]> callback)
			throws IllegalArgumentException;
}
