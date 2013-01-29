package ie.wombat.ha.ui.client;


import com.google.gwt.user.client.rpc.AsyncCallback;

/**
 * The async counterpart of {@link ZigBeeService}
 */
public interface ScriptServiceAsync {
	//void getAttributeValue(Long deviceId, int clusterId, int attrId, AsyncCallback<String> callback)
	//		throws IllegalArgumentException;
	void eval(Long networkId, String script, AsyncCallback<String> callback)
			throws IllegalArgumentException;
}
