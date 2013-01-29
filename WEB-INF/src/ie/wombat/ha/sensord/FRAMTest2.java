/*
 *
 */
package ie.wombat.ha.sensord;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;



public class FRAMTest2   {
	
	public static void main (String[] arg) throws IOException {

		InputStream in = new FileInputStream("/dev/ttyACM0");
		
		int c,i=0;
		while ( (c = in.read()) != -1) {
			//System.out.print(" " + c);
			if (++i % 100  == 0) {
				System.out.print(".");
				System.out.flush();
			}
		}

	}
	
}
