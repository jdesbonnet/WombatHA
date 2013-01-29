package ie.wombat.ha.ui.client;

import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;

/**
 * The client side stub for the RPC service.
 */
@RemoteServiceRelativePath("script")
public interface ScriptService extends RemoteService {
	public String eval(Long networkId, String script) throws IllegalArgumentException;
}
