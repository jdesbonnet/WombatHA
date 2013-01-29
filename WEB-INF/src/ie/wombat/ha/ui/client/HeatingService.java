package ie.wombat.ha.ui.client;

import java.util.Date;

import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;

/**
 * The client side stub for the improvised heating RPC service.
 */
@RemoteServiceRelativePath("heating")
public interface HeatingService extends RemoteService {
	public void setState(Long networkId, boolean state) throws IllegalArgumentException;
	public void setState(Long networkId, int zone, boolean state) throws IllegalArgumentException;

	public boolean getState(Long networkId) throws IllegalArgumentException;
	public void setTargetTemperature(Long networkId, double t) throws IllegalArgumentException;
	public double getTargetTemperature(Long networkId) throws IllegalArgumentException;
	public int getSystemStatus(Long networkId) throws IllegalArgumentException;
}
