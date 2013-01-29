package ie.wombat.ha.ui.client;

import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;

/**
 * The client side stub for the RPC service.
 */
@RemoteServiceRelativePath("getdev")
public interface GetDevicesService extends RemoteService {
	public DeviceInfo[] getDevices(Long networkId) throws IllegalArgumentException;
	public DeviceInfo getDeviceInfo(Long deviceId) throws IllegalArgumentException;
}
