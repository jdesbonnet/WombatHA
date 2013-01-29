package ie.wombat.ha.ui.client;

import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;

/**
 * The client side stub for the RPC service.
 */
@RemoteServiceRelativePath("zb")
public interface ZigBeeService extends RemoteService {
	public String[] getAttributeValues(Long deviceId, int endPoint, int profileId, int cluster, int[] attrIds) throws IllegalArgumentException;
	public int[] getAttributeIds (Long networkId, Long deviceId, int endPoint, int clusterId) throws IllegalArgumentException;
	public String getReporting (Long deviceId, int ep, int clusterId, int attrId) throws IllegalArgumentException;
	public Integer setReporting (Long deviceId,int ep, int clusterId, int attrId, 
			int minInterval, int maxInterval, int reportableChange) throws IllegalArgumentException;
	public Integer identify (Long deviceId) throws IllegalArgumentException;

}
