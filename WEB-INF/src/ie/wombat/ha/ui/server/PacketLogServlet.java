package ie.wombat.ha.ui.server;



import java.io.IOException;

import javax.servlet.ServletException;

import org.apache.log4j.Logger;

import net.zschech.gwt.comet.server.CometServlet;
import net.zschech.gwt.comet.server.CometServletResponse;
import net.zschech.gwt.comet.server.CometSession;

@SuppressWarnings("serial")
public class PacketLogServlet extends CometServlet  {

	private static Logger log = Logger.getLogger(PacketLogServlet.class);
	
	private NICListener nicListener = NICListener.getInstance();

	public PacketLogServlet() {
		log.info ("CometServlet started");
	}

	@Override
	protected void doComet(CometServletResponse cometResponse)
			throws ServletException, IOException {
		log.info ("doComet()");
		CometSession cometSession = cometResponse.getSession(false);
		if (cometSession == null) {
			log.info("creating new comet session");
			// The comet session has not been created yet so create it.
			cometSession = cometResponse.getSession();
			nicListener.registerMessageSink(cometSession);
		}
	}
}
