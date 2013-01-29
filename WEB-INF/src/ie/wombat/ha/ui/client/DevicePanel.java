package ie.wombat.ha.ui.client;

import ie.wombat.zigbee.zdo.BindTableEntry;
import ie.wombat.zigbee.zdo.BindTableResponse;


import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.SpanElement;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.logical.shared.SelectionHandler;

import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;

import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.TabLayoutPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.user.client.ui.IntegerBox;


/**
 * Display device information
 */
public class DevicePanel extends Composite {

	interface Binder extends UiBinder<Widget, DevicePanel> {
	}

	private static final Binder uiBinder = GWT.create(Binder.class);

	private DeviceInfo deviceInfo;
	private int[] endPoints;

	@UiField TabLayoutPanel tabPanel;
	
	@UiField SpanElement heading;
	@UiField SpanElement zclVersionEl;
	@UiField SpanElement appVersionEl;
	@UiField SpanElement stackVersionEl;
	@UiField SpanElement hwVersionEl;
	@UiField SpanElement manufacturerEl;
	
	@UiField SpanElement modelIdEl;
	@UiField SpanElement dateEl;
	@UiField SpanElement powerEl;
	
	@UiField SpanElement epEl;
	
	@UiField SpanElement simpleDescriptorEl;
	
	@UiField SpanElement nodeDescriptorEl;
	
	@UiField SpanElement bindTableEl;
	@UiField Button idBtn;
	
	
	@UiField IntegerBox minIntervalField;
	@UiField IntegerBox endPointField;
	@UiField IntegerBox clusterIdField;
	@UiField IntegerBox attrIdField;
	
	@UiField Button setReportCfgBtn;
	@UiField Button getReportCfgBtn;

	@UiField SpanElement reportingCfg;
	
	@UiField SpanElement addr16Span;
	
	@UiField Button getAddr16Btn;
	@UiField Button getDeviceInfoBtn;
	@UiField Button getNodeDescBtn;
	@UiField Button getSimpleDescBtn;
	//@UiField Button getActiveEPBtn;
	
	// Relating to bindings tab
	@UiField Panel bindingTablePanel;
	@UiField Button setBindBtn;
	@UiField IntegerBox bindSrcEpField;
	@UiField IntegerBox bindDstEpField;
	@UiField IntegerBox bindClusterIdField;
	@UiField IntegerBox bindDstDeviceIdField;

	@UiField Panel epBtnPanel;
	
	@UiField TabLayoutPanel endPointsTabPanel;
	
	private final ZDOServiceAsync zdoService = GWT.create(ZDOService.class);
	private final ZigBeeServiceAsync zigbeeService = GWT.create(ZigBeeService.class);
	private final AddressServiceAsync addressService =  GWT.create(AddressService.class);
	
	public DevicePanel(DeviceInfo devInfo) {
		// super();

		this.deviceInfo = devInfo;
		
		initWidget(uiBinder.createAndBindUi(this));

		heading.setInnerText("Device#" + devInfo.id + " " + devInfo.getName() + " (" + devInfo.addr64 + "/" + devInfo.addr16 + ")");
		
		getAddr16Btn.addClickHandler(new ClickHandler() {		
			public void onClick(ClickEvent event) {
				getDeviceAddr16();
			}
		});
		
		getDeviceInfoBtn.addClickHandler(new ClickHandler() {		
			public void onClick(ClickEvent event) {
				// Properties tab
				int[] attrIds = {0x0000, 0x0001, 0x0002, 0x0003, 0x0004, 0x0005,0x0006,0x0007};
				SpanElement[] els = {zclVersionEl, appVersionEl, stackVersionEl, hwVersionEl, 
					manufacturerEl, modelIdEl, dateEl, powerEl};
				getBasicAttributes(deviceInfo.id, 1, 0x104, attrIds, els);
				
			}
		});
		

		/*
		getActiveEPBtn.addClickHandler(new ClickHandler() {		
			public void onClick(ClickEvent event) {
				updateActiveEP();
			}
		});
		*/
		

		getNodeDescBtn.addClickHandler(new ClickHandler() {		
			public void onClick(ClickEvent event) {
				updateNodeDescriptor();
			}
		});
		
	
		
		setBindBtn.addClickHandler(new ClickHandler() {

			public void onClick(ClickEvent event) {
				// TODO Auto-generated method stub
				Long dstDeviceId = new Long(bindDstDeviceIdField.getValue());
				int clusterId = bindClusterIdField.getValue();
				int srcEp = bindSrcEpField.getValue();
				int dstEp = bindDstEpField.getValue();
				zdoService.bindRequest(deviceInfo.networkId,deviceInfo.id, 
						dstDeviceId, 
						clusterId, 
						srcEp, 
						dstEp, 
						new AsyncCallback<Integer>() {

							public void onFailure(Throwable caught) {
								// TODO Auto-generated method stub
								
							}

							public void onSuccess(Integer result) {
								// TODO Auto-generated method stub
								
							}
						}
				);
			}
			
		});
		
		
		getReportCfgBtn.addClickHandler(new ClickHandler() {
			public void onClick(ClickEvent event) {
				
				Window.alert("test");
				
				int endPoint, clusterId, attrId;
				
				try {
					endPoint = Integer.decode(endPointField.getText());
					clusterId = Integer.decode(clusterIdField.getText());
					attrId = Integer.decode(attrIdField.getText());
					/*
				Window.alert("deviceId=" + deviceInfo.id 
						+ " ep=" + endPoint 
						+ " cluster=0" + Integer.toHexString(clusterId) 
						+ " attrId=" + attrId);
					*/
				} catch (Exception e ) {
					Window.alert(e.toString());
					return;
				}
			
				zigbeeService.getReporting (deviceInfo.id, 
						endPoint, 
						clusterId,
						attrId,
						new AsyncCallback<String>() {

							public void onSuccess(String result) {
								reportingCfg.setInnerText(result);
							}

							public void onFailure(Throwable caught) {
								Window.alert(caught.toString());
							}

				});
			}
		});
		
		setReportCfgBtn.addClickHandler(new ClickHandler() {
			public void onClick(ClickEvent event) {
				int minInterval = Integer.parseInt(minIntervalField.getText());
				int endPoint = Integer.parseInt(endPointField.getText());
				int clusterId = Integer.parseInt(clusterIdField.getText());
				int attrId = Integer.parseInt(attrIdField.getText());
				Window.alert("deviceId=" + deviceInfo.id 
						+ " ep=" + endPoint 
						+ " cluster=0" + Integer.toHexString(clusterId) 
						+ " attrId=" + attrId);
				zigbeeService.setReporting(deviceInfo.id, 
						endPoint, 
						0x0402, // ClusterID 
						attrId, 
						minInterval, 3600, 0, new AsyncCallback<Integer>() {
							public void onFailure(Throwable caught) {								
							}
							public void onSuccess(Integer result) {								
							}
						}
						);
			}
		});
		
		// Identify (blink LED) button
		idBtn.addClickHandler(new ClickHandler() {
			
			public void onClick(ClickEvent event) {
				//Window.alert("ID");
				zigbeeService.identify(deviceInfo.id,
						new AsyncCallback<Integer>() {

					public void onSuccess(Integer result) {
						//
						//Window.alert("ID success");
					}

					public void onFailure(Throwable caught) {
						//
					}

				}); // end onClick()
						
			}
		}); // end click handler
		
		// When tab panes are selected 
		tabPanel.addSelectionHandler(new SelectionHandler<Integer>() {
			
			public void onSelection(SelectionEvent<Integer> event) {
				
				
				switch (event.getSelectedItem()) {
				case 0: { 
					
					break;
				}
				case 1: {
					updateBindTable();
					break;
				}
				
				// Reporting tab
				case 2: {
					
					break;
				}
				
				case 3: {
					// Endpoints
					updateActiveEP();
					//getDeviceAddr16();
				}
				
				}
			}
		});
		
	}
	
	private void getDeviceAddr16() {
		addressService.getDeviceAddr16(deviceInfo.networkId, deviceInfo.addr64, new AsyncCallback<String>() {
			public void onSuccess(String result) {
				addr16Span.setInnerText(result);
			}
			public void onFailure(Throwable caught) {
				Window.alert(caught.toString());
			}
		});
	}
	
	private void updateBindTable () {
		// Bindings tab
		bindTableEl.setInnerText("...fetching...");
		bindingTablePanel.clear();
		
		final Grid bindingTable = new Grid (5,7);
		bindingTable.setText(0, 0, "Addr Mode");
		bindingTable.setText(0, 1, "Src Addr64");
		bindingTable.setText(0, 2, "Src EP");
		bindingTable.setText(0, 3, "Dst Addr64");
		bindingTable.setText(0, 4, "Dst EP");
		bindingTable.setText(0, 5, "Cluster");
		
		bindingTablePanel.add(bindingTable);
		
		zdoService.getBindTable2(deviceInfo.networkId,deviceInfo.id, 
				new AsyncCallback<BindTableResponse>() {

					public void onSuccess(BindTableResponse result) {
						//bindTableEl.setInnerText(result);
						int i = 1;
						for (final BindTableEntry entry : result.getEntries()) {
							bindingTable.setText(i, 0, ""+entry.addrMode);
							bindingTable.setText(i, 1, entry.srcAddr64.toString());
							bindingTable.setText(i, 2, ""+entry.srcEp);
							bindingTable.setText(i, 3, entry.dstAddr64.toString());
							bindingTable.setText(i, 4, ""+entry.dstEp);
							bindingTable.setText(i, 5, "0x" + Integer.toHexString(entry.clusterId));
							final Button deleteBindingBtn = new Button("Delete");
							
							bindingTable.setWidget(i, 6, deleteBindingBtn);
							deleteBindingBtn.addClickHandler(new ClickHandler() {
								public void onClick(ClickEvent event) {
									
									zdoService.unbindRequest(
											deviceInfo.networkId, 
											entry.srcAddr64.toString(), 
											entry.dstAddr64.toString(), 
											entry.clusterId, 
											entry.srcEp, 
											entry.dstEp, 
											new AsyncCallback<Integer>() {

												public void onFailure(
														Throwable caught) {
													// TODO Auto-generated method stub
													
												}

												public void onSuccess(
														Integer result) {
													// TODO Auto-generated method stub
													
												}
											}
										);
									
								}
							});
						}
					}

					public void onFailure(Throwable caught) {
						Window.alert(caught.toString());
					}

				}); // end getBindTable()
		
	}
	private void updateSimpleDesc () {
		for (int i = 0; i < endPoints.length; i++) {
			//Window.alert("fetching desc for ep " + endPoints[i]);
			simpleDescriptorEl.setInnerText("fetching...");
			zdoService.getSimpleDescriptor(deviceInfo.networkId,deviceInfo.id, endPoints[i], 
				new AsyncCallback<String>() {

					public void onSuccess(String result) {
						String text = simpleDescriptorEl.getInnerText();
						text += "; " + result;
						simpleDescriptorEl.setInnerText(text);
					}

					public void onFailure(Throwable caught) {
						Window.alert(caught.toString());
					}

				});
		}
	}
	
	private void updateActiveEP () {
		zdoService.getActiveEndpoints(deviceInfo.networkId, deviceInfo.id, 
				new AsyncCallback<int[]>() {

					public void onSuccess(int[] result) {
						
						if (result == null) {
							epEl.setInnerText("ERROR");
							return;
						}
						
						// Clear previous results
						endPointsTabPanel.clear();
						
						StringBuffer buf = new StringBuffer();
						for (int i = 0; i < result.length; i++) {
							buf.append(result[i] + " ");
						}
						epEl.setInnerText("" + buf.toString());
						endPoints = result;
						
						for (int i = 0; i < result.length; i++) {
							final int ep = result[i];
							
							EndPointPanel epPanel = new EndPointPanel(deviceInfo,ep);
							endPointsTabPanel.add(epPanel,"EP"+ep);
						
						}
						
					}

					public void onFailure(Throwable caught) {
						Window.alert(caught.toString());
					}

				});
		
	}
	
	private void updateNodeDescriptor () {
		zdoService.getNodeDescriptor(deviceInfo.networkId,deviceInfo.id,
				new AsyncCallback<String>() {

					public void onSuccess(String result) {
						nodeDescriptorEl.setInnerText(result);
					}

					public void onFailure(Throwable caught) {
						Window.alert(caught.toString());
					}

				});
		
	
		
		
		
	}

	/**
	 * Create a remote service proxy to talk to the server-side GetDevices
	 * service.
	 */

	
	private void getBasicAttributes(final Long deviceId, int endPoint, int profileId, final int[] attrIds, final SpanElement[] els) {
		zigbeeService.getAttributeValues(deviceId, 1, 0x104 /* HA */, 0x0, attrIds,
				new AsyncCallback<String[]>() {

					public void onSuccess(String[] result) {
						for (int i = 0; i < result.length; i++) {
							els[i].setInnerHTML(result[i]);
						}
					}

					public void onFailure(Throwable caught) {
						Window.alert(caught.toString());
					}

				});
	}

}
