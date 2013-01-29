package ie.wombat.ha.server;

import ie.wombat.zigbee.zcl.Cluster;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Date;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.encoders.KeypointPNGEncoderAdapter;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.time.Minute;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.jfree.ui.RectangleInsets;

public class ObserveChartServlet extends HttpServlet {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private static int DEFAULT_WIDTH = 320;
	private static int DEFAULT_HEIGHT = 240;
	private static final int MAX_WIDTH=1400;
	private static final int MAX_HEIGHT=1000;
	
	
	public void doGet (HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
		
		Long observeId = new Long(request.getParameter("observe_id"));
		
		Date startTime = new Date (System.currentTimeMillis() - 48L*3600000L);
		if (request.getParameter("h")!=null) {
			long h = Long.parseLong(request.getParameter("h"));
			startTime = new Date(System.currentTimeMillis() - (long)h*3600000L);
		}
		
		EntityManagerFactory emf = Persistence.createEntityManagerFactory("ie.wombat.ha.server");
		EntityManager em = emf.createEntityManager();
		em.getTransaction().begin();
		
		Observe ob = em.find(Observe.class, observeId);
		
		@SuppressWarnings("unchecked")
		List<ObserveData> data = em.createQuery("from ObserveData "
				+ " where observe.id=:id "
				+ " and timestamp > :ts")
		.setParameter("id", observeId)
		.setParameter("ts", startTime)
		.getResultList();
		
		response.setContentType("image/png");
		OutputStream out = response.getOutputStream();
		
		int width = DEFAULT_WIDTH;
		try {
			width = Integer.parseInt(request.getParameter("width"));
		} catch (Exception e) {
			// ignore
		}
		
		int height = DEFAULT_HEIGHT;
		try {
			height = Integer.parseInt(request.getParameter("height"));
		} catch (Exception e) {
			// ignore
		}
		
		if (width > MAX_WIDTH) {
			width = MAX_WIDTH;
		}
		
		if (height > MAX_HEIGHT) {
			height = MAX_HEIGHT;
		}
		
		String legend;
		switch (ob.getClusterId()) {
		case Cluster.TEMPERATURE_SENSOR:
			legend = "Temperature (°C)";
			break;
		case Cluster.OCCUPANCY:
			legend = "Occupancy";
			break;
		default:
			legend = "Unknown";
			
		}
		TimeSeriesCollection tsc = new TimeSeriesCollection();
		TimeSeries ts = new TimeSeries(legend, Minute.class);
		for (ObserveData d : data) {
			try {
				float f = Float.parseFloat(d.getData());
				if (ob.getClusterId() == Cluster.TEMPERATURE_SENSOR) {
					f /= 100;
				}
				ts.add(new Minute(d.getTimestamp()), f);
			} catch (Exception e) {
				
			}
		}
		tsc.addSeries(ts);
		
		
		JFreeChart chart = ChartFactory.createTimeSeriesChart(
				null, // title
				"Time (UTC)", // x-axis label
				legend, // y-axis label
				tsc, // data
				false, // create legend?
				true, // generate tooltips?
				false // generate URLs?
				);

		chart.setBackgroundPaint(Color.white);
		
		/*
		if (includeDataCredit) {
			TextTitle dataCredit = new TextTitle ("Data courtesy Galway Harbour and the Marine Institute");
			dataCredit.setPosition(RectangleEdge.TOP);
			dataCredit.setHorizontalAlignment(HorizontalAlignment.CENTER);
			chart.addSubtitle(dataCredit);
		}
		*/
		
		
		XYPlot plot = (XYPlot) chart.getPlot();
		plot.setBackgroundPaint(Color.lightGray);
		plot.setDomainGridlinePaint(Color.white);
		plot.setRangeGridlinePaint(Color.white);
		plot.setAxisOffset(new RectangleInsets(5.0, 5.0, 5.0, 5.0));

		plot.setDomainCrosshairVisible(true);
		plot.setRangeCrosshairVisible(true);
		
		XYItemRenderer r = plot.getRenderer();
		if (r instanceof XYLineAndShapeRenderer) {
			XYLineAndShapeRenderer renderer = (XYLineAndShapeRenderer) r;
			renderer.setBaseShapesVisible(true);
			renderer.setBaseShapesFilled(true);
		}

		//ChartUtilities.writeChartAsPNG(out, chart, width, height);
		
		KeypointPNGEncoderAdapter encoder = new KeypointPNGEncoderAdapter();
		encoder.setEncodingAlpha(true);
		encoder.encode(chart.createBufferedImage(width, height, BufferedImage.BITMASK, null) , out);

	}
}
