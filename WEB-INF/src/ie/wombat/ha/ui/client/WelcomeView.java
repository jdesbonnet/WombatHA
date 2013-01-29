package ie.wombat.ha.ui.client;

import com.google.gwt.uibinder.client.UiField;

import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;

/**
 * Welcome message on start.
 */
public class WelcomeView extends CenterAreaView {


	@UiField
	Image sheetImage;

	public WelcomeView() {
		super (true);
		setViewTitle ("Welcome");
		vp.add(new Label ("welcome"));
	}

	
}
