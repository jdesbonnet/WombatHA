
package ie.wombat.ha.ui.client;


import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.ResizeComposite;
import com.google.gwt.user.client.ui.StackLayoutPanel;

/**
 * A composite that contains the shortcut stack panel on the left side. 
 * {@link com.google.gwt.user.client.ui.StackPanel},
 * {@link com.google.gwt.user.client.ui.Tree}, and other custom widgets.
 */
public class LeftMenu extends ResizeComposite {

  interface Binder extends UiBinder<StackLayoutPanel, LeftMenu> { }
  private static final Binder binder = GWT.create(Binder.class);

  //@UiField Projects projects;
  //@UiField QueuedJobs queuedJobs;
  //@UiField Settings settings;
  //@UiField Button createProjectBtn;

  /**
   * Constructs a new shortcuts widget using the specified images.
   * 
   * @param images a bundle that provides the images for this widget
   */
  public LeftMenu() {
    initWidget(binder.createAndBindUi(this));
    final DialogBox dialogBox = new NewProjectDialogBox();
    
    /*
    createProjectBtn.addClickHandler(new ClickHandler() {
		
		@Override
		public void onClick(ClickEvent event) {
			 dialogBox.center();
	         dialogBox.show();
		}
	});
	*/
    

  }
  
 
  
}
