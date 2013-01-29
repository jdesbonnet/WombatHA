package ie.wombat.ha.ui.client;


import ie.wombat.zigbee.zdo.BindTableResponse;
import ie.wombat.zigbee.zdo.SimpleDescriptorResponse;

import com.google.gwt.user.client.rpc.AsyncCallback;

/**
 * The async counterpart of {@link ZDOService}
 */
public interface ZDOServiceAsync {
	
	void getActiveEndpoints(Long networkId,Long deviceId, AsyncCallback<int[]> callback)
			throws IllegalArgumentException;
	void getSimpleDescriptor(Long networkId,Long deviceId, int endPoint, AsyncCallback<String> callback)
			throws IllegalArgumentException;
	
	void getSimpleDescriptor2(Long networkId,Long deviceId, int endPoint, AsyncCallback<SimpleDescriptorResponse> callback)
			throws IllegalArgumentException;
	
	
	
	void getNodeDescriptor(Long networkId,Long deviceId, AsyncCallback<String> callback)
			throws IllegalArgumentException;
	void getNetworkDiscovery(Long networkId,Long deviceId, AsyncCallback<String> callback)
			throws IllegalArgumentException;
	void getLQI(Long networkId,Long deviceId, AsyncCallback<String> callback)
			throws IllegalArgumentException;
	void getBindTable(Long networkId, Long deviceId, AsyncCallback<String> callback)
			throws IllegalArgumentException;
	
	void getBindTable2(Long networkId, Long deviceId, AsyncCallback<BindTableResponse> callback)
			throws IllegalArgumentException;
	
	void bindRequest(Long networkId, Long srcDeviceId, Long dstDeviceId, int clusterId, int srcEp, int dstEp, AsyncCallback<Integer> callback)
			throws IllegalArgumentException;
	void unbindRequest(Long networkId, String srcAddr64, String dstAddr64, int clusterId, int srcEp, int dstEp, AsyncCallback<Integer> callback)
			throws IllegalArgumentException;

	
	void getDeviceAddr16 (Long networkId, Long deviceId, AsyncCallback<String> callback)
			throws IllegalArgumentException;
	
}
