package ie.wombat.ha.ui.client;


import com.google.gwt.user.client.rpc.AsyncCallback;

/**
 * The async counterpart of {@link ZigBeeService}
 */
public interface ZigBeeServiceAsync {
	//void getAttributeValue(Long deviceId, int clusterId, int attrId, AsyncCallback<String> callback)
	//		throws IllegalArgumentException;
	void getAttributeValues(Long deviceId, int endPoint, int profileId, int clusterId, int[] attrIds, AsyncCallback<String[]> callback)
			throws IllegalArgumentException;
	void getAttributeIds(Long networkId, Long deviceId, int endPoint, int clusterId, AsyncCallback<int[]> callback)
			throws IllegalArgumentException;
	void getReporting(Long deviceId, int ep, int clusterId, int attrId, AsyncCallback<String> callback)
			throws IllegalArgumentException;
	void setReporting(Long deviceId, int ep, int clusterId, int attrId, 
			int minInterval, int maxInterval, int reportableChange, AsyncCallback<Integer> callback)
			throws IllegalArgumentException;
	
	void identify(Long deviceId, AsyncCallback<Integer> callback)
			throws IllegalArgumentException;
	
}
