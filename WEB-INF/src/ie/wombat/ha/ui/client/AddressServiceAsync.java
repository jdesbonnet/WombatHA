package ie.wombat.ha.ui.client;


import com.google.gwt.user.client.rpc.AsyncCallback;

/**
 * The async counterpart of {@link AddressService}
 */
public interface AddressServiceAsync {
	void getDeviceAddr16 (Long networkId, String addr64, AsyncCallback<String> callback) throws IllegalArgumentException;
	void getDeviceAddr64 (Long networkId, String addr16, AsyncCallback<String> callback) throws IllegalArgumentException;
}
