package ie.wombat.ha.ui.client;


import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Widget;

/**
 * Composite that represents devices on the network.
 */
public class AppsStack extends Composite {

	@UiField Button heatingAppBtn;
	
	interface Binder extends UiBinder<Widget, AppsStack> {
	}

	private static final Binder binder = GWT.create(Binder.class);

	public AppsStack() {
		initWidget(binder.createAndBindUi(this));

		heatingAppBtn.addClickHandler(new ClickHandler() {
			
			public void onClick(ClickEvent event) {
				AppFrame.INSTANCE.setCenterArea(AppFrame.APP_VIEW);
			}
		});
		
	}

}
