package ie.wombat.ha.nic;

import ie.wombat.ha.ByteFormatUtils;
import ie.wombat.ha.HANetwork;
import ie.wombat.ha.ZigBeeNIC;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Writer;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

/**
 * This implements the HTTP gateway service. The remote gateway (Fonera) will
 * push packets transmitted byte the NIC UART to this servlet using a POST.
 * It will also periodically poll for packets queued at the server and forward
 * received packets to the NIC UART for transmission on the ZigBee network.
 * 
 * The original plan was to always return queued packets on the server in response
 * to a POST request from the Fonera. However for implementation reasons only POST
 * requests with no packets will returned queued packets on server. POST requests
 * with packets from NIC will not return packets.
 * 
 * @author joe
 *
 */
@SuppressWarnings("serial")
public class FoneraServlet extends HttpServlet {
	
	private Logger log = Logger.getLogger(FoneraServlet.class);
	
	public void doGet (HttpServletRequest request, HttpServletResponse response) throws IOException {
		response.getWriter().write("GET method not implemented.");
	}
	
	/**
	 * POST is called by the Gateway whenever a packet has arrived from the ZigBee network to be sent
	 * to the server. Or periodically by the gateway to see if there are any waiting packets to be
	 * transmitted to the ZigBee network.
	 */
	public void doPost (HttpServletRequest request, HttpServletResponse response) throws ServletException,IOException {
		
		
		String queryString = request.getQueryString();
		Long networkId;
		
		// TODO: remove default 2L network
		if (queryString == null) {
			networkId = 2L;
			throw new ServletException ("Missing network ID");
		} else {
			networkId = new Long(queryString);
		}
	
		log.info("POST to networkId=" + networkId + " from " + request.getRemoteAddr() );

		int i;
		
		InputStream in = request.getInputStream();
		BufferedReader r = new BufferedReader(new InputStreamReader(in));
		
		HANetwork network = HANetwork.getInstance(networkId);
		
		ZigBeeNIC nic = network.getNIC();
		if ( ! (nic.getUARTAdapter() instanceof  ServletAdapter)) {
			log.info("servlet adaptor not enabled, ignoring POST request");
			return;
		}
		ServletAdapter httpadapter = (ServletAdapter)nic.getUARTAdapter();
		
		log.debug("lastRxTime=" + (System.currentTimeMillis()-nic.getLastRxTime())/1000L + "s ago");
		
		int packetCount = 0;
		String line;
		while ( (line = r.readLine()) != null) {
			line = line.replaceAll(" ", "");

			if (line.length() < 2) {
				continue;
			}
			log.debug("NIC->Server: " + line);
			
			int nbytes = line.length()/2;
			byte[] bytes = new byte[nbytes];
			for (i = 0; i < nbytes; i++) {
				bytes[i] = (byte)Integer.parseInt(line.substring(i*2,i*2+2), 16);
			}
						
			// Log arrival of packet
			APIPacket apiPacket = new APIPacket ();
			apiPacket.direction = APIPacket.FROM_UART_TO_SERVER;
			apiPacket.status = APIPacket.STATUS_DELIVERED;
			apiPacket.payload = bytes;
			
			APIPacketLog.addPacket(apiPacket);
			
			packetCount++;
			
			// Send API frame to NIC
			nic.handleAPIFrame(bytes, bytes.length);
		}
		
		// Modification requested by Frank 30 June 2011: 
		// only return packets if the POST was empty.
		if (packetCount > 0) {
			response.setContentLength(0);
			return;
		}
		
		log.trace("no packets from NIC");
		
		//
		// Now send any queued packets
		//
		APIPacket packet;

		StringBuffer buf = new StringBuffer();
		
// For debugging the UBee
		if ( (System.currentTimeMillis() - nic.getLastRxTime()) > 120000L) {
			buf.append ("2101\r\n");
		}

		int packetSentCount = 0;
		while (  (packet = httpadapter.getNextAPIPacket()) != null) {			
			for (i = 0; i < packet.payload.length; i++) {
				buf.append(ByteFormatUtils.formatHexByte(packet.payload[i]));				
			}				
			buf.append ("\r\n");
			packet.status = APIPacket.STATUS_DELIVERED;
			packetSentCount++;
		}
		log.debug("Server->NIC: " + buf.toString());
		
		// TODO: if there is nothing to send, then wait for a few seconds
		if (packetSentCount == 0) {
			try {
				
				log.debug ("Long pole wait (15s)....");
				long t = -System.currentTimeMillis();
				synchronized (httpadapter) {
					httpadapter.wait(15000); 
				}
				t += System.currentTimeMillis();
				log.debug ("Long poll wait over after "+ t + "ms");
				System.err.println ("Long poll wait over after "+ t + "ms");

			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		// Write response length and response to HTTP response
		response.setContentLength(buf.length());
		Writer w = response.getWriter();
		w.write(buf.toString());
		w.flush();
		
	}
}
