package ie.wombat.ha.api.method;

import net.sf.json.util.JSONUtils;
import ie.wombat.ha.HANetwork;
import ie.wombat.ha.ZigBeeNIC;
import ie.wombat.ha.api.Method;
import ie.wombat.ha.api.MethodResponse;

public class GetStatus implements Method {

	@Override
	public MethodResponse invokeMethod(HANetwork network, String[] params) {
		StringBuffer buf = new StringBuffer();
		ZigBeeNIC nic = network.getNIC();
		buf.append("{");
		buf.append("\"time\":" + System.currentTimeMillis());
		buf.append(",\"nicType\":" + JSONUtils.quote(nic.getClass().getName()));
		buf.append(",\"lastRx\":" + nic.getLastRxTime());
		buf.append("}");
		MethodResponse response = new MethodResponse();
		response.setStatusCode(0);
		response.setResponse(buf.toString());
		return response;
	}

}
