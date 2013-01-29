package ie.wombat.ha.restapi.resources;

import ie.wombat.ha.HANetwork;
import ie.wombat.ha.devices.DeviceDriver;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;



@Path("/api/{network}")
public class NetworkResource {

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public String getVersion (@PathParam("network") Long networkId) {
		HANetwork network = HANetwork.getInstance(networkId);
		
		if (network == null) {
			return "{\"response\":-1}";
		}
		
		StringBuffer ret = new StringBuffer();
		ret.append ("{\"response\": 0");
		ret.append (", \"devices\":[");
		boolean first = true;
		for (DeviceDriver device : network.getDevices()) {
			if ( ! first ) {
				ret.append (",");
			} else {
				first = false;
			}
			ret.append ("{\"id\":" + device.getId());
			ret.append (",\"addr64\":" + device.getAddress64().toString());
			// TODO: use JSON Utils to quote
			ret.append (",\"name\": \"" + device.getName() + "\"");
			ret.append (",\"href\": \"/WombatHA/rest/api/" + networkId + "/" + device.getAddress64());
			ret.append ("}");
		}
		ret.append ("]}");
		return ret.toString();
	}
}
