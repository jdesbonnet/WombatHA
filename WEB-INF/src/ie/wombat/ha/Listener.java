package ie.wombat.ha;

import java.util.Date;

public interface Listener {

	/** 
	 * A time after which the listening object should be unregistered from the
	 * listening object list. Prevents accumulation of listeners due to 
	 * clients that fail to unregister themselves after use. If null then
	 * there is no expiry time.
	 * @return
	 */
	public Date getExpiryTime();
	public void setExpiryTime(Date expire);
	
}
