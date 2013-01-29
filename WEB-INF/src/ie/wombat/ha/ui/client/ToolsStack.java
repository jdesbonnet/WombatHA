package ie.wombat.ha.ui.client;


import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;

import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;

import com.google.gwt.user.client.ui.Widget;

/**
 * Composite that represents devices on the network.
 */
public class ToolsStack extends Composite {

	

	interface Binder extends UiBinder<Widget, ToolsStack> {}

	private static final Binder binder = GWT.create(Binder.class);

	/**
	 * Create a remote service proxy to talk to the server-side GetDevices
	 * service.
	 */
	private final GetDevicesServiceAsync getDevicesService = GWT
			.create(GetDevicesService.class);

	@UiField Button packetLogBtn;
	@UiField Button networkStatusBtn;
	@UiField Button scriptsBtn;

	
	public ToolsStack() {
		initWidget(binder.createAndBindUi(this));
		
		
		packetLogBtn.addClickHandler(new ClickHandler() {
			
			public void onClick(ClickEvent event) {
				AppFrame.INSTANCE.setCenterArea(AppFrame.LOG_VIEW);
			}
		});
		
		networkStatusBtn.addClickHandler(new ClickHandler() {
			public void onClick(ClickEvent event) {
				AppFrame.INSTANCE.setCenterArea(AppFrame.NETWORK_STATUS_VIEW);
			}
		});
		
		scriptsBtn.addClickHandler(new ClickHandler() {
			public void onClick(ClickEvent event) {
				AppFrame.INSTANCE.setCenterArea(AppFrame.SCRIPT_VIEW);
			}
		});
	}
}
