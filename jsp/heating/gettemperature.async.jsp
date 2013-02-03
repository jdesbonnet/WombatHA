<%@page import="java.net.NoRouteToHostException"
import="ie.wombat.ha.HeatingControl"
import="ie.wombat.ha.NetvoxUtil"%><%@page pageEncoding="UTF-8"
contentType="text/html; charset=UTF-8"%><%@include file="_header.jsp" %><%
String HOST = "localhost";
int PORT = 4445;
java.net.Socket sock = new java.net.Socket(HOST, PORT);
sock.setSoTimeout(1000);
java.io.Reader r = new java.io.InputStreamReader(sock.getInputStream());
java.io.BufferedReader lnr = new java.io.BufferedReader(r);


java.text.DecimalFormat df = new java.text.DecimalFormat(".0");


String line;

String kitchen = null;
Float upstairs = null;
long t;
long now = System.currentTimeMillis()/1000L;
while ( ( line = lnr.readLine()) != null) {
	String p[] = line.split("\\s+");
	if (!"T".equals(p[1])) {
		continue;
	}
	t = Long.parseLong(p[0]);
	if (now - t > 300) {
		continue;
	}
	if ("kitchen".equals(p[2])) {
		kitchen = df.format(Double.parseDouble(p[3]));
	}
}
%>
[<%= kitchen %>, null ]

