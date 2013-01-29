package ie.wombat.ha.ui.client;

import com.google.gwt.user.client.rpc.IsSerializable;

/**
 * ID + Name is a common light weight record that can be reused.
 * @author joe
 *
 */
public class IdName implements IsSerializable {
	
	public String id;
	public String name;
	public String param;
	
	public static IdName make (String id, String name) {
		IdName in = new IdName();
		in.id = id;
		in.name = name;
		return in;
	}
}
