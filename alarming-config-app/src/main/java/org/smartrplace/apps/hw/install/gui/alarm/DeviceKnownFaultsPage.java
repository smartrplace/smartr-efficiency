package org.smartrplace.apps.hw.install.gui.alarm;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.ogema.accessadmin.api.SubcustomerUtil;
import org.ogema.core.application.ApplicationManager;
import org.ogema.core.model.simple.FloatResource;
import org.ogema.core.model.simple.IntegerResource;
import org.ogema.core.model.simple.StringResource;
import org.ogema.core.model.simple.TimeResource;
import org.ogema.devicefinder.api.DatapointGroup;
import org.ogema.devicefinder.api.DatapointService;
import org.ogema.devicefinder.api.DeviceHandlerProvider;
import org.ogema.devicefinder.api.InstalledAppsSelector;
import org.ogema.devicefinder.util.AlarmingConfigUtil;
import org.ogema.devicefinder.util.DeviceTableBase;
import org.ogema.devicefinder.util.DpGroupUtil;
import org.ogema.model.devices.buildingtechnology.Thermostat;
import org.ogema.model.extended.alarming.AlarmGroupData;
import org.ogema.model.extended.alarming.AlarmGroupDataMajor;
import org.ogema.model.extended.alarming.DevelopmentTask;
import org.ogema.model.gateway.LocalGatewayInformation;
import org.ogema.model.prototypes.PhysicalElement;
import org.ogema.timeseries.eval.simple.api.TimeProcUtil;
import org.smartrplace.apps.alarmconfig.util.AlarmMessageUtil;
import org.smartrplace.apps.alarmconfig.util.AlarmResourceUtil;
import org.smartrplace.apps.alarmingconfig.AlarmingConfigAppController;
import org.smartrplace.apps.alarmingconfig.release.ReleasePopup;
import org.smartrplace.apps.alarmingconfig.sync.SuperiorIssuesSyncUtils;
import org.smartrplace.apps.hw.install.config.InstallAppDevice;
import org.smartrplace.apps.hw.install.gui.ThermostatPage;
import org.smartrplace.external.accessadmin.config.SubCustomerSuperiorData;
import org.smartrplace.tissue.util.resource.GatewaySyncUtil;
import org.smartrplace.util.directobjectgui.ObjectGUIHelperBase.ValueResourceDropdownFlex;
import org.smartrplace.util.directobjectgui.ObjectResourceGUIHelper;
import org.smartrplace.util.format.WidgetHelper;
import org.smartrplace.util.virtualdevice.ChartsUtil;
import org.smartrplace.util.virtualdevice.ChartsUtil.GetPlotButtonResult;
import org.smartrplace.widget.extensions.GUIUtilHelper;

import de.iwes.util.resource.ResourceHelper;
import de.iwes.util.resource.ValueResourceHelper;
import de.iwes.widgets.api.extended.html.bricks.PageSnippet;
import de.iwes.widgets.api.widgets.OgemaWidget;
import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.dynamics.TriggeredAction;
import de.iwes.widgets.api.widgets.dynamics.TriggeringAction;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.alert.Alert;
import de.iwes.widgets.html.buttonconfirm.ButtonConfirm;
import de.iwes.widgets.html.complextable.RowTemplate.Row;
import de.iwes.widgets.html.form.button.Button;
import de.iwes.widgets.html.form.button.ButtonData;
import de.iwes.widgets.html.form.button.RedirectButton;
import de.iwes.widgets.html.form.checkbox.SimpleCheckbox;
import de.iwes.widgets.html.form.dropdown.Dropdown;
import de.iwes.widgets.html.form.dropdown.DropdownOption;
import de.iwes.widgets.html.form.dropdown.TemplateDropdown;
import de.iwes.widgets.html.form.label.Label;
import de.iwes.widgets.html.form.label.LabelData;
import de.iwes.widgets.resource.widget.textfield.ValueResourceTextField;

@SuppressWarnings("serial")
public class DeviceKnownFaultsPage extends DeviceAlarmingPage {
	//@Deprecated //Currently not used, but still available in the details section
	
	public static enum KnownFaultsPageType {
		OPERATION_STANDARD,
		SUPERVISION_STANDARD
	}
	final KnownFaultsPageType pageType;
	//private final Button createIssueSubmit;
	private final CreateIssuePopup createIssuePopup;
	
	private final IssueDetailsPopup lastMessagePopup;
	private final ReleasePopup releasePopup;
	
	public static final Map<String, String> dignosisVals = new HashMap<>();
	static {
		dignosisVals.put("0", "not set");
		dignosisVals.put("1", "requires more analysis");
		dignosisVals.put("10", "no contact: Device not on site, out of radio signal or battery empty");
		dignosisVals.put("11", "no contact: Device not on site");
		dignosisVals.put("12", "no contact: Device out of radio signal");
		dignosisVals.put("13", "no contact: Battery empty");
		dignosisVals.put("20", "insufficient signal strength: requires additional repeater, controller or HAP");
		dignosisVals.put("21", "insufficient signal strength: wrong controller association");
		dignosisVals.put("22", "insufficient signal strength: other reason");
		dignosisVals.put("30", "Thermostat is not properly installed (valve / adaption error)");
		dignosisVals.put("40", "Thermostat requires wall thermostat");
		dignosisVals.put("50", "Battery low");
	}
	
	protected boolean showAllDevices = false;
	
	protected int getTopTableLines() {
		return 3;
	}
	
	@Override
	protected String getHeader() {
		if(pageType == KnownFaultsPageType.SUPERVISION_STANDARD)
			return "8. Device Issue Status Supervision";
		return "3. Device Issue Status";
	}
	
	@Override
	protected boolean isAllOptionAllowedSuper(OgemaHttpRequest req) {
		return true;
	}
	
	public DeviceKnownFaultsPage(WidgetPage<?> page, AlarmingConfigAppController controller,
			KnownFaultsPageType pageType) {
		super(page, controller, false);
		this.pageType = pageType;
		
		this.lastMessagePopup = new IssueDetailsPopup(page);
		
		//
		finishConstructor();
		
		Button switchAllDeviceBut = new Button(page, "switchAllDeviceBut") {
			@Override
			public void onGET(OgemaHttpRequest req) {
				if(showAllDevices)
					setText("ALL DEVICES", req);
				else
					setText("STANDARD", req);
			}
			@Override
			public void onPOSTComplete(String data, OgemaHttpRequest req) {
				showAllDevices = !showAllDevices;
			}
		};
		switchAllDeviceBut.registerDependentWidget(switchAllDeviceBut);
		
		final Button createKnownIssue = new Button(page, "createKnownIssue");
		createKnownIssue.setDefaultText("Create issue");
		createKnownIssue.setDefaultToolTip("Manually create a new device issue that is not captured by the automated alarming system");
			
		final PageSnippet cellSnippet0 = new PageSnippet(page, "cellSnippet0", true);
		cellSnippet0.append(switchAllDeviceBut, null);
		cellSnippet0.append(createKnownIssue, null);
		cellSnippet0.addCssItem("#bodyDiv", Map.of("display", "flex", "column-gap", "1em"), null);
		topTable.setContent(1, 5, cellSnippet0);
		
		this.releasePopup = new ReleasePopup(page, "releasepop", appMan, alert, controller);
		this.createIssuePopup = new CreateIssuePopup(page, appMan, alert, releasePopup, controller);
		createIssuePopup.trigger(createKnownIssue);
		
		ButtonConfirm releaseAllUnassigned = new ButtonConfirm(page, "releaseAllUnassigned") {
			@Override
			public void onPOSTComplete(String data, OgemaHttpRequest req) {
				if(devHandAcc != null)  {
					Collection<DeviceHandlerProvider<?>> allProvs = devHandAcc.getTableProviders().values();
					for(DeviceHandlerProvider<?> pe: allProvs) {
						List<InstallAppDevice> allforPe = getDevicesSelected(pe, req);
						releaseAllUnassigned(allforPe, appMan.getFrameworkTime());
						/*for(InstallAppDevice iad: allforPe) {
							AlarmGroupData res = iad.knownFault();
							if((!res.assigned().isActive()) || (res.assigned().getValue() <= 0)) {
								res.delete();
							}
						}*/
					}
				}
			}
		};
		releaseAllUnassigned.setDefaultText("Release all Unassigned");
		releaseAllUnassigned.setDefaultConfirmMsg("Really release all known issues that are not assigned?");
		
		topTable.setContent(1, 6, releaseAllUnassigned);
		
		ButtonConfirm setAllUnassignedDependent = new ButtonConfirm(page, "setAllUnassignedDependent") {
			@Override
			public void onPOSTComplete(String data, OgemaHttpRequest req) {
				if(devHandAcc != null)  {
					Collection<DeviceHandlerProvider<?>> allProvs = devHandAcc.getTableProviders().values();
					for(DeviceHandlerProvider<?> pe: allProvs) {
						List<InstallAppDevice> allforPe = getDevicesSelected(pe, req);
						setAllUnassignedDependent(allforPe);
					}
				}
			}
		};
		setAllUnassignedDependent.setDefaultText("Set all Unassigned Dependent");
		setAllUnassignedDependent.setDefaultConfirmMsg("Really set all unassigned DEPENDENT?");
		topTable.setContent(1, 1,setAllUnassignedDependent);

		ButtonConfirm releaseAllDependent = new ButtonConfirm(page, "releaseAllDependent") {
			@Override
			public void onPOSTComplete(String data, OgemaHttpRequest req) {
				if(devHandAcc != null)  {
					Collection<DeviceHandlerProvider<?>> allProvs = devHandAcc.getTableProviders().values();
					for(DeviceHandlerProvider<?> pe: allProvs) {
						List<InstallAppDevice> allforPe = getDevicesSelected(pe, req);
						releaseAllDependent(allforPe, appMan.getFrameworkTime());
					}
				}
			}
		};
		releaseAllDependent.setDefaultText("Release all Dependent");
		releaseAllDependent.setDefaultConfirmMsg("Really release all known issues that are set DEPENDENT?");
		topTable.setContent(1, 2, releaseAllDependent);

		
		RedirectButton homeScreen = new RedirectButton(page, "homeScreen", "Other Apps", "/org/smartrplace/apps/apps-overview/index.html") {
			@Override
			public void onGET(OgemaHttpRequest req) {
				String user = GUIUtilHelper.getUserLoggedIn(req);
				if(isMasterUser(user))
					setUrl("/ogema/index.html", req);
			}
			
		};
		topTable.setContent(2, 0, homeScreen);
		
		RedirectButton thermostatPage = new RedirectButton(page, "thermostatPage", "Devices", "/org/smartrplace/hardwareinstall/index.html") {
			@Override
			public void onGET(OgemaHttpRequest req) {
				String user = GUIUtilHelper.getUserLoggedIn(req);
				if(isMasterUser(user)) {
					setUrl("/org/smartrplace/hardwareinstall/expert/thermostatDetails2.hmtl.html", req);
					setText("Thermostats", req);
				}
			}
			
		};
		topTable.setContent(2, 1, thermostatPage);
		
		RedirectButton networkPage = new RedirectButton(page, "networkPage", "Network", "/org/smartrplace/hardwareinstall/superadmin/index.html") {
			@Override
			public void onGET(OgemaHttpRequest req) {
				String user = GUIUtilHelper.getUserLoggedIn(req);
				if(isMasterUser(user)) {
					setWidgetVisibility(true, req);;
				} else
					setWidgetVisibility(false, req);;
			}
			
		};
		topTable.setContent(2, 2, networkPage);

		RedirectButton chartPage = new RedirectButton(page, "chartPage", "Charts", "/org/sp/app/srcmon/roomcontrolcharts.html") {
			@Override
			public void onGET(OgemaHttpRequest req) {
				String user = GUIUtilHelper.getUserLoggedIn(req);
				if(isMasterUser(user)) {
					setUrl("/org/sp/app/srcmonexpert/roomcontrolcharts.html", req);
				}
			}
			
		};
		topTable.setContent(2, 3, chartPage);

		final LocalGatewayInformation gwInfo = ResourceHelper.getLocalGwInfo(controller.appMan);
		final SubCustomerSuperiorData subc;
		if(!Boolean.getBoolean("org.smartplace.app.srcmon.server.issuperior")) {
			subc = SubcustomerUtil.getEntireBuildingSubcustomerDatabase(appMan);
		} else
			subc = null;
		
		RedirectButton wikiPage = new RedirectButton(page, "wikiPage", "Wiki Page", "https://smartrplace.onlyoffice.eu/Products/Files/#sbox-75287-%7Cpublic%7COperation%7CKunden") {
			@Override
			public void onGET(OgemaHttpRequest req) {
				String user = GUIUtilHelper.getUserLoggedIn(req);
				if(isMasterUser(user) && (gwInfo != null)) {
					final StringResource linkOverviewUrlRes = subc != null? subc.gatewayLinkOverviewUrl():gwInfo.gatewayLinkOverviewUrl();
					if(linkOverviewUrlRes.exists()) {
						String curLink = linkOverviewUrlRes.getValue();
						setUrl(curLink, req);
					}
					setWidgetVisibility(true, req);
				} else
					setWidgetVisibility(false, req);
			}
			
		};
		topTable.setContent(2, 4, wikiPage);

		RedirectButton floorPlanPage = new RedirectButton(page, "floorPlanPage", "Floor Plan", "https://smartrplace.onlyoffice.eu/Products/Files/#sbox-75287-%7Cpublic%7COperation%7CKunden") {
			@Override
			public void onGET(OgemaHttpRequest req) {
				String user = GUIUtilHelper.getUserLoggedIn(req);
				if(isMasterUser(user) && (gwInfo != null)) {
					final StringResource databaseUrlRes = subc != null? subc.gatewayOperationDatabaseUrl():gwInfo.gatewayOperationDatabaseUrl();
					if(databaseUrlRes.exists()) {
						String curLink = databaseUrlRes.getValue();
						setUrl(curLink, req);
					}
					setWidgetVisibility(true, req);
				} else
					setWidgetVisibility(false, req);
			}
			
		};
		topTable.setContent(2, 5, floorPlanPage);

		
		releasePopup.append(page);
	}


	@Override
	public void updateTables() {
		synchronized(tableProvidersDone) {
		if(devHandAcc != null) for(DeviceHandlerProvider<?> pe: devHandAcc.getTableProviders().values()) {
			//if(isObjectsInTableEmpty(pe))
			//	continue;
			String id = pe.id();
			if(tableProvidersDone.contains(id))
				continue;
			tableProvidersDone.add(id);
			DeviceTableBase tableLoc = getDeviceTable(page, alert, this, pe);
			tableLoc.triggerPageBuild();
			typeFilterDrop.registerDependentWidget(tableLoc.getMainTable());
			roomsDrop.registerDependentWidget(tableLoc.getMainTable());
			roomsDrop.getFirstDropdown().registerDependentWidget(tableLoc.getMainTable());
			subTables.add(new SubTableData(pe, tableLoc));
			
		}
		}
	}

	static Map<String, String> valuesToSetBlock = new LinkedHashMap<>();
	static {
		valuesToSetBlock.put("Blocking", "Blocking");
		valuesToSetBlock.put("No-Block", "No-Block");
		valuesToSetBlock.put("Retard", "Retard");
	}
	
	protected DeviceTableBase getDeviceTable(WidgetPage<?> page, Alert alert, InstalledAppsSelector appSelector,
			final DeviceHandlerProvider<?> pe) {
		final String pageTitle;
		DatapointGroup grp = DpGroupUtil.getDeviceTypeGroup(pe, appManPlus.dpService(), false);
		if(grp != null)
			pageTitle = "Devices of type "+ grp.label(null);
		else
			pageTitle = "Devices of type "+ pe.label(null);
		//final boolean showAlarmCtrl = pageType == KnownFaultsPageType.SUPERVISION_STANDARD;
		final AlarmingDeviceTableBase result = new AlarmingDeviceTableBase(page, appManPlus, alert, pageTitle, resData, commitButton, appSelector, pe, false, false) {
			@Override
			protected void addFrontWidgets(InstallAppDevice object,
					ObjectResourceGUIHelper<InstallAppDevice, InstallAppDevice> vh, String id, OgemaHttpRequest req,
					Row row, ApplicationManager appMan, PhysicalElement device) {
				if(req == null) {
					vh.registerHeaderEntry("Order Onsite");
				}
			}
			
			protected void addAdditionalWidgets(final InstallAppDevice object, ObjectResourceGUIHelper<InstallAppDevice,InstallAppDevice> vh, String id,
					OgemaHttpRequest req, Row row, final ApplicationManager appMan,
					PhysicalElement device, final InstallAppDevice template) {
				if(req == null) {
					//vh.registerHeaderEntry("Main value");
					if(Boolean.getBoolean("org.smartplace.app.srcmon.server.issuperior") && pe.label(null).equals("Gateway Device"))
						vh.registerHeaderEntry("Gateway");
					vh.getHeader().put("value", "Value/contact");
					vh.registerHeaderEntry("Started");
					vh.registerHeaderEntry("Message");
					vh.registerHeaderEntry("Details");
					vh.registerHeaderEntry("Comment_Analysis");
					vh.registerHeaderEntry("Analysis_Assigned");
					vh.registerHeaderEntry("Task Tracking");
					vh.registerHeaderEntry("Responsible");
					vh.getHeader().put("followup", "Follow-up");
					if(pageType == KnownFaultsPageType.SUPERVISION_STANDARD)
						vh.registerHeaderEntry("Edit TT");
					if(pe.id().toLowerCase().contains("thermostat"))
						vh.registerHeaderEntry("TH-Plot");
					vh.registerHeaderEntry("Plot");
					if(pageType == KnownFaultsPageType.SUPERVISION_STANDARD)
						vh.registerHeaderEntry("For");
					vh.registerHeaderEntry("Release");
					if(pageType == KnownFaultsPageType.SUPERVISION_STANDARD) {
						vh.registerHeaderEntry("Special Set(Dev)");
						vh.registerHeaderEntry("Alarming delay");
					}
					return;
				}
				final AlarmGroupData res = object.knownFault();
				//res.create();
				if(row == null) {
					//TODO: There is still a bug in the detail popup support so that for each table the popup is not adapted when
					//another detail button is clicked until the page is reloaded.
					//Another issue: only widgets generated via the vh helper can be added to the popup, no widgets that otherwise
					//would be added directly to the row. This should be possible by calling
					//popTableData.add(new WidgetEntryData(widgetId, newWidget));
					vh.dropdown("Diagnosis",  id, res.diagnosis(), row, dignosisVals);
					return;
				}
				
				if(Boolean.getBoolean("org.smartplace.app.srcmon.server.issuperior") && pe.label(null).equals("Gateway Device")) {
					String loc = object.installationLocation().getValue();
					if(loc == null || loc.isEmpty())
						loc = GatewaySyncUtil.getGatewayBaseIdIfRemoteDevice(object.device().getLocationResource());
					vh.stringLabel("Gateway", id, loc, row);
				}
				
				//vh.stringLabel("Finished", id, ""+res.isFinished().getValue(), row);
				// some special sensor values... priority for display: battery voltage; mainSensorValue, rssi, eq3state
				//final ValueData valueData = getValueData(object, appMan);
				final Label valueField = new Label(mainTable, "valueField" + id, req);
				AlarmMessageUtil.configureAlarmValueLabel(object, appMan, valueField, req, Locale.ENGLISH);
				//valueField.setText(valueData.message, req);
				row.addCell("value", valueField);
				//if (valueData.responsibleResource != null)
				//	valueField.setToolTip("Value resource: " + valueData.responsibleResource.getLocationResource(), req);
				
				vh.timeLabel("Started", id, res.ongoingAlarmStartTime(), row, 0);
				
				final Dropdown followupemail = new FollowUpDropdown(mainTable, "followup" + id, req, appMan, alert, object, controller);
				
				final Button showMsg = new Button(mainTable, "msg" + id, req) {
					
					private String getOrEmpty(final StringResource res) {
						return res.isActive() ? res.getValue() : "--";
					}
					
					@Override
					public void onPOSTComplete(String data, OgemaHttpRequest req) {
						lastMessagePopup.setValues(res, object, device, followupemail, req);
					}
					
				};
				showMsg.setDefaultText("Last message");
				showMsg.setDefaultToolTip("Show the last alarm message sent for this device, which contains some details about the source of the alarm.");
				showMsg.addDefaultStyle(ButtonData.BOOTSTRAP_GREEN);
				lastMessagePopup.setTriggers(showMsg);
				row.addCell("Message", showMsg);
				
				final RedirectButton detailsRedirect = new RedirectButton(mainTable, "details" + id, "Details", 
						"/org/smartrplace/alarmingexpert/ongoingbase.html?device=" + object.deviceId().getValue(), req);
				detailsRedirect.setToolTip("View alarm details in new tab", req);
				row.addCell("Details", detailsRedirect);
				
				final AtomicReference<Button> releaseBtnRef = new AtomicReference<>(null);
				final AtomicInteger releaseCnt = new AtomicInteger(0);
				final PageSnippet releaseBtnSnippet = new PageSnippet(mainTable, "releasesnippet" + id, req);
				if(res.exists()) {
					vh.stringEdit("Comment_Analysis",  id, res.comment(), row, alert, res.comment());
					ValueResourceDropdownFlex<IntegerResource> widgetPlus = new ValueResourceDropdownFlex<IntegerResource>(
							"Assigned"+id, vh, AlarmingConfigUtil.ASSIGNEMENT_ROLES) {
						public void onGET(OgemaHttpRequest req) {
							myDrop.selectItem(res.assigned(), req);
							final String role = AlarmingConfigUtil.ASSIGNEMENT_ROLES.get(String.valueOf(res.assigned().getValue()));
							if (role != null)
								myDrop.setToolTip(role, req);
						}
						@Override
						public void onPrePOST(String data, OgemaHttpRequest req) {
							IntegerResource source = res.assigned();
							if(!source.exists()) {
								source.create();
								source.activate(true);
							}
						}
						@Override
						public void onPOSTComplete(String data, OgemaHttpRequest req) {
							int val = res.assigned().getValue();
							if(val >= 7000 && val < 8000) {
								//Non-Blocking
								ValueResourceHelper.setCreate(res.minimumTimeBetweenAlarms(), 0);
							} else {
								//Blocking
								ValueResourceHelper.setCreate(res.minimumTimeBetweenAlarms(), -1);
							}
							if (SuperiorIssuesSyncUtils.syncIssueToSuperiorIfRelevant(res, appMan) != null) {
								// delete old release button and replace by new one...
								updateReleaseBtn(res, releaseBtnRef, releaseCnt, releaseBtnSnippet, id, req);
							}
						}
					};
					widgetPlus.myDrop.triggerAction(releaseBtnSnippet, TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST);
					row.addCell(WidgetHelper.getValidWidgetId("Analysis_Assigned"), widgetPlus.myDrop);
					
					if(!res.linkToTaskTracking().getValue().isEmpty()) {
						RedirectButton taskLink = new RedirectButton(mainTable, "taskLink"+id, "Task Tracking",
								res.linkToTaskTracking().getValue(), req);
						row.addCell(WidgetHelper.getValidWidgetId("Task Tracking"), taskLink);
					}
					final ValueResourceTextField<FloatResource> prioField 
						= new ValueResourceTextField<FloatResource>(mainTable, "prio" + id, res.getSubResource("processingOrder", FloatResource.class), req);
					prioField.setDefaultToolTip("Alarm priority, e.g. 10, 20, 30, ...");
					prioField.setDefaultWidth("4em");
					prioField.triggerAction(prioField, TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST, req);
					row.addCell(WidgetHelper.getValidWidgetId("Order Onsite"), prioField);
					if(pageType == KnownFaultsPageType.SUPERVISION_STANDARD)
						vh.stringEdit("Edit TT",  id, res.linkToTaskTracking(), row, alert);
					
					final Dropdown responsibleDropdown = new ResponsibleDropdown(mainTable, "responsible"+id, req, 
							appMan, res, () -> updateReleaseBtn(res, releaseBtnRef, releaseCnt, releaseBtnSnippet, id, req), object, controller);
					responsibleDropdown.triggerAction(releaseBtnSnippet, TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST);
					row.addCell("Responsible", responsibleDropdown);
					
				}
				row.addCell("followup", followupemail);
				
				
				
				if(pageType == KnownFaultsPageType.SUPERVISION_STANDARD) {
					SimpleCheckbox forRelease = new SimpleCheckbox(mainTable, "forRelease"+id, "", req) {
						@Override
						public void onGET(OgemaHttpRequest req) {
							int status = res.forRelease().getValue();
							setValue(status>0, req);
							if(status > 1)
								setStyle(LabelData.BOOTSTRAP_ORANGE, req);
							else
								setStyles(Collections.emptyList(), req);
						}
						@Override
						public void onPOSTComplete(String data, OgemaHttpRequest req) {
							boolean status = getValue(req);
							ValueResourceHelper.setCreate(res.forRelease(), status?1:0);
						}
					};
					row.addCell("For", forRelease);
				}
				
				Button releaseBut; 
				
				if (res.exists()) {
					releaseBut = createReleaseBtn(res, releaseBtnSnippet, id, req);
				}
				/*
				if (res instanceof AlarmGroupDataMajor) {
					releaseBut = new Button(mainTable, "releaseBut"+id, "Release", req);
					releasePopup.trigger(releaseBut);
				} else if(res.assigned().isActive() &&
						(res.assigned().getValue() > 0) && (res.forRelease().getValue() == 0)) {  // FIXME probably this case should not occur any more
					ButtonConfirm butConfirm = new ButtonConfirm(mainTable, "releaseBut"+id, req) {
						@Override
						public void onPOSTComplete(String data, OgemaHttpRequest req) {
							//TODO: In the future we may want to keep this information in a log of solved issues
							AlarmResourceUtil.release(res, appMan.getFrameworkTime());
							//res.delete();
							//res.ongoingAlarmStartTime().setValue(-1);
						}
					};
					butConfirm.setConfirmMsg("Really delete issue assigned to "+AlarmingConfigUtil.assignedText(res.assigned().getValue())+"?", req);
					releaseBut = butConfirm;
					releaseBut.setText("Release", req);
				} else if(res.exists()) {
					releaseBut = new Button(mainTable, "releaseBut"+id, "Release", req) {
						public void onGET(OgemaHttpRequest req) {
							int status = res.forRelease().getValue();
							if(status > 1)
								setStyle(ButtonData.BOOTSTRAP_ORANGE, req);
							else if(status > 0)
								setStyle(ButtonData.BOOTSTRAP_GREEN, req);
							else
								setStyles(Collections.emptyList(), req);							
						};
						@Override
						public void onPOSTComplete(String data, OgemaHttpRequest req) {
							//TODO: In the future we may want to keep this information in a log of solved issues
							AlarmResourceUtil.release(res, appMan.getFrameworkTime());
							//res.delete();
							//res.ongoingAlarmStartTime().setValue(-1);
						}
					};
				
				} */ else {
					releaseBut = new Button(releaseBtnSnippet, "releaseBut"+id, "Create", req) {
						@Override
						public void onPOSTComplete(String data, OgemaHttpRequest req) {
							res.create();
							long now = appMan.getFrameworkTime();
							ValueResourceHelper.setCreate(res.ongoingAlarmStartTime(), now);
							ValueResourceHelper.setCreate(res.minimumTimeBetweenAlarms(), -1);
							res.activate(true);
						}
					};
					releaseBut.addDefaultStyle(ButtonData.BOOTSTRAP_ORANGE);
				}
				releaseBtnRef.set(releaseBut);
				releaseBtnSnippet.append(releaseBut, req);
				row.addCell("Release", releaseBtnSnippet);
				
				if(object.device() instanceof Thermostat) {
					Thermostat dev = (Thermostat)object.device();
					final GetPlotButtonResult logResultSpecial = ThermostatPage.getThermostatPlotButton(dev, appManPlus, vh, id, row, req, ScheduleViewerConfigProvAlarm.getInstance());
					row.addCell(WidgetHelper.getValidWidgetId("TH-Plot"), logResultSpecial.plotButton);
				}
				
				final GetPlotButtonResult logResult = ChartsUtil.getPlotButton(id, object, appManPlus.dpService(), appMan, false, vh, row, req, pe,
						ScheduleViewerConfigProvAlarm.getInstance(), null);
				row.addCell("Plot", logResult.plotButton);
				
				if(pageType == KnownFaultsPageType.SUPERVISION_STANDARD) {
					TemplateDropdown<DevelopmentTask> devTaskDrop = new DevelopmentTaskDropdown(object, resData, appMan, controller,
							vh.getParent(), "devTaskDrop"+id, req);
					row.addCell(WidgetHelper.getValidWidgetId("Special Set(Dev)"), devTaskDrop);
					final Dropdown alarmingDelayDrop = new Dropdown(mainTable, "alarmingDelayDrop"+id, req) {
						
						@Override
						public void onGET(OgemaHttpRequest req) {
							final int delayHours = (int)(object.minimumIntervalBetweenNewValues().getValue()/60);
							selectSingleOption(String.valueOf(delayHours), req); // find closest option matching the delay?
						}
						
						@Override
						public void onPOSTComplete(String arg0, OgemaHttpRequest req) {
							final String selected = getSelectedValue(req);
							try {
								final int delay = Integer.parseInt(selected);
								if (delay == 0) {
									object.minimumIntervalBetweenNewValues().setValue(-1);
								} else {
									ValueResourceHelper.setCreate(object.minimumIntervalBetweenNewValues(), delay*60);
								}
							} catch (NumberFormatException ignore) {}
						}
						
					};
					alarmingDelayDrop.setDefaultOptions(IntStream.builder().add(0).add(6).add(12).add(24).add(48).add(72).add(168).build()
						.mapToObj(i -> new DropdownOption(i > 0 ? String.valueOf(i) : "", i > 0 ? i + "h" : "--", i == 0))
						.collect(Collectors.toList()));
					row.addCell(WidgetHelper.getValidWidgetId("Alarming delay"), alarmingDelayDrop);
					
				}
			}
			
			@Override
			public List<InstallAppDevice> getObjectsInTable(OgemaHttpRequest req) {
				List<InstallAppDevice> all = super.getObjectsInTable(req);
				return getDevicesWithKnownFault(all);
			}
		};
		return result;
	}
	
	private Button updateReleaseBtn(
			final AlarmGroupData res,
			final AtomicReference<Button> releaseBtnRef, 
			final AtomicInteger releaseCnt,
			final PageSnippet releaseBtnSnippet,
			final String id,
			final OgemaHttpRequest req) {
		final Button release = releaseBtnRef.get();
		if (release == null) // XXX?
			return release;
		releaseBtnSnippet.remove(release, req);
		try {
			Thread.sleep(1000); // replacement of the alarm resource by a reference is done by some other listener...
		} catch(InterruptedException e) {
			Thread.currentThread().interrupt();
			return null;
		}
		final Button newRelease = createReleaseBtn(res.getLocationResource(), releaseBtnSnippet, id + releaseCnt.getAndIncrement(), req);
		releaseBtnSnippet.append(newRelease, req);
		releaseBtnRef.set(newRelease);
		return newRelease;
	}
	
	private Button createReleaseBtn(final AlarmGroupData res, final OgemaWidget parent, final String id, final OgemaHttpRequest req) {
		final Button releaseBut;
		if(res instanceof AlarmGroupDataMajor 
				|| (res.assigned().isActive() &&
						(res.assigned().getValue() > 0) && (res.assigned().getValue() != AlarmingConfigUtil.ASSIGNMENT_DEPDENDENT)) 
				|| res.responsibility().isActive()) {
			releaseBut = new Button(parent, "releaseBut"+id, "Release", req) {
				
				/*  // opens a popup 
				@Override
				public void onGET(OgemaHttpRequest req) {
					int status = res.forRelease().getValue();
					if(status > 1)
						setStyle(ButtonData.BOOTSTRAP_ORANGE, req);
					else if(status > 0)
						setStyle(ButtonData.BOOTSTRAP_GREEN, req);
					else
						setStyles(Collections.emptyList(), req);							
				}
				*/
				
				@Override
				public void onPOSTComplete(String arg0, OgemaHttpRequest req) {
					releasePopup.selectIssue(res, req);
				}
				
			};
			releasePopup.trigger(releaseBut);
			releaseBut.addDefaultStyle(ButtonData.BOOTSTRAP_LIGHT_BLUE);
			releaseBut.setDefaultToolTip("Open the release popup to provide release information");
		} else {
			//Unassigned issues shall still just be released without analysis
			releaseBut = new Button(parent, "releaseBut"+id, "Release", req) {
				
				@Override
				public void onGET(OgemaHttpRequest req) {
					int status = res.forRelease().getValue();
					if(status > 1)
						setStyle(ButtonData.BOOTSTRAP_ORANGE, req);
					else if(status > 0)
						setStyle(ButtonData.BOOTSTRAP_GREEN, req);
					else
						setStyles(Collections.emptyList(), req); // red maybe?
				}
				
				@Override
				public void onPOSTComplete(String data, OgemaHttpRequest req) {
					//TODO: In the future we may want to keep this information in a log of solved issues
					AlarmResourceUtil.release(res, appMan.getFrameworkTime());
					//res.delete();
					//res.ongoingAlarmStartTime().setValue(-1);
				}
			};
			releaseBut.setDefaultToolTip("Directly delete the issue.");
		}
		return releaseBut;
	}
	
	protected List<InstallAppDevice> getDevicesWithKnownFault(List<InstallAppDevice> all) {
		if(showAllDevices)
			return all;
		List<InstallAppDevice> result = new ArrayList<>();
		for(InstallAppDevice dev: all) {
			if(dev.knownFault().exists()) { // && dev.knownFault().ongoingAlarmStartTime().getValue() > 0) {
				//int[] actAlarms = AlarmingConfigUtil.getActiveAlarms(dev);
				//if(actAlarms[1] > 0)
				result.add(dev);
			}
		}
		return result;		
	}
	
	@Override
	protected boolean isObjectsInTableEmpty(DeviceHandlerProvider<?> pe, OgemaHttpRequest req) {
		List<InstallAppDevice> all = getDevicesSelected(pe, req);
		List<InstallAppDevice> result = getDevicesWithKnownFault(all);
		return result.isEmpty();
	}
	
	
	public static void releaseAllUnassigned(DatapointService dpService, long now) {
		Collection<InstallAppDevice> all = dpService.managedDeviceResoures(null, false, false);
		releaseAllUnassigned(all, now);
	}
	
	public static void releaseAllUnassigned(Collection<InstallAppDevice> allforPe, long now) {
		for(InstallAppDevice iad: allforPe) {
			AlarmGroupData res = iad.knownFault();
			if((!res.assigned().isActive()) || (res.assigned().getValue() <= 0)) {
				AlarmResourceUtil.release(res, now);
				//res.delete();
			}
		}		
	}
	
	public static void setAllUnassignedDependent(Collection<InstallAppDevice> allforPe) {
		for(InstallAppDevice iad: allforPe) {
			AlarmGroupData res = iad.knownFault();
			if(res.isActive() && (res.assigned().getValue() <= 0)) {
				ValueResourceHelper.setCreate(res.assigned(), AlarmingConfigUtil.ASSIGNMENT_DEPDENDENT);
			}
		}		
	}

	public static void releaseAllDependent(Collection<InstallAppDevice> allforPe, long now) {
		for(InstallAppDevice iad: allforPe) {
			AlarmGroupData res = iad.knownFault();
			if(res.assigned().getValue() == AlarmingConfigUtil.ASSIGNMENT_DEPDENDENT) {
				AlarmResourceUtil.release(res, now);
				//res.delete();
			}
		}		
	}
	
	public static boolean isMasterUser(String user) {
		return user.startsWith("master") || (user.startsWith("support") && (!user.contains("@")));
	}

	public static void setHistoryForFollowupChange(String deviceId,
			Long preValue, TimeResource newValue, String responsibility, 
			OgemaHttpRequest req,
			AlarmingConfigAppController controller) {
		if(preValue == null)
			return;
		long now = controller.appMan.getFrameworkTime();
		float oldValueF = (float) (((double)(preValue-now))/TimeProcUtil.DAY_MILLIS);
		String user = GUIUtilHelper.getUserLoggedIn(req);
		if(newValue == null || (!newValue.isActive())) {
			controller.addHistoryItem(user, "Disabled reminder for "+responsibility, null, oldValueF, deviceId);
		}
		float newValueF = (float) (((double)(newValue.getValue()-now))/TimeProcUtil.DAY_MILLIS);
		controller.addHistoryItem(user, "Set reminder days for "+responsibility, newValueF, oldValueF, deviceId);
	}
	
	public static void setHistoryForResponsibleChange(String deviceId,
			String preValue, StringResource responsibility, OgemaHttpRequest req,
			AlarmingConfigAppController controller) {
		if(preValue == null || preValue.isEmpty())
			return;
		String user = GUIUtilHelper.getUserLoggedIn(req);
		if(responsibility == null || (!responsibility.isActive())) {
			controller.addHistoryItem(user, "Disabled responsibility", null, preValue, deviceId);
		}
		controller.addHistoryItem(user, "Set responsibility", responsibility.getValue(), preValue, deviceId);
	}
	
	public static void setHistoryForTaskDelete(String deviceId,
			String responsibility, boolean movedToTrash, OgemaHttpRequest req,
			AlarmingConfigAppController controller) {
		String user = GUIUtilHelper.getUserLoggedIn(req);
		if(!movedToTrash) {
			controller.addHistoryItem(user, "Deleted Device Issue for "+responsibility, "--", "--", deviceId);
		}
		controller.addHistoryItem(user, "Set Device Issue to Trash for "+responsibility, "--", "--", deviceId);
	}
}
