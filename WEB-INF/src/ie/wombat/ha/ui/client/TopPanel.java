
package ie.wombat.ha.ui.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.SpanElement;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;

/**
 * The top panel (logo, login status, log out link etc)
 */
public class TopPanel extends Composite {

  interface Binder extends UiBinder<Widget, TopPanel> { }
  private static final Binder binder = GWT.create(Binder.class);

  @UiField Anchor signOutLink;
  @UiField Anchor aboutLink;
  @UiField SpanElement networkId;

  
  public TopPanel() {
    initWidget(binder.createAndBindUi(this));
    
    /*
    networkId.addChangeHandler(new ChangeHandler() {
		
		public void onChange(ChangeEvent event) {
			Long newNetworkId = new Long(networkId.getText());
			//AppFrame.INSTANCE.setNetworkId(newNetworkId);
			
		}
	});
	*/
  }

  @UiHandler("aboutLink")
  void onAboutClicked(ClickEvent event) {
    AboutDialog aboutDialog = new AboutDialog();
    aboutDialog.show();
    aboutDialog.center();
  }

  @UiHandler("signOutLink")
  void onSignOutClicked(ClickEvent event) {
    Window.alert("TODO: logout");
  }
}
