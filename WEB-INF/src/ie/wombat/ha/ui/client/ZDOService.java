package ie.wombat.ha.ui.client;

import ie.wombat.zigbee.zdo.BindTableResponse;
import ie.wombat.zigbee.zdo.SimpleDescriptorResponse;

import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;

/**
 * The client side stub for the RPC service.
 */
@RemoteServiceRelativePath("zdo")
public interface ZDOService extends RemoteService {
	
	public int[] getActiveEndpoints(Long networkId,Long deviceId) throws IllegalArgumentException;
	public String getSimpleDescriptor(Long networkId,Long deviceId, int endPoint) throws IllegalArgumentException;
	public SimpleDescriptorResponse getSimpleDescriptor2(Long networkId,Long deviceId, int endPoint) throws IllegalArgumentException;

	public String getNodeDescriptor(Long networkId,Long deviceId) throws IllegalArgumentException;
	public String getNetworkDiscovery(Long networkId,Long deviceId) throws IllegalArgumentException;
	public String getLQI(Long networkId,Long deviceId) throws IllegalArgumentException;
	
	public String getBindTable (Long networkId, Long deviceId) throws IllegalArgumentException;
	public BindTableResponse getBindTable2 (Long networkId, Long deviceId) throws IllegalArgumentException;

	public Integer bindRequest(Long networkId, Long srcDeviceId, Long dstDeviceId,int clusterId, int srcEp, int dstEp) throws IllegalArgumentException;
	public Integer unbindRequest(Long networkId, String srcAddr64, String dstAddr64,int clusterId, int srcEp, int dstEp) throws IllegalArgumentException;

	public String getDeviceAddr16(Long networkId,Long deviceId) throws IllegalArgumentException;

}
