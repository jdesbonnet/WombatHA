package ie.wombat.ha.ui.client;


import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import net.zschech.gwt.comet.client.CometClient;
import net.zschech.gwt.comet.client.CometListener;

import com.google.gwt.canvas.client.Canvas;
import com.google.gwt.canvas.dom.client.Context2d;
import com.google.gwt.canvas.dom.client.CssColor;
import com.google.gwt.canvas.dom.client.FillStrokeStyle;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Grid;


/**
 * Display device information
 */
public class LogView extends CenterAreaView implements CometListener {

	private static final int NROW = 32;
	
	//private VerticalPanel packetPanel;
	private Grid packetTable;
	private CometClient client; 
	private int row=0;
	
	public LogView() {
		super(false);
		setViewTitle("Log View");

		FlowPanel buttonPanel = new FlowPanel();
		
		// Clear button will clear packet log
		Button clearBtn = new Button("Clear");
		clearBtn.addClickHandler(new ClickHandler() {
			public void onClick(ClickEvent event) {
				//packetTable.clear();
				for (int i = 0; i < NROW; i++) {
					packetTable.setHTML(i,0,"");
				}
			}
		});
		buttonPanel.add(clearBtn);
		vp.add(buttonPanel);
		

		//buttonPanel.add(new TimeChart(800, 200));


		//packetPanel = new VerticalPanel();
		packetTable = new Grid(NROW, 1);
	    //packetPanel.getCellFormatter().setWidth(0, 2, "256px");
	    //packetPanel.getColumnFormatter().setWidth(0, "150px");
	    //packetPanel.getColumnFormatter().setStyleName(1, "ps");

	    
		vp.add(packetTable);
		
		client = new CometClient("/WombatHA/packetlog", this);
		//client.start();
		

	}
	
	public void start() {
		client.start();
	}
	public void stop() {
		client.stop();
	}
	
	/*
	public void addLogEntry (NICAPIPacket apiPacket) {
		//packetPanel.add(new HTMLPanel(logEntry));
		packetTable.setHTML(row,0,apiPacket.timestamp);
		packetTable.setHTML(row++, 1, "<tt>" + apiPacket.packetHex + "</tt>");
	}
	*/
	
	public void addLogEntry (String apiPacket) {
		// Move rows down one
		for (int i = NROW-1; i > 0; i--) {
			String html = packetTable.getHTML(i-1, 0);
			packetTable.setHTML(i,0,html);
		}
		//packetPanel.add(new HTMLPanel(logEntry));
		//packetPanel.setHTML(row,0,apiPacket.timestamp);
		packetTable.setHTML(0, 0,  apiPacket );
	}

	public void onConnected(int heartbeat) {		
	}

	public void onDisconnected() {		
	}

	public void onError(Throwable exception, boolean connected) {		
	}

	public void onHeartbeat() {		
	}

	public void onRefresh() {		
	}

	public void onMessage(List<? extends Serializable> messages) {
		
		//Window.alert("Got " + messages.size() + " messages");
		
		for (Serializable message : messages) {
			//NICAPIPacket apiPacket = (NICAPIPacket)message;
			String apiPacket = (String)message;
			addLogEntry(apiPacket);
		}
	}
}
