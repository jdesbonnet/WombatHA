package ie.wombat.ha.ui.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;

import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.TreeItem;

/**
 * Display all devices on network with summary status information
 */
public class NetworkView extends CenterAreaView {

	private static final int NROW = 16;
	/**
	 * Create a remote service proxy to talk to the server-side GetDevices
	 * service.
	 */
	private final GetDevicesServiceAsync devicesService = GWT
			.create(GetDevicesService.class);
	
	private final AddressServiceAsync addrService = GWT
			.create(AddressService.class);
	
	
	private Grid deviceStatusGrid;
	private DeviceInfo[] devices = null;
	
	public NetworkView() {
		super(false);
		
		deviceStatusGrid = new Grid(NROW, 6);
		deviceStatusGrid.setText(0, 0, "Name");
		deviceStatusGrid.setText(0, 1, "Address (64 bit)");
		deviceStatusGrid.setText(0, 2, "Address (16 bit)");
		deviceStatusGrid.setText(0, 3, "Battery");
		deviceStatusGrid.setText(0, 4, "Last Rx");

		deviceStatusGrid.setText(0, 5, "Test");
		
		setViewTitle("Network Status");

		vp.add(deviceStatusGrid);
		
		update();
	

	    // Create a new timer
	    Timer elapsedTimer = new Timer () {
	      public void run() {
	    	  update();
	      }
	    };
	    elapsedTimer.scheduleRepeating(60000);	    
	}

	public void update () {
		Long networkId = AppFrame.INSTANCE.getNetworkId();
		devicesService.getDevices(networkId,
				new AsyncCallback<DeviceInfo[]>() {
					public void onFailure(Throwable caught) {
						Window.alert("oops: " + caught.toString());
					}

					public void onSuccess(DeviceInfo[] result) {
						
						setViewTitle("Network Status: last update = " + System.currentTimeMillis());

						devices = result;
						for (int i = 0; i < result.length; i++) {
							final DeviceInfo devInfo = result[i];
							deviceStatusGrid.setText(i+1, 0, devInfo.name);
							deviceStatusGrid.setText(i+1, 1, devInfo.addr64);
							deviceStatusGrid.setText(i+1, 2, devInfo.addr16);
							String batteryStatusString;
							if (devInfo.isBatteryPowered()) {
								switch (devInfo.batteryStatus) {
								case 0:
									batteryStatusString = "OK";
									break;
								case 1:
									batteryStatusString = "LOW";
									break;
								default:
									batteryStatusString = "ERR";
									break;
								} 
							} else {
								batteryStatusString = "n/a";
							}
							deviceStatusGrid.setText(i+1, 3, batteryStatusString);
							
							deviceStatusGrid.setText(i+1, 4, ""+(devInfo.lastRxTime/1000) );
							final Button testBtn = new Button("Test");
							testBtn.addClickHandler(new ClickHandler() {
								
								public void onClick(ClickEvent event) {
									//Window.alert(devInfo.addr64);
									addrService.getDeviceAddr16(devInfo.networkId, devInfo.addr64, new AsyncCallback<String>() {
										public void onFailure(Throwable caught) {
											Window.alert("oops: " + caught.toString());
										}
										public void onSuccess(String result) {
											testBtn.setText(result);
										}
									});
								}
							});
							deviceStatusGrid.setWidget(i+1,5,testBtn);
						}
						
						
					}
				});
	}

	
}
