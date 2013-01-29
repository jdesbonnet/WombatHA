package ie.wombat.ha.ui.client;

import ie.wombat.zigbee.zdo.SimpleDescriptorResponse;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.SpanElement;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

public class EndPointPanel extends Composite {

	interface Binder extends UiBinder<Widget, EndPointPanel> {
	}

	private static final Binder uiBinder = GWT.create(Binder.class);

	@UiField SpanElement heading;
	@UiField Panel clustersPanel;
	//@UiField SpanElement bindings;
	//@UiField SpanElement simpleDesc;

	private final ZDOServiceAsync zdoService = GWT.create(ZDOService.class);
	private final ZigBeeServiceAsync zigbeeService = GWT
			.create(ZigBeeService.class);

	private int endPoint;
	private DeviceInfo deviceInfo;
	private SimpleDescriptorResponse simpleDescriptor;
	

	public EndPointPanel(final DeviceInfo deviceInfo, final int endPoint) {
		initWidget(uiBinder.createAndBindUi(this));

		this.deviceInfo = deviceInfo;
		this.endPoint = endPoint;

		getSimpleDescriptor();
	}

	/**
	 * Get endpoint simple descriptor from server.
	 */
	private void getSimpleDescriptor() {

		heading.setInnerText("EndPoint " + endPoint + " ...fetching...");

		// Simple Descriptor lists in/out clusters for an end point.
		zdoService.getSimpleDescriptor2(deviceInfo.networkId, deviceInfo.id,
				endPoint, new AsyncCallback<SimpleDescriptorResponse>() {

					public void onSuccess(SimpleDescriptorResponse result) {
											
						simpleDescriptor = result;
						
						int[] inClusters = result.getInputClusters();
						int[] outClusters = result.getOutputClusters();
						//int nCluster = inClusters.length + outClusters.length;

						//heading.setInnerText("EP" + endPoint + ", profileId=0x"
						//		+ Integer.toHexString(result.getProfileId()));

						//simpleDesc.setInnerText(result.toString());

						int i;
						/*
						for (i = 0; i < inClusters.length; i++) {
							clustersGrid.setHTML(row, 0, "0x"
									+ Integer.toHexString(inClusters[i]));
							clustersGrid.setHTML(row, 1, "IN");
							final FlowPanel attrsPanel = new FlowPanel();
							clustersGrid.setWidget(row, 2, attrsPanel);
							popupateAttributesPanel(attrsPanel, inClusters[i]);
							
							row++;
						}
						*/
						for (i = 0; i < inClusters.length; i++) {
							final Grid grid = new Grid(14, 3);
							clustersPanel.add(grid);
							grid.setHTML(0, 0, "IN 0x"+ Integer.toHexString(inClusters[i]));
							popupateAttributesPanel(grid, inClusters[i]);
						}
						for (i = 0; i < outClusters.length; i++) {
							final Grid grid = new Grid(14, 3);
							clustersPanel.add(grid);
							grid.setHTML(0, 0, "OUT 0x"+ Integer.toHexString(outClusters[i]));
							popupateAttributesPanel(grid, outClusters[i]);
						}
					}

					public void onFailure(Throwable caught) {
						// simpleDesc.setInnerText(caught.toString());
					}

				});
	}
	
	private void popupateAttributesPanel(final Grid grid,
			final int clusterId) {
		grid.setText (0,1,"..."); 
		zigbeeService.getAttributeIds(deviceInfo.networkId, deviceInfo.id,
				endPoint, clusterId, new AsyncCallback<int[]>() {
					public void onFailure(Throwable caught) {
						Window.alert(caught.toString());
					}

					public void onSuccess(int[] result) {
						//attrsPanel.clear();
						grid.setText (0,1,"done."); 
						int row = 0;
						for (final int aid : result) {
							row++;
							grid.setText(row,0,"0x"+Integer.toHexString(aid));
							final int rowf = row;
							final Button attrBtn = new Button("Q");
							grid.setWidget(row,2,attrBtn);
							attrBtn.addClickHandler(new ClickHandler() {

								public void onClick(ClickEvent event) {
									int[] attrIds = new int[1];
									attrIds[0] = aid;
									grid.setText(rowf,1,"...fetching...");
									zigbeeService.getAttributeValues(
											deviceInfo.id, endPoint, 0x104, clusterId, attrIds,
											new AsyncCallback<String[]>() {

												public void onFailure(
														Throwable caught) {
													// TODO Auto-generated
													// method stub

												}

												public void onSuccess(
														String[] result) {
													grid.setText(rowf,1,
														(result.length == 1 ? result[0]
																	: "??"));

												}
											}); // end getAttributeValues()

								}
							}); // end attrBtn click handler
						}
					}
				}); // end getAttributeIds()
	}
}
