
package ie.wombat.ha.ui.client;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Style.Overflow;

import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.resources.client.CssResource.NotStrict;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.DockLayoutPanel;

import com.google.gwt.user.client.ui.SplitLayoutPanel;
import com.google.gwt.user.client.ui.Widget;

import com.google.gwt.user.client.ui.RootLayoutPanel;

public class AppFrame implements EntryPoint {

	
  interface Binder extends UiBinder<DockLayoutPanel, AppFrame> { }
  
  interface GlobalResources extends ClientBundle {
    @NotStrict
    @Source("global.css")
    CssResource css();
  }
  
  private static final Binder binder = GWT.create(Binder.class);
  
  /**
	* Create a remote service proxy to talk to the server-side GetDevices service.
	*/
  //private final GetDevicesServiceAsync getDevicesService = GWT
	//		.create(GetDevicesService.class);

  public static final String DEVICE_VIEW = "device_view";
  public static final String APP_VIEW = "app_view";
  public static final String WELCOME_VIEW = "welcome_view";
  public static final String THUMBNAIL_VIEW = "thumbnail_view";
  public static final String NETWORK_STATUS_VIEW = "device_status_view";
  public static final String LOG_VIEW = "log_view";
  public static final String SCRIPT_VIEW = "script_view";

  // TODO: experimental
  static AppFrame INSTANCE; // TODO: not the usual way of doing singleton!! FIX.
  
  @UiField TopPanel topPanel;
  @UiField SplitLayoutPanel contentArea;
  
 NetworkView networkView;
 DeviceView deviceView;
 AppView appView;
 WelcomeView welcomeView;
 LogView logView;
 ScriptView scriptView;
 
 private Widget currentCenterAreaWidget = null;

 private Long networkId = null;
 
  /**
   * This method constructs the application user interface by instantiating
   * controls and hooking up event handler.
   */
  public void onModuleLoad() {
	  
	 INSTANCE = this; // TODO: yeuch.
	 networkId = new Long(com.google.gwt.user.client.Window.Location.getParameter("network_id"));
	    
    // Inject global styles.
    GWT.<GlobalResources>create(GlobalResources.class).css().ensureInjected();

    // Create the UI defined in Mail.ui.xml.
    DockLayoutPanel outer = binder.createAndBindUi(this);

    // Get rid of scrollbars, and clear out the window's built-in margin,
    // because we want to take advantage of the entire client area.
    Window.enableScrolling(false);
    Window.setMargin("0px");

    // Special-case stuff to make topPanel overhang a bit.
    Element topElem = outer.getWidgetContainerElement(topPanel);
    topElem.getStyle().setZIndex(2);
    topElem.getStyle().setOverflow(Overflow.VISIBLE);

    // Add the outer panel to the RootLayoutPanel, so that it will be
    // displayed.
    RootLayoutPanel root = RootLayoutPanel.get();
    root.add(outer);
	
   
    
    //currentCenterAreaWidget = thumbnailView;
    
    // TODO: really awful way of sharing this object with Projects
    // Why not make this a singleton?
   
    // TODO: lazy instantiation
    networkView = new NetworkView();
    welcomeView = new WelcomeView();
    scriptView = new ScriptView();
    
    //contentArea.add(welcomeView);
    contentArea.add(networkView);
    //currentCenterAreaWidget = appView;
    currentCenterAreaWidget = networkView;

  }
  
  public void setCenterArea(String pane) {
	  
	  //
	  // TODO: replace this with an array
	  //
	  if (DEVICE_VIEW.equals(pane)) {
		  if (currentCenterAreaWidget != deviceView) {
			  contentArea.remove(currentCenterAreaWidget);
			  if (deviceView == null) {
				  deviceView = new DeviceView();
			  }
			  contentArea.add(deviceView);
			  currentCenterAreaWidget = deviceView;
		  }
	  }
	  if (APP_VIEW.equals(pane)) {
		  if (currentCenterAreaWidget != appView) {
			  contentArea.remove(currentCenterAreaWidget);
			  if (appView == null) {
				  appView = new AppView();
			  }
			  contentArea.add(appView);
			  currentCenterAreaWidget = appView;
		  }
	  }
	  if (WELCOME_VIEW.equals(pane)) {
		  if (currentCenterAreaWidget != welcomeView) {
			  contentArea.remove(currentCenterAreaWidget);
			  contentArea.add(welcomeView);
			  currentCenterAreaWidget = welcomeView;
		  }
	  }
	
	  if (LOG_VIEW.equals(pane)) {
		  if (currentCenterAreaWidget != logView) {
			  contentArea.remove(currentCenterAreaWidget);
			  if (logView == null) {
				  logView = new LogView();
			  }
			  contentArea.add(logView);
			  currentCenterAreaWidget = logView;
			  logView.start();
		  }
	  }
	  
	  if (NETWORK_STATUS_VIEW.equals(pane)) {
		  if (currentCenterAreaWidget != networkView) {
			  contentArea.remove(currentCenterAreaWidget);
			  contentArea.add(networkView);
			  currentCenterAreaWidget = networkView;
		  }
	  }
	  
	  if (SCRIPT_VIEW.equals(pane)) {
		  if (currentCenterAreaWidget != scriptView) {
			  contentArea.remove(currentCenterAreaWidget);
			  contentArea.add(scriptView);
			  currentCenterAreaWidget = scriptView;
		  }
	  }
  }
  
  private Widget getCenterAreaWidget () {
	  // Identify current widget
	  int nwidget = contentArea.getWidgetCount();
	  
		for (int i = 0; i < nwidget; i++) {
			Widget w = contentArea.getWidget(i);
			if (w instanceof ThumbnailView ) {
				return w;
			}
		}
		return null;
  }
  
  public Long getNetworkId () {
	  return networkId;
  }
  public void setNetworkId (Long networkId) {
	  this.networkId = networkId;
  }
}
