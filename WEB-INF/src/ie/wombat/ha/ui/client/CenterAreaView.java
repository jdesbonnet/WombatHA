package ie.wombat.ha.ui.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Element;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.DockLayoutPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.ResizeComposite;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

/**
 * A superclass of all center area view classes
 */
public abstract class CenterAreaView extends ResizeComposite {

	interface Binder extends UiBinder<Widget, CenterAreaView> {
	}

	private static final Binder binder = GWT.create(Binder.class);

	
	@UiField Element title;
	@UiField Element line0;
	@UiField Element line1;
	
	@UiField DockLayoutPanel dockLayout;
	
	//@UiField Panel vp;
	VerticalPanel vp;
	
	//@UiField Panel eastPanel;
	VerticalPanel eastPanel;
	
	
	
	public CenterAreaView(boolean includeEastPanel) {
		initWidget(binder.createAndBindUi(this));
		
		// Optional East panel
		if (includeEastPanel) {
			eastPanel = new VerticalPanel();
			dockLayout.addEast(eastPanel, 28L);
			eastPanel.add(new Label("blah"));
		}
		
		
		// Center panel
		ScrollPanel sp = new ScrollPanel();
		vp = new VerticalPanel();
		sp.add(vp);
		dockLayout.add(sp);
		
		
	}
	
	public void setViewTitle(String viewTitle) {
		title.setInnerText(viewTitle);
	}

}
