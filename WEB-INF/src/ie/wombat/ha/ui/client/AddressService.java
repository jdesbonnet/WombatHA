package ie.wombat.ha.ui.client;

import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;

/**
 * The client side stub for the RPC service.
 */
@RemoteServiceRelativePath("addr")
public interface AddressService extends RemoteService {
	public String getDeviceAddr16(Long networkId, String addr64) throws IllegalArgumentException;
	public String getDeviceAddr64(Long networkId, String addr16) throws IllegalArgumentException;
}
