
package ie.wombat.ha.ui.client;

import java.util.ArrayList;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;


/**
 * A composite for displaying scan batch thumbnails
 */
public class ThumbnailView extends CenterAreaView {

  private ArrayList<FlowPanel> tnPanels = new ArrayList<FlowPanel>();
  
  //CometClient cometClient;
  
  FlowPanel thumbnailsPanel;
  
  public ThumbnailView() {
    super(false);
    thumbnailsPanel = new FlowPanel();
    vp.add(thumbnailsPanel);
  }
 
}
