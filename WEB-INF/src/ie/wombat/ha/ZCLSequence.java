package ie.wombat.ha;

/**
 * Provides a sequence for use in ZigBee Cluster Library commands. Runs
 * from 1 to 249 and wraps back to 1 again.
 * 
 * TODO: sequence doesn't have to be global. Only when sending multiple
 * commands to the same device. Or does it?
 * 
 * @author joe
 *
 */
public class ZCLSequence {

	private static int next = 22;
	
	public synchronized static int getNext() {
		return ((next++) % 250) + 1;
	}
}
