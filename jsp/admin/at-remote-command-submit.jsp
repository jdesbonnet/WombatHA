<%@page import="ie.wombat.ha.nic.xbee.XBeeDriver"%>
<%@include file="_header.jsp" %>
<%
	ZigBeeNIC nic = HANetwork.getInstance(4L).getNIC();

	if ( ! (nic instanceof XBeeDriver) ) {
		throw new ServletException ("AT Commands only apply to XBee NIC");
	}

	XBeeDriver xbee = (XBeeDriver)nic;
	
	// Heating controller
	Address64 addr64 = new Address64("00:13:A2:00:40:31:BD:77");
	Address16 addr16 = new Address16("FFFE");
	
	// Outdoor sensor
	//Address64 addr64 = new Address64("00:13:A2:00:40:31:BD:5E");
	//Address16 addr16 = new Address16("FFFE");
	
	//Address64 addr64 = new Address64("00:00:00:00:00:00:00:00");
	//Address16 addr16 = new Address16("0000");
	
	// This is verified to work.
	byte[] on = {0x05};
	byte[] off = {0x04};
	xbee.sendRemoteATCommand(addr64, addr16, "MY", new byte[0]);
	xbee.sendRemoteATCommand(addr64, addr16, "D1", off);
	xbee.sendRemoteATCommand(addr64, addr16, "AC", new byte[0]);
	xbee.sendRemoteATCommand(addr64, addr16, "D1", new byte[0]);

%>

DONE!