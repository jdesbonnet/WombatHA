package ie.wombat.zigbee.zdo;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.google.gwt.user.client.rpc.core.java.util.Collections;


/**
 * TODO: this does not belong in the ZDO package.
 * 
 * See section ZCL specification 2.4.14, page 41, "Discover Attributes Response Command"
 *  
 * @author Joe Desbonnet, jdesbonnet@gmail.com
 *
 */
public class AttributesDiscovery  {

	private List<Integer> attributeIds = new ArrayList<Integer>(8);
	private List<Integer> attributeType = new ArrayList<Integer>(8);
	
	private boolean discoveryComplete;
	
	public AttributesDiscovery () {
		
	}
	
	public void addPacket (byte[] packet, int offset) {
	
		discoveryComplete = (packet[offset+0]&0xff) != 0;
		
		int i = offset+1;
		int attributeId;
		int dataType;
				
		while ( i < packet.length) {
			attributeId = packet[i] &0xff;
			attributeId |= (packet[i+1]&0xff)<<8;
			dataType = packet[i+2] & 0xff;
			attributeIds.add(new Integer(attributeId));
			attributeType.add(new Integer(dataType));
			i+=3;
		}
	}

	public int[] getAttributeIds () {
		int[] ret = new int[attributeIds.size()];
		int i = 0;
		for (Integer attrId : attributeIds) {
			ret[i++] = attrId.intValue();
		}
		return ret;		
	}
	
	public boolean isComplete() {
		return discoveryComplete;
	}
	
	
	
}
