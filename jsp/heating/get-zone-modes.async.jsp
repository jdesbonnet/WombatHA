<%@page
import="java.util.List"
import="java.util.Date"
import="java.text.DecimalFormat"
import="ie.wombat.ha.HANetwork"
import="ie.wombat.ha.app.AppBase"
import="ie.wombat.ha.app.heating.HeatingApp"
import="ie.wombat.ha.devices.CleodeZRC"
 import="ie.wombat.ha.server.DataLogRecord"
 import="ie.wombat.ha.server.LogRecord"
 import="ie.wombat.ha.HibernateUtil"
 import="javax.persistence.EntityManager"
%><%@page pageEncoding="UTF-8"
contentType="text/html; charset=UTF-8"%><%@include file="_header.jsp" %><%
/**
 * Service to zone state
 */
 
EntityManager em = HibernateUtil.getEntityManager();
em.getTransaction().begin();

Long appId = new Long(request.getParameter("app_id"));
HeatingApp app = (HeatingApp)HANetwork.getApplication(appId);

if (app == null) {
	throw new ServletException ("App #" + appId + " not found.");
}


DecimalFormat df = new DecimalFormat("#0.0");
int NZONES = 3;

response.setContentType("application/json");
out.write ("{\"status\":0, \"result\": [");
boolean first = true;
for (int zone = 0; zone < NZONES; zone++)  {
	if (first) {
		first = false;
	} else {
		out.write (",");
	}
	out.write ("{");
	out.write ("\"id\":" + zone );
	out.write (",\"name\":\"Zone" + (zone+1) + "\"");
	out.write (",\"mode\":\"" + app.getZoneMode(zone) + "\"");
	out.write (",\"temperature\":\"" + df.format(app.getTemperature(zone)) + "\"");

	out.write ("}");
}
	
out.write ("]}");
%>
