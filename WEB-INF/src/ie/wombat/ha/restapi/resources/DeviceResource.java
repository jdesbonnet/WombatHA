package ie.wombat.ha.restapi.resources;

import ie.wombat.ha.HANetwork;
import ie.wombat.ha.devices.DeviceDriver;
import ie.wombat.zigbee.address.Address64;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Path("/api/{networkId}/{deviceId}")
public class DeviceResource {

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public String getDevice (@PathParam("networkId") Long networkId, @PathParam("deviceId") String deviceIdStr) {
		
		HANetwork network = HANetwork.getInstance(networkId);
		
		if (network == null) {
			return "{\"response\":-1}";
		}
	
		DeviceDriver device=null;
		try {
			Long deviceId = new Long(deviceIdStr);
			for (DeviceDriver d : network.getDevices()) {
				if (d.getId().equals(deviceId)) {
					device = d;
					break;
				}
			}
		} catch (Exception e) {
			// ignore
		}
		if (device == null) {
			try {
				Address64 addr64 = new Address64(deviceIdStr);
				for (DeviceDriver d : network.getDevices()) {
					if (d.getAddress64().equals(addr64)) {
						device = d;
						break;
					}
				}	
			} catch (Exception e) {
				// ignore
			}
		}
		
		if (device == null) {
			return "{\"response\":-1}";
		}
		
		
		StringBuffer ret = new StringBuffer();
		ret.append ("{\"response\": 0");
		ret.append (",\"id\":" + device.getId());
		ret.append (",\"addr64\":" + device.getAddress64().toString());
		ret.append (",\"addr16\":" + device.getAddress16().toString());
		ret.append (",\"name\": \"" + device.getName() + "\"");
		ret.append (",\"lastRx\": " + device.getLastRxTime());
		ret.append ("}");
		return ret.toString();
	}
}
