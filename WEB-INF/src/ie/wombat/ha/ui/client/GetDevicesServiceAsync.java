package ie.wombat.ha.ui.client;

import com.google.gwt.user.client.rpc.AsyncCallback;

/**
 * The async counterpart of {@link GetDevicesService}
 */
public interface GetDevicesServiceAsync {
	void getDevices(Long networkId, AsyncCallback<DeviceInfo[]> callback)
			throws IllegalArgumentException;
	void getDeviceInfo(Long deviceId, AsyncCallback<DeviceInfo> callback)
			throws IllegalArgumentException;
	
}
