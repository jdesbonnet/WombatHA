package ie.wombat.zigbee.zdo;

import java.io.Serializable;

public interface ZDOResponse extends Serializable {

	public int getStatus ();
	public void setStatus (int status);
	public void addPacket (byte[] bytes, int offset);
	public boolean isComplete();
	
}
