package ie.wombat.ha.api;

import ie.wombat.ha.HANetwork;

public interface Method {

	public MethodResponse invokeMethod (HANetwork network, String[] params);
}
