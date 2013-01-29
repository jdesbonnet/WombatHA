package ie.wombat.ha.ui.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.SpanElement;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiFactory;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.TextBox;


public class NewProjectDialogBox extends DialogBox {

	// interface Binder extends UiBinder<VerticalPanel, NewProjectDialogBox> {}
	interface Binder extends UiBinder<DialogBox, NewProjectDialogBox> {
	}

	private static final Binder binder = GWT.create(Binder.class);
	//private final OMRRPCServiceAsync rpcService = GWT.create(OMRRPCService.class);
	
	@UiField
	TextBox projectName;
	
	@UiField
	Button createBtn;
	@UiField
	Button cancelBtn;
	@UiField
	SpanElement message;

	public NewProjectDialogBox() {
		// super();

		binder.createAndBindUi(this);

		final NewProjectDialogBox me = this;

		// doc on exception handling
		// http://code.google.com/webtoolkit/doc/latest/tutorial/RPC.html#exceptions
		
		createBtn.addClickHandler(new ClickHandler() {

			public void onClick(ClickEvent event) {
				
/*				
				rpcService.createProject(projectName.getText(), new AsyncCallback<Long>() {
					
					@Override
					public void onSuccess(Long result) {
						// TODO Auto-generated method stub
						me.message.setInnerHTML("Project created Id=" + result);
					}
					
					@Override
					public void onFailure(Throwable caught) {
						// TODO Auto-generated method stub
						me.message.setInnerHTML("<img src=\"" 
								+ IconResources.INSTANCE.error().getURL()
								+ "\" />"
								+  " Error creating project: " + caught.getMessage());
					}
				});
*/			
				
			}
		});
		
		cancelBtn.addClickHandler(new ClickHandler() {
			
			public void onClick(ClickEvent event) {
				me.removeFromParent();
			}
		});
		

	}

	@UiFactory
	DialogBox getDialogBox() {
		return this;
	}

}
