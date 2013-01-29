package ie.wombat.ha.ui.server;

public class ScriptSecurityManager extends SecurityManager {

	public void checkPackageAccess (String packageName) {
		if (Thread.currentThread().getName().equals("bsh-thread")) {
			return;
		}
		
System.err.println ("Check access for bsh-thread!!");
/*
		if (packageName.startsWith("bsh")) {
			return;
		}
		if (! packageName.startsWith("ie.wombat.ha")) {
			throw new SecurityException ("Attempting to access forbidden package " + packageName); 
		}
		*/

		return;
	}
	
}
