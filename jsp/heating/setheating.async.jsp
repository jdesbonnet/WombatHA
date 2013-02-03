<%@page import="ie.wombat.ha.app.heating.ZoneMode"%>
<%@page import="ie.wombat.ha.app.heating.HeatingApp"%>
<%@page import="ie.wombat.ha.nic.xbee.XBeeUtil"%>
<%@page import="ie.wombat.ha.HANetwork"%>
<%@page import="java.net.NoRouteToHostException"%>
<%@page pageEncoding="UTF-8"
contentType="text/html; charset=UTF-8"%><%@include file="_header.jsp" %><%
	Long appId = new Long(request.getParameter("app_id"));
	HeatingApp app = (HeatingApp)HANetwork.getApplication(appId);

	// zone is mandatory
	int zone = Integer.parseInt(request.getParameter("zone"));
	
	try {
		float t = Float.parseFloat(request.getParameter("t"));
		app.setTargetTemperature(zone, t);
	} catch (Exception e) {
		// 
	}
	
	String mode = request.getParameter("mode");
	if (mode != null) {
		app.setZoneMode(zone, ZoneMode.valueOf(mode));
	}
%>