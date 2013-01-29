<%@page 
import="java.util.List"
import="java.util.Date"
import="ie.wombat.ha.HANetwork"
import="ie.wombat.ha.app.AppBase"
import="ie.wombat.ha.app.heating.HeatingApp"
import="ie.wombat.ha.devices.CleodeZRC"
 import="ie.wombat.ha.server.DataLogRecord"
 import="ie.wombat.ha.server.LogRecord"
 import="ie.wombat.ha.HibernateUtil"
 import="javax.persistence.EntityManager"
%><%@page pageEncoding="UTF-8"
contentType="text/html; charset=UTF-8"%><%@include file="_header.jsp" %><%/**
 * Service to return heating data (temperature, targets, state)
 */
 
EntityManager em = HibernateUtil.getEntityManager();

long now = System.currentTimeMillis();
long startTime = now - 4L*24L*3600000L;
//long startTime = now - 3600L;

long endTime = now;

Long appId = new Long(request.getParameter("app_id"));
HeatingApp app = (HeatingApp)HANetwork.getApplication(appId);

if (app == null) {
	throw new ServletException ("App #" + appId + " not found.");
}

try {
	startTime = Long.parseLong(request.getParameter("start_time"))*1000;
	endTime = Long.parseLong(request.getParameter("end_time"))*1000;
} catch (Exception e) {
	e.printStackTrace();
	// ignore
}
Date start = new Date(startTime);
Date end = new Date(endTime);

// Time resolution 
long resolution = 900000L;
try {
	resolution = Long.parseLong(request.getParameter("res"));
} catch (Exception e) {
	// ignore
}

System.err.println ("heating.async.jsp: startTime=" + startTime + " endTime=" + endTime);
em.getTransaction().begin();

response.setContentType("application/json");

int NZONES = 3;

//Start of zones array
out.write ("[");

for (int zone = 0; zone < NZONES; zone++)  {

	CleodeZRC sensor = app.getSensorDevice(zone);
	
	
	out.write ("{");
	out.write ("\"id\":" + zone );
	out.write (",\"name\":\"Zone" + (zone+1) + "\"");
	out.write (",\"mode\":\"" + app.getZoneMode(zone) + "\"");
	

	if (sensor != null) {	
		long temperatureAge = System.currentTimeMillis() - app.getTemperatureTime(zone);
		if (temperatureAge < 600000) {
			out.write (",\"t\":" + app.getTemperature(zone));
		}
		out.write (",\"target_t\":" + app.getTargetTemperature(zone));
	}
	
	
	// Sensor data
	out.write (",\"data\":[");
	if (sensor != null) {
		
		List<DataLogRecord> list1 = em.createQuery("from DataLogRecord "
			+ " where address64=:addr64 "
			+ " and key='temperature' "
			+ " and timestamp >= :start and timestamp < :end ")
			.setParameter("addr64", sensor.getAddress64().toString())
			.setParameter("start",start)
			.setParameter("end",end)
			.getResultList();
		int i = 0;
		long prevt = 0;
		for (DataLogRecord r : list1) {
			if (r.getTimestamp().getTime()-prevt < resolution) {
				continue;
			}
			out.write ("[");
			out.write (""+(r.getTimestamp().getTime()/1000));
			out.write (",");
			out.write (r.getValue());
			out.write ("], ");
			prevt = r.getTimestamp().getTime();
		}
		out.write (" null ] \n ");
	} else {
		out.write("] \n");
	}
	

	out.write ("\n , \"state\":[ ");
	List<LogRecord> list2 = em.createQuery("from LogRecord where networkId=1 "
		+ " and name='ie.wombat.ha.app.heating.heating_state_zone_" + zone + "'"
		+ " and timestamp >= :start and timestamp < :end ")
		.setParameter("start",start)
		.setParameter("end",end)
		.getResultList();
	int state = 0;
	for (LogRecord r : list2) {
		if (state == 0 && "1".equals(r.getData()) ) {
			out.write ("["+r.getTimestamp().getTime()/1000L);
			out.write (",1], ");
			state = 1;
			continue;
		}
		if (state == 1 && "0".equals(r.getData())) {
			out.write ("["+r.getTimestamp().getTime()/1000L);
			out.write (",0], ");
			state = 0;
			continue;
		}
	}
	if (state == 1) {
		out.write ("[" + end.getTime()/1000L + ",0],");
	}
	out.write (" null ]\n"); // end of state record list

	// Target temperature
	out.write ("\n , \"target\":[ ");
	if (sensor != null) {
		List<LogRecord> list3 = em.createQuery("from LogRecord where networkId=1 "
			+ " and name='ie.wombat.ha.app.heating.set_target_temperature' "
			+ " and timestamp >= :start and timestamp < :end order by id")
			.setParameter("start",start)
			.setParameter("end",end)
			.getResultList();
		if (list3.size() > 3) {
			String lastValue="";
			for (LogRecord r : list3) {
				out.write ("["+r.getTimestamp().getTime()/1000L);
				out.write (",");
				out.write (r.getData());
				out.write ("],");
				lastValue = r.getData();
			}
			out.write ("[" + System.currentTimeMillis()/1000L + "," + lastValue +"]");
		}
	}
	out.write ("]\n");
	
	
	out.write ("} , \n"); // end of zone

}

	CleodeZRC externalSensor = app.getExternalSensorDevice();
	if (externalSensor == null) {
		// If there is no external temperature sensor, use internet source
		out.write ("{\"name\":\"Outside\"");
		out.write (",\"id\":-1"); 
		
		long temperatureAge = System.currentTimeMillis() - app.getExternalTemperatureTime();
		if (temperatureAge < 600000) {
			out.write (",\"t\":" + app.getExternalTemperature() );
		}
		 
		List<LogRecord> list4 = em.createQuery("from LogRecord "
			+ " where name='ie.wombat.ha.app.heating.temperature_external' "
			+ " and timestamp >= :start and timestamp < :end ")
			.setParameter("start",start)
			.setParameter("end",end)
			.getResultList();

		out.write (", \"data\":[");
		long prevt = 0;
		for (LogRecord r : list4) {
			if (r.getTimestamp().getTime()-prevt < resolution) {
				continue;
			}
			out.write ("[");
			out.write (""+(r.getTimestamp().getTime()/1000L));
			out.write (",");
			out.write (r.getData());
			out.write ("], ");
	
			prevt = r.getTimestamp().getTime();
		}
		out.write (" null]}  \n");
	} else {
		out.write ("{\"name\":\"Outside\"");
		out.write (",\"id\":-1"); 
		
		long temperatureAge = System.currentTimeMillis() - app.getExternalTemperatureTime();
		if (temperatureAge < 600000) {
			out.write (",\"t\":" + app.getExternalTemperature() );
		}
		 
		List<DataLogRecord> list4 = em.createQuery("from DataLogRecord "
			//+ " where address64='10:00:00:50:C2:10:00:63' "
			+ " where address64=:addr64 "
			+ " and key='temperature' "
			+ " and timestamp >= :start and timestamp < :end ")
			.setParameter("addr64", externalSensor.getAddress64().toString())
			.setParameter("start",start)
			.setParameter("end",end)
			.getResultList();

		out.write (", \"data\":[");
		long prevt = 0;
		for (DataLogRecord r : list4) {
			if (r.getTimestamp().getTime()-prevt < resolution) {
				continue;
			}
			out.write ("[");
			out.write (""+(r.getTimestamp().getTime()/1000L));
			out.write (",");
			out.write (r.getValue());
			out.write ("], ");
	
			prevt = r.getTimestamp().getTime();
		}
		out.write (" null]}  \n");
	}

out.write ("]"); // end of zones array%>
