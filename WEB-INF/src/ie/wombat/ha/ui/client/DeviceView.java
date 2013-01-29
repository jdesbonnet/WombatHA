package ie.wombat.ha.ui.client;

import com.google.gwt.core.client.GWT;

import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.HTMLPanel;

/**
 * Display device information
 */
public class DeviceView extends CenterAreaView {

	/**
	 * Create a remote service proxy to talk to the server-side GetDevices
	 * service.
	 */
	private final GetDevicesServiceAsync devicesService = GWT
			.create(GetDevicesService.class);
	private final ZigBeeServiceAsync zigbeeService = GWT
			.create(ZigBeeService.class);

	public DeviceView() {
		super(false);
	}

	public void setDeviceInView(final DeviceInfo devInfo) {

			
		//vp.add(new AboutDeviceDialog(deviceId));
		vp.add(new DevicePanel(devInfo));
		
		Long deviceId = devInfo.getId();
		
		devicesService.getDeviceInfo(deviceId, new AsyncCallback<DeviceInfo>() {

			public void onSuccess(DeviceInfo result) {
				// Window.alert("Got DeviceInfo " + result.addr64.toString());
				setViewTitle("Device View. " + result.name);
				line0.setInnerText(result.addr64.toString());
			}

			public void onFailure(Throwable caught) {
				Window.alert(caught.toString());
			}
		});

		/*
		zigbeeService.getAttributeValue(deviceId, 0x0, 0x0,
				new AsyncCallback<String>() {

					public void onSuccess(String result) {
						line0.setInnerText("ZCL Version: " + result);
					}

					public void onFailure(Throwable caught) {
						Window.alert(caught.toString());
					}

		});
		*/

	}
}
