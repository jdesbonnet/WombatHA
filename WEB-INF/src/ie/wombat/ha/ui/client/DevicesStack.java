package ie.wombat.ha.ui.client;


import com.google.gwt.core.client.GWT;
import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.logical.shared.SelectionHandler;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiTemplate;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Tree;
import com.google.gwt.user.client.ui.TreeItem;
import com.google.gwt.user.client.ui.Widget;

/**
 * Composite that represents devices on the network.
 */
public class DevicesStack extends Composite {

	@UiField
	Tree tree;

	interface Binder extends UiBinder<Widget, DevicesStack> {
	}

	private static final Binder binder = GWT.create(Binder.class);

	/**
	 * Create a remote service proxy to talk to the server-side GetDevices
	 * service.
	 */
	private final GetDevicesServiceAsync getDevicesService = GWT
			.create(GetDevicesService.class);

	public DevicesStack() {
		initWidget(binder.createAndBindUi(this));

		getDeviceList();

		tree.addSelectionHandler(new SelectionHandler<TreeItem>() {
			public void onSelection(SelectionEvent<TreeItem> event) {
				DeviceInfo devInfo = (DeviceInfo)event.getSelectedItem().getUserObject();
				
				// Ensure device view is in center area
				AppFrame.INSTANCE.setCenterArea(AppFrame.DEVICE_VIEW);
				AppFrame.INSTANCE.deviceView.setDeviceInView(devInfo);
				// Set device on device view
			}
		});

	}

	/**
	 * Get list of devices from server.
	 */
	public void getDeviceList() {
		Long networkId = AppFrame.INSTANCE.getNetworkId();
		getDevicesService.getDevices(networkId,
				new AsyncCallback<DeviceInfo[]>() {
					public void onFailure(Throwable caught) {
						Window.alert("oops: " + caught.toString());
					}

					public void onSuccess(DeviceInfo[] result) {
						for (DeviceInfo devInfo : result) {
							TreeItem item = new TreeItem(devInfo.name);
							item.setUserObject(devInfo);
							tree.addItem(item);
						}
					}
				});
	}
}
