package ie.wombat.ha.ui.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.Widget;


/**
 * Display device information
 */
public class ScriptPanel extends Composite {

	private final ScriptServiceAsync scriptService = GWT.create(ScriptService.class);	
	
	interface Binder extends UiBinder<Widget, ScriptPanel> {
	}

	private static final Binder uiBinder = GWT.create(Binder.class);

	@UiField TextArea scriptIn;
	@UiField TextArea scriptOut;
	@UiField Button execBtn;
	
	public ScriptPanel() {
				
		final Long networkId = AppFrame.INSTANCE.getNetworkId();

		initWidget(uiBinder.createAndBindUi(this));
		
		execBtn.addClickHandler(new ClickHandler() {
			
			public void onClick(ClickEvent event) {
				String script = scriptIn.getValue();
				scriptService.eval(networkId, script, new AsyncCallback<String>() {

					public void onFailure(Throwable caught) {						
					}

					public void onSuccess(String result) {
						scriptOut.setValue(result);
					}
				});
				
			}
		});
			
			
	}
}
