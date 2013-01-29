package ie.wombat.ha.ui.client;


import java.util.ArrayList;
import java.util.Date;

import sun.font.LayoutPathImpl.EndType;

import ie.wombat.ha.ui.client.DevicePanel.Binder;

import com.google.gwt.canvas.client.Canvas;
import com.google.gwt.canvas.dom.client.Context2d;
import com.google.gwt.canvas.dom.client.CssColor;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.SpanElement;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.GestureChangeEvent;
import com.google.gwt.event.dom.client.GestureChangeHandler;
import com.google.gwt.event.dom.client.MouseDownEvent;
import com.google.gwt.event.dom.client.MouseDownHandler;
import com.google.gwt.event.dom.client.MouseMoveEvent;
import com.google.gwt.event.dom.client.MouseMoveHandler;
import com.google.gwt.event.dom.client.MouseUpEvent;
import com.google.gwt.event.dom.client.MouseUpHandler;
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.i18n.client.NumberFormat;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.DeferredCommand;
import com.google.gwt.user.client.IncrementalCommand;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

/**
 * Misnomer right now: this started off as a simple temperature vs time chart. It has now become 
 * the UI for a heating application. TODO: refactor.
 * 
 * @author joe
 *
 */
public class TimeChart extends Composite {

	interface Binder extends UiBinder<Widget, TimeChart> {
	}

	private static final Binder uiBinder = GWT.create(Binder.class);

	private static final NumberFormat temperatureFormat = NumberFormat.getFormat("##.0");
	private static final NumberFormat temperatureAxisFormat = NumberFormat.getFormat("##");
	private static final NumberFormat currencyFormat = NumberFormat.getFormat("#.00");
	private static final DateTimeFormat hourMinuteFormat = DateTimeFormat.getFormat("HH:mm");
	private static final DateTimeFormat dateFormat = DateTimeFormat.getFormat("dd MMM");
	
	final CssColor bgColor = CssColor.make("rgb(240,240,240)");
	final CssColor chartBgColor = CssColor.make("rgb(220,220,0");
	
	final CssColor majorGridColor = CssColor.make("rgb(128,128,128)");
	final CssColor minorGridColor = CssColor.make("rgb(200,200,200)");
	final CssColor chartBorderColor = CssColor.make("rgb(0,0,0)");
	
	final CssColor triggerCursorColor = CssColor.make("rgb(255,0,0)");
	final CssColor dataPointColor = CssColor.make("rgb(0,0,255)");

	
	
	private int width; 
	private int height;
	private static final int paddingTop = 20;
	private static final int paddingBottom = 40;
	private static final int paddingLeft = 40;
	private static final int paddingRight = 40;
	
	private static final int x0 = paddingLeft;
	private static final int y0 = paddingTop;
	private int chartWidth;
	private int chartHeight;
	
	private Canvas canvas;

	private double chartMinTemperature = 14;
	private double chartMaxTemperature = 28;
	private double triggerTemperature = 19;

	private double temperatureRange;
	
	private long chartStartTime;
	private long chartEndTime;
	private long timeRange = 48L*3600000L;
	
	
	private Context2d gc;

	private boolean triggerTemperatureDragMode = false;
	
	@UiField SpanElement heading;
	@UiField Panel canvasWrapper;
	@UiField Button incTriggerBtn;
	@UiField Button decTriggerBtn;
	@UiField Button tr3hBtn;
	@UiField Button tr12hBtn;
	@UiField Button tr1dBtn;
	@UiField Button tr2dBtn;
	@UiField Button tr7dBtn;
	
	@UiField Button z1OnBtn;
	@UiField Button z1OffBtn;
	@UiField Button z2OnBtn;
	@UiField Button z2OffBtn;
	@UiField Button z3OnBtn;
	@UiField Button z3OffBtn;
	
	@UiField SpanElement triggerTemperatureValue;
	@UiField SpanElement calcCostPerDay;
	@UiField SpanElement minTemperatureField;
	@UiField SpanElement maxTemperatureField;
	@UiField SpanElement meanTemperatureField;
	
	@UiField SpanElement systemStatus;
	
	private final DataServiceAsync dataService = GWT.create(DataService.class);
	private final HeatingServiceAsync heatingService = GWT.create(HeatingService.class);

	// Data returned from server
	private Data[] chartData;
	
	// Post processed data
	private ArrayList<Double>xArray;
	private ArrayList<Double>yArray;
	
	private Long networkId;
	
	public TimeChart (final Long networkId, int width, int height) {
		
		this.networkId = networkId;
		
		initWidget(uiBinder.createAndBindUi(this));

		this.width = width;
		this.height = height;
		
		chartWidth = width - paddingLeft - paddingRight;
		chartHeight = height - paddingTop - paddingBottom;
		
		temperatureRange = chartMaxTemperature - chartMinTemperature;
		
		canvas = Canvas.createIfSupported();		
		canvas.setWidth(width + "px");
		canvas.setHeight(height + "px");
		canvas.setCoordinateSpaceWidth(width);
		canvas.setCoordinateSpaceHeight(height);
		
		canvasWrapper.add(canvas);
		
		incTriggerBtn.addClickHandler(new ClickHandler() {
			public void onClick(ClickEvent event) {
				setTriggerTemperature(triggerTemperature+0.5);
				sendTriggerTemperature();
			}
		});
		
		decTriggerBtn.addClickHandler(new ClickHandler() {
			public void onClick(ClickEvent event) {
				setTriggerTemperature(triggerTemperature-0.5);
				sendTriggerTemperature();
			}
		});
		
		tr3hBtn.addClickHandler(new ClickHandler() {
			public void onClick(ClickEvent event) {
				setTimeRange(3L*3600000L);
			}
		});
		tr12hBtn.addClickHandler(new ClickHandler() {
			public void onClick(ClickEvent event) {
				setTimeRange(12L*3600000L);
			}
		});
		
		
		tr1dBtn.addClickHandler(new ClickHandler() {
			public void onClick(ClickEvent event) {
				setTimeRange(24L*3600000L);
			}
		});
		tr2dBtn.addClickHandler(new ClickHandler() {
			public void onClick(ClickEvent event) {
				setTimeRange(48L*3600000L);
			}
		});
		tr7dBtn.addClickHandler(new ClickHandler() {
			public void onClick(ClickEvent event) {
				setTimeRange(7L*24L*3600000L);
			}
		});
		
		z1OnBtn.addClickHandler(new ClickHandler() {
			public void onClick(ClickEvent event) {
				heatingService.setState(networkId,0,true, null);
			}
		});
		z1OffBtn.addClickHandler(new ClickHandler() {
			public void onClick(ClickEvent event) {
				heatingService.setState(networkId,0,false, null);
			}
		});
		z2OnBtn.addClickHandler(new ClickHandler() {
			public void onClick(ClickEvent event) {
				heatingService.setState(networkId,1,true, null);
			}
		});
		z2OffBtn.addClickHandler(new ClickHandler() {
			public void onClick(ClickEvent event) {
				heatingService.setState(networkId,1,false, null);
			}
		});
		z3OnBtn.addClickHandler(new ClickHandler() {
			public void onClick(ClickEvent event) {
				heatingService.setState(networkId,2,true, null);
			}
		});
		z3OffBtn.addClickHandler(new ClickHandler() {
			public void onClick(ClickEvent event) {
				heatingService.setState(networkId,2,false, null);
			}
		});
		
		gc = canvas.getContext2d();		
	
		
		canvas.addGestureChangeHandler(new GestureChangeHandler() {
			public void onGestureChange(GestureChangeEvent event) {
				Window.alert ("got gesture=" + event);		
			}
		});
		
		canvas.addMouseDownHandler(new MouseDownHandler() {
			public void onMouseDown(MouseDownEvent event) {				
				int x = event.getRelativeX(canvas.getElement());
				int y = event.getRelativeY(canvas.getElement());
				int yt = (int)((y0 + chartHeight) 
				- (double)chartHeight*(triggerTemperature-chartMinTemperature)/temperatureRange);
				
				if ( (x>= x0+chartWidth) && (x < x0+chartWidth+16)
						&& (y >= yt-16) && (y<yt+16)) {
					triggerTemperatureDragMode = true;
				}
				
			}
		});
		
		canvas.addMouseUpHandler(new MouseUpHandler() {
			
			public void onMouseUp(MouseUpEvent event) {
				if ( ! triggerTemperatureDragMode) {
					
					return;
				}
				
				triggerTemperatureDragMode = false;
				int x = event.getRelativeX(canvas.getElement());
				int y = event.getRelativeY(canvas.getElement());
				
				triggerTemperature = chartMaxTemperature 
				- temperatureRange * (double)(y-y0)/(double)chartHeight;
				// Round to nearest 0.5 degree
				triggerTemperature = (int)((triggerTemperature+0.25)*2) / 2.0;
				redrawChart();
				
				sendTriggerTemperature();
			}
		});
		
		// Interested in mouse move if trigger is in drag mode.
		canvas.addMouseMoveHandler(new MouseMoveHandler() {
			
			public void onMouseMove(MouseMoveEvent event) {
				if ( ! triggerTemperatureDragMode) {
					return;
				}
				int x = event.getRelativeX(canvas.getElement());
				int y = event.getRelativeY(canvas.getElement());
				
				// Allow a good region around the trigger cursor
				// where the drag will still work. +/-32px sounds good.
				if ( (x>= x0+chartWidth-32) && (x < x0+chartWidth+32)
						&& (y >= y0) && (y<y0+chartHeight)) {
					double t = chartMaxTemperature 
					- temperatureRange * (double)(y-y0)/(double)chartHeight;
					// Round to nearest 0.5 degree
					t = (int)((t+0.25)*2) / 2.0;
					
					// Avoid redraw unless necessary
					if (t != triggerTemperature) {
						triggerTemperature = t;
						redrawChart();
					}
				}
					
			}	
		});
		
		heatingService.getSystemStatus(networkId,new AsyncCallback<Integer>() {

			public void onFailure(Throwable caught) {
				systemStatus.setInnerText("ERR: " + caught.toString());
			}

			public void onSuccess(Integer result) {
				//systemStatus.setInnerText("code=" + result);
			}
		});
		
		heatingService.getTargetTemperature(networkId,new AsyncCallback<Double>() {

			public void onFailure(Throwable caught) {
				systemStatus.setInnerText("ERR: " + caught.toString());
			}

			public void onSuccess(Double result) {
				triggerTemperature = result;
				redrawChart();
			}
		});
		
		
		Timer dataDownloadTimer = new Timer () {
			public void run () {
				getDataFromServer();
				redrawChart();
			}
		};
		dataDownloadTimer.scheduleRepeating(10000);			
	}
	
	/**
	 * Called externally to set the time period that the chart should display data for.
	 * entTime=now and startTime=now-timeRange.
	 * @param timeRange
	 */
	public void setTimeRange (long timeRange) {
		this.timeRange = timeRange;
		this.chartEndTime = System.currentTimeMillis();
		this.chartStartTime = this.chartEndTime - timeRange;
		
		// Redraw to show new time range as quickly as possible to give illusion 
		// of responsiveness
		//chartData = null;
		//xArray = null;
		//yArray = null;
		processData();
		drawUnpopulatedChart();
		
		// Get data for new time range from server and draw data when available
		getDataFromServer();
		redrawChart();
	}
	
	/**
	 * Retrieve data from server and post-process.
	 */
	private void getDataFromServer () {
		// Get data
		long now = System.currentTimeMillis();
		chartStartTime = now - timeRange;
		chartEndTime = now;
		
		dataService.getTemperatureHistory(networkId, new Date(chartStartTime), new Date(chartEndTime), 
				new AsyncCallback<Data[]>() {

					public void onFailure(Throwable caught) {
						//Window.alert(caught.toString());
					}

					public void onSuccess(Data[] result) {
						chartData = result;
						//Window.alert("Got " + chartData.length + " data items");
						processData();
						redrawChart();
					}
			
				});
	}
	
	private void setTriggerTemperature(double t) {
		triggerTemperature = t;
		if (triggerTemperature>chartMaxTemperature) {
			triggerTemperature = chartMaxTemperature;
		}
		if (triggerTemperature<chartMinTemperature) {
			triggerTemperature = chartMinTemperature;
		}
		redrawChart();
	}
	private void sendTriggerTemperature () {
		// Send new target temperature back to server
		heatingService.setTargetTemperature(networkId,triggerTemperature, new AsyncCallback<Void>() {
			public void onFailure(Throwable caught) {
				// nothing to do
			}
			public void onSuccess(Void result) {
				// nothing to do
			}
		});
	}
	
	private void redrawChart () {
		drawUnpopulatedChart();
		drawTriggerLevel(triggerTemperature);
		triggerTemperatureValue.setInnerText(temperatureFormat.format(triggerTemperature) + "°C");
		double ambientTemperature = 5;
		double cost = (triggerTemperature - ambientTemperature) / 4;
		calcCostPerDay.setInnerText("€"+currencyFormat.format(cost) + "/day");
		plotDataPoints(chartData);
	}
	
	private void drawUnpopulatedChart () {
				
		// Draw canvas background
		gc.setFillStyle(bgColor);
		gc.fillRect(0, 0, width, height);
		
		// Draw chart area background
		gc.setFillStyle(chartBgColor);
		gc.fillRect(x0,y0,chartWidth,chartHeight);
	
		
		gc.beginPath();
		
		int i;
		
		gc.setLineWidth(1.0);
		
		// Draw minor grid (vertical lines)
		double minorGridInterval = (15L*60L*1000L); // 15 min
		if (timeRange > 24L*3600000L) {
			minorGridInterval = 3L*3600000L;
		}
		
		double minorGridSpacing = minorGridInterval*((double)chartWidth/timeRange);
		{
		long t = (long)(Math.ceil(chartStartTime / minorGridInterval) * minorGridInterval);
		double x = x0 + chartWidth * (t-chartStartTime)/timeRange;
		gc.setStrokeStyle(minorGridColor);
		while ( x < (x0+chartWidth)) {
			gc.beginPath();
			drawVerticalDashedLine(x, y0, y0+chartHeight);
			x += minorGridSpacing;
			gc.stroke();
		}
		}

		// Draw major y axis grid (horizontal lines)
		gc.setStrokeStyle(minorGridColor);
		gc.setFillStyle("black");
		gc.beginPath();
		double majorYGridSpacing = 2.0;
		if (chartMaxTemperature-chartMinTemperature > 20) {
			majorYGridSpacing = 5.0;
		}
		
		for (double t = chartMinTemperature; t <= chartMaxTemperature; t += majorYGridSpacing) {
			double y = (y0 + chartHeight) - (t-chartMinTemperature)*(chartHeight/temperatureRange);
			gc.beginPath();
			drawHorizontalDashedLine(y, x0, x0 + chartWidth);
			gc.stroke();
			gc.fillText(temperatureAxisFormat.format(t), x0 - 20, y);
			gc.fill();
		}
		
		// Draw 0C axis if visible
		if (chartMaxTemperature<= 0 && chartMaxTemperature > 0) {
			double y = (y0 + chartHeight) - (0-chartMinTemperature)*(chartHeight/temperatureRange);
			gc.beginPath();
			gc.moveTo(x0, y);
			gc.lineTo(x0 + chartWidth,y);
			gc.stroke();
		}
		
		// Draw major grid (vertical lines)
		boolean displayDate = false;
		long majorGridInterval = (60L*60L*1000L); // 60 min
		if (timeRange > 24L*3600000L) {
			majorGridInterval = 24L*3600000L;
			displayDate = true;
		}
		double majorGridSpacing = majorGridInterval*((double)chartWidth/(double)timeRange);
		{
			long t = chartStartTime - chartStartTime % majorGridInterval;
			t += majorGridInterval;
			double x = x0 + (chartWidth * (t-chartStartTime))/(double)timeRange;
			gc.setStrokeStyle(majorGridColor);
			gc.setFillStyle("black");
			while ( x < (x0+chartWidth)) {
				gc.beginPath();
				gc.moveTo(x-0.5, y0);
				gc.lineTo(x-0.5, y0+chartHeight);
				gc.stroke();
				Date d = new Date(t);
				gc.fillText(hourMinuteFormat.format(d), 
					x-10, y0+chartHeight+10);
				if (displayDate) {
					gc.fillText(dateFormat.format(d), x-10,  y0+chartHeight+20);
				}
				x += majorGridSpacing;
				t += majorGridInterval;
			}
		}
		
		/*
		// Draw major vertical grid
		int nMajorGridLines = 6;
		gc.setStrokeStyle(majorGridColor);
		double majorTickInterval = chartWidth / nMajorGridLines;
		gc.setStrokeStyle(majorGridColor);
		gc.setFillStyle(majorGridColor);
		for (i = 0; i < 24; i++) {
			gc.beginPath();
			gc.moveTo(x0+i*majorTickInterval-0.5, y0);
			gc.lineTo(x0+i*majorTickInterval-0.5, y0+chartHeight);
			gc.stroke();
			
			gc.fillText(""+i, x0+i*majorTickInterval, y0+chartHeight+10);
		}
		*/
	
		// Draw border around chart area
		gc.setLineWidth(1.0);
		gc.beginPath();
		gc.setStrokeStyle(chartBorderColor);
		gc.moveTo(x0-0.5, y0-0.5);
		gc.lineTo(x0+chartWidth-0.5,y0-0.5);
		gc.lineTo(x0+chartWidth-0.5,y0+chartHeight-0.5);
		gc.lineTo(x0-0.5,y0+chartHeight-0.5);
		gc.lineTo(x0-0.5,y0-0.5);
		gc.stroke();
		
	}
	
	private void drawChartGrid () {
		
	}
	private void drawTriggerLevel (double t) {			
		double y = (y0 + chartHeight) - (t-chartMinTemperature)*(chartHeight/(temperatureRange));

		// Draw trigger level line
		gc.setFillStyle(triggerCursorColor);
		gc.beginPath();
		gc.moveTo(x0 + chartWidth, y);
		gc.lineTo(x0 + chartWidth + 12, y-6);
		gc.lineTo(x0 + chartWidth + 12, y+6);
		gc.fill();
		
		// Draw cursor
		gc.setStrokeStyle(triggerCursorColor);
		gc.beginPath();
		gc.moveTo(x0, y-0.5);
		gc.lineTo(x0+chartWidth,y-0.5);
		gc.stroke();
		
		// Draw value next to cursor
		gc.fillText(temperatureFormat.format(t), x0 + chartWidth + 14, y);
		
	}
	
	private void drawVerticalDashedLine (double x, double y1, double y2) {
		double y = y1;
		while (y < y2) {
			gc.moveTo(x-0.5, y);
			y+=4.0;
			gc.lineTo(x-0.5, y);
			y+=4.0;
		}
		gc.stroke();
	}
	private void drawHorizontalDashedLine (double y, double x1, double x2) {
		double x = x1;
		while (x < x2) {
			gc.moveTo(x, y-0.5);
			x+=4.0;
			gc.lineTo(x, y-0.5);
			x+=4.0;
		}
		gc.stroke();
	}
	
	/**
	 * The chart data will have to be redrawn frequently so do as much of the CPU intensive
	 * processing on the data only once and store simple screen x,y coordinates for fast redrawing.
	 * 
	 * @param points
	 */
	private void processData () {
		if (chartData == null || chartData.length == 0) {
			return;
		}
		
		long t;
		double x,y,v;
		
		xArray = new ArrayList<Double>(chartData.length);
		yArray = new ArrayList<Double>(chartData.length);
		
		double min = 1000;
		double max = -1000;
		
		long timeRange = chartEndTime - chartStartTime;
		
		double meanTemperature = 0;
		
		// Find min, max and mean
		for (Data d : chartData) {
			//v = Double.parseDouble(d.value) / 100.0;
			v = Double.parseDouble(d.value);
			if (max<v) max=v;
			if (min>v) min=v;
			meanTemperature += v;
		}
		meanTemperature /= chartData.length;
		
		chartMinTemperature = Math.floor(min / 2.0) * 2.0;
		chartMaxTemperature = Math.ceil(max/2.0)*2.0;
		temperatureRange = chartMaxTemperature - chartMinTemperature;
		if (temperatureRange < 4) {
			chartMinTemperature -= 2.0;
			chartMaxTemperature += 2.0;
			temperatureRange += 4.0;
		}
		
		for (Data d : chartData) {
			t = d.timestamp.getTime();
			x = x0 + (t-chartStartTime)*chartWidth/(double)timeRange;
			//v = Double.parseDouble(d.value) / 100.0;
			v = Double.parseDouble(d.value) ;
			y = (y0 + chartHeight) - (v-chartMinTemperature)*(chartHeight/temperatureRange);
			xArray.add(x);
			yArray.add(y);	
		}
		
		
		minTemperatureField.setInnerText(temperatureFormat.format(min));
		maxTemperatureField.setInnerText(temperatureFormat.format(max));
		meanTemperatureField.setInnerText(temperatureFormat.format(meanTemperature));
	}
	
	private void plotDataPoints (Data[] points) {
		
		if (xArray == null || yArray == null) {
			return;
		}
		gc.setFillStyle(dataPointColor);

		double d2,dx,dy;
		int lastPoint = 0;
		int n = xArray.size();
		for (int i = 1; i < n; i++) {
			
			// It can happen if the time scale is changed, some of the existing points 
			// will no longer fit on the chart. Therefore need to check if still 
			// inside the chart area.
			if (xArray.get(i)<x0) {
				continue;
			}
			
			dx = xArray.get(i) - xArray.get(lastPoint);
			dy = yArray.get(i) - yArray.get(lastPoint);
			d2 = dx*dx + dy*dy;
			if (d2 > 9) {
				gc.fillRect(xArray.get(i)-2.0, yArray.get(i)-2.0, 4.0, 4.0);
				lastPoint = i;
			}
			
		}
	}
}
