package org.smartrplace.apps.hw.install.gui.alarm;

import java.time.Instant;
import java.time.Month;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.ogema.core.application.ApplicationManager;
import org.ogema.core.model.Resource;
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
import org.ogema.model.extended.alarming.DevelopmentTask;
import org.ogema.model.prototypes.PhysicalElement;
import org.ogema.model.user.NaturalPerson;
import org.ogema.tools.resource.util.ResourceUtils;
import org.smartrplace.apps.alarmconfig.util.AlarmMessageUtil;
import org.smartrplace.apps.alarmconfig.util.AlarmResourceUtil;
import org.smartrplace.apps.alarmingconfig.AlarmingConfigAppController;
import org.smartrplace.apps.hw.install.config.HardwareInstallConfig;
import org.smartrplace.apps.hw.install.config.InstallAppDevice;
import org.smartrplace.apps.hw.install.gui.ThermostatPage;
import org.smartrplace.gateway.device.GatewaySuperiorData;
import org.smartrplace.util.directobjectgui.ObjectGUIHelperBase.ValueResourceDropdownFlex;
import org.smartrplace.util.directobjectgui.ObjectResourceGUIHelper;
import org.smartrplace.util.format.WidgetHelper;
import org.smartrplace.util.virtualdevice.ChartsUtil;
import org.smartrplace.util.virtualdevice.ChartsUtil.GetPlotButtonResult;
import org.smartrplace.widget.extensions.GUIUtilHelper;

import com.google.common.base.Objects;

import de.iwes.util.resource.ValueResourceHelper;
import de.iwes.widgets.api.extended.html.bricks.PageSnippet;
import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.dynamics.TriggeredAction;
import de.iwes.widgets.api.widgets.dynamics.TriggeringAction;
import de.iwes.widgets.api.widgets.html.StaticTable;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.alert.Alert;
import de.iwes.widgets.html.autocomplete.Autocomplete;
import de.iwes.widgets.html.autocomplete.AutocompleteData;
import de.iwes.widgets.html.buttonconfirm.ButtonConfirm;
import de.iwes.widgets.html.complextable.RowTemplate.Row;
import de.iwes.widgets.html.form.button.Button;
import de.iwes.widgets.html.form.button.ButtonData;
import de.iwes.widgets.html.form.button.RedirectButton;
import de.iwes.widgets.html.form.checkbox.SimpleCheckbox;
import de.iwes.widgets.html.form.dropdown.Dropdown;
import de.iwes.widgets.html.form.dropdown.DropdownData;
import de.iwes.widgets.html.form.dropdown.DropdownOption;
import de.iwes.widgets.html.form.dropdown.TemplateDropdown;
import de.iwes.widgets.html.form.label.Label;
import de.iwes.widgets.html.form.label.LabelData;
import de.iwes.widgets.html.form.textfield.TextField;
import de.iwes.widgets.html.html5.Flexbox;
import de.iwes.widgets.html.html5.flexbox.JustifyContent;
import de.iwes.widgets.html.popup.Popup;
import de.iwes.widgets.resource.widget.dropdown.ReferenceDropdown;
import de.iwes.widgets.resource.widget.textfield.ValueResourceTextField;

@SuppressWarnings("serial")
public class DeviceKnownFaultsPage extends DeviceAlarmingPage {
	//@Deprecated //Currently not used, but still available in the details section
	
	public static enum KnownFaultsPageType {
		OPERATION_STANDARD,
		SUPERVISION_STANDARD
	}
	final KnownFaultsPageType pageType;
	private final Button createIssueSubmit;
	private final Popup lastMessagePopup;
	private final Label lastMessageDevice;
	private final Label lastMessageRoom;
	private final Label lastMessageLocation;
	private final Label lastMessage;
	
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
		// popup for message display
		this.lastMessagePopup = new Popup(page, "lastMessagePopup", true);
		lastMessagePopup.setDefaultTitle("Last alarm message");
		this.lastMessageDevice = new Label(page, "lastMessagePopupDevice");
		this.lastMessageRoom = new Label(page, "lastMessagePopupRoom");
		this.lastMessageLocation = new Label(page, "lastMessagePopupLocation");
		this.lastMessage = new Label(page, "lastMessage");
		
		
		final StaticTable tab = new StaticTable(4, 2, new int[]{3, 9});
		tab.setContent(0, 0, "Device").setContent(0, 1, lastMessageDevice)
			.setContent(1, 0, "Room").setContent(1, 1, lastMessageRoom)
			.setContent(2, 0, "Location").setContent(2, 1, lastMessageLocation)
			.setContent(3, 0, "Message").setContent(3,1, lastMessage);
		final PageSnippet snip = new PageSnippet(page, "lastMessageSnip", true);
		snip.append(tab, null);
		lastMessagePopup.setBody(snip, null);
		final Button closeLastMessage = new Button(page, "lastMessageClose", "Close");
		closeLastMessage.triggerAction(lastMessagePopup, TriggeringAction.ON_CLICK, TriggeredAction.HIDE_WIDGET);
		lastMessagePopup.setFooter(closeLastMessage, null);
		page.append(lastMessagePopup);
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
		
		final Popup createIssuePopup = new Popup(page, "createIssuePopup", true);
		//final Header createHeader = new Header(page, "createIssueHeader", "Create known issue");
		//createHeader.setDefaultHeaderType(2);
		//createIssuePopup.setHeader(createHeader, null);
		createIssuePopup.setTitle("Create known issue", null);
		
		final StaticTable createPopupTable = new StaticTable(4, 2, new int[] {4, 8});
		final Label createIssueDeviceLab = new Label(page, "createIssueDeviceLab", "Select device");
		final Autocomplete deviceSelector = new Autocomplete(page, "createIssueDeviceSelector") {
			
			@Override
			public DeviceSelectorData getData(OgemaHttpRequest req) {
				return (DeviceSelectorData) super.getData(req);
			}
			
			@Override
			public AutocompleteData createNewSession() {
				return new DeviceSelectorData(this);
			}
			
			@Override
			public void onGET(OgemaHttpRequest req) {
				final List<InstallAppDevice> devices = appMan.getResourceAccess().getResources(HardwareInstallConfig.class).stream()
					.flatMap(hwi -> hwi.knownDevices().getAllElements().stream())
					.filter(dev -> dev.deviceId().isActive())
					.collect(Collectors.toList());
				getData(req).setItems(devices);
			}
			
			@Override
			public void onPOSTComplete(String data, OgemaHttpRequest req) {
				final InstallAppDevice device = getData(req).getSelectedItem();
				final boolean active = device != null;
				if (active)
					createIssueSubmit.enable(req);
				else
					createIssueSubmit.disable(req);
			}
			
		};
		// expensive
		deviceSelector.postponeLoading();
		
		final Label deviceFaultActiveLab = new Label(page, "createIssueFaultActiveLab", "Fault active?");
		deviceFaultActiveLab.setDefaultToolTip("Is the currently selected device in an alarm state?");
		final Label deviceFaultActive = new Label(page, "createIssueFaultActive") {
			
			@Override
			public void onGET(OgemaHttpRequest req) {
				final InstallAppDevice device = ((DeviceSelectorData) deviceSelector.getData(req)).getSelectedItem();
				if (device == null) {
					setText("", req);
				} else {
					final boolean active = device.knownFault().isActive();
					setText(active + "", req);
				}
			}
			
		};
		deviceFaultActive.setDefaultToolTip("Is the currently selected device in an alarm state?");
		final Label createIssueCommentLab = new Label(page, "createIssueCommentLab", "Comment");
		final TextField createIssueComment = new TextField(page, "createIssueComment") {
			
			@Override
			public void onGET(OgemaHttpRequest req) {
				final InstallAppDevice device = ((DeviceSelectorData) deviceSelector.getData(req)).getSelectedItem();
				if (device == null || !device.knownFault().comment().isActive()) {
					setValue("", req);
				} else {
					setValue(device.knownFault().comment().getValue(), req);
				}
			}
		};
		
		final Label createIssueAssignedLab = new Label(page, "createIssueAssignedLab", "Assigned");
		final Dropdown createIssueAssigned = new Dropdown(page, "createIssueAssigned") {
			
			@Override
			public void onGET(OgemaHttpRequest req) {
				final InstallAppDevice device = ((DeviceSelectorData) deviceSelector.getData(req)).getSelectedItem();
				if (device == null || !device.knownFault().assigned().isActive()) {
					selectSingleOption("0", req);
				} else {
					final int val = device.knownFault().assigned().getValue();
					selectSingleOption(val + "", req);
				}
			}
			
		};
		final Collection<DropdownOption> assignmentOpts = AlarmingConfigUtil.ASSIGNEMENT_ROLES.entrySet().stream()
			.map(entry -> new DropdownOption(entry.getKey(), entry.getValue(), "0".equals(entry.getKey())))
			.collect(Collectors.toList());
		createIssueAssigned.setDefaultOptions(assignmentOpts);
		
		createPopupTable.setContent(0, 0, createIssueDeviceLab).setContent(0, 1, deviceSelector);
		createPopupTable.setContent(1, 0, deviceFaultActiveLab).setContent(1, 1, deviceFaultActive);
		createPopupTable.setContent(2, 0, createIssueCommentLab).setContent(2, 1, createIssueComment);
		createPopupTable.setContent(3, 0, createIssueAssignedLab).setContent(3, 1, createIssueAssigned);
		
		
		final PageSnippet bodySnippet = new PageSnippet(page, "createIssueBodySnip", true);
		bodySnippet.append(createPopupTable, null);
		createIssuePopup.setBody(bodySnippet, null);
		
		final Button cancel = new Button(page, "createIssueCancel", "Cancel");
		cancel.setDefaultToolTip("Cancel alarm generation and close this popup");
		final Button submit = new Button(page, "createIssueSubmit", "Submit") {
			
			@Override
			protected void setDefaultValues(ButtonData opt) {
				super.setDefaultValues(opt);
				opt.disable();
			}
			
			@Override
			public void onPOSTComplete(String arg0, OgemaHttpRequest req) {
				final InstallAppDevice device = ((DeviceSelectorData) deviceSelector.getData(req)).getSelectedItem();
				if (device == null) {
					if (alert != null)
						alert.showAlert("Device " + device + " not found", false, req);
					return;
				}
				try {
					final AlarmGroupData alarm = device.knownFault();
					final String comment = createIssueComment.getValue(req).trim();
					if (comment.isEmpty())
						alarm.comment().delete();
					else
						alarm.comment().<StringResource> create().setValue(comment);
					final int assignment = Integer.parseInt(createIssueAssigned.getSelectedValue(req));
					if (assignment == 0)
						alarm.assigned().delete();
					else
						alarm.assigned().<IntegerResource> create().setValue(assignment);
					final boolean isBlocking = assignment < 7000 || assignment >= 8000;
					alarm.minimumTimeBetweenAlarms().<FloatResource> create().setValue(isBlocking ? -1 : 0);
					if (!alarm.isActive())
						alarm.ongoingAlarmStartTime().<TimeResource> create().setValue(appMan.getFrameworkTime());
					alarm.activate(true);
					if (alert != null)
						alert.showAlert("Alarm generation succeeded for device " + device.deviceId().getValue() + " (" + device.getLocation() + ")", true, req);
				} catch (Exception e) {
					if (alert != null)
						alert.showAlert("Alarm generation failed: " + e, false, req);
					else
						appMan.getLogger().error("Alarm generation failed", e);
				}
				// TODO reload page?
			}
			
		};
		submit.setDefaultToolTip("Create alarm");
		submit.addDefaultStyle(ButtonData.BOOTSTRAP_GREEN);
		this.createIssueSubmit = submit;
		
		final Flexbox popupFooter = new Flexbox(page, "createIssueFooter", true);
		popupFooter.setJustifyContent(JustifyContent.FLEX_RIGHT, null);
		popupFooter.addCssItem(">div", Collections.singletonMap("column-gap", "0.5em"), null);
		popupFooter.addItem(cancel, null).addItem(submit, null);
		createIssuePopup.setFooter(popupFooter, null);
		
		
		page.append(createIssuePopup);
		deviceSelector.triggerAction(deviceFaultActive, TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST);
		deviceSelector.triggerAction(createIssueComment, TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST);
		deviceSelector.triggerAction(createIssueAssigned, TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST);
		deviceSelector.triggerAction(submit, TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST);
		createKnownIssue.triggerAction(createIssuePopup, TriggeringAction.POST_REQUEST, TriggeredAction.SHOW_WIDGET);
		cancel.triggerAction(createIssuePopup, TriggeringAction.POST_REQUEST, TriggeredAction.HIDE_WIDGET);
		submit.triggerAction(createIssuePopup, TriggeringAction.POST_REQUEST, TriggeredAction.HIDE_WIDGET);
		if (alert != null)
			submit.triggerAction(alert, TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST);
		
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
				if(user.equals("master"))
					setUrl("/ogema/index.html", req);
			}
			
		};
		topTable.setContent(1, 3, homeScreen);
		
		RedirectButton thermostatPage = new RedirectButton(page, "thermostatPage", "Devices", "/org/smartrplace/hardwareinstall/index.html") {
			@Override
			public void onGET(OgemaHttpRequest req) {
				String user = GUIUtilHelper.getUserLoggedIn(req);
				if(user.equals("master")) {
					setUrl("/org/smartrplace/hardwareinstall/expert/thermostatDetails2.hmtl.html", req);
					setText("Thermostats", req);
				}
			}
			
		};
		topTable.setContent(1, 4, thermostatPage);
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
		final boolean showAlarmCtrl = pageType == KnownFaultsPageType.SUPERVISION_STANDARD;
		final AlarmingDeviceTableBase result = new AlarmingDeviceTableBase(page, appManPlus, alert, pageTitle, resData, commitButton, appSelector, pe, showAlarmCtrl, false) {
			protected void addAdditionalWidgets(final InstallAppDevice object, ObjectResourceGUIHelper<InstallAppDevice,InstallAppDevice> vh, String id,
					OgemaHttpRequest req, Row row, final ApplicationManager appMan,
					PhysicalElement device, final InstallAppDevice template) {
				if(req == null) {
					//vh.registerHeaderEntry("Main value");
					vh.getHeader().put("value", "Value/contact");
					vh.registerHeaderEntry("Started");
					vh.registerHeaderEntry("Message");
					vh.registerHeaderEntry("Details");
					vh.registerHeaderEntry("Comment");
					vh.registerHeaderEntry("Assigned");
					vh.registerHeaderEntry("Task Tracking");
					vh.registerHeaderEntry("Priority");
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
				AlarmGroupData res = object.knownFault();
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
				final Button showMsg = new Button(mainTable, "msg" + id, req) {
					
					@Override
					public void onPOSTComplete(String data, OgemaHttpRequest req) {
						lastMessage.setText(res.lastMessage().getValue(), req);
						lastMessageDevice.setText(object.deviceId().getValue(), req);
						final String room = device.location().room().isActive() ? ResourceUtils.getHumanReadableShortName(device.location().room()) : "--";
						lastMessageRoom.setText(room, req);
						final String location = object.installationLocation().isActive() ? object.installationLocation().getValue() : "--";
						lastMessageLocation.setText(location, req);
					}
					
				};
				showMsg.setDefaultText("Last message");
				showMsg.setDefaultToolTip("Show the last alarm message sent for this device, which contains some details about the source of the alarm.");
				showMsg.triggerAction(lastMessagePopup, TriggeringAction.POST_REQUEST, TriggeredAction.SHOW_WIDGET, req);
				showMsg.triggerAction(lastMessageDevice,  TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST, req);
				showMsg.triggerAction(lastMessageRoom,  TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST, req);
				showMsg.triggerAction(lastMessageLocation,  TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST, req);
				
				showMsg.triggerAction(lastMessage,  TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST, req);
				row.addCell("Message", showMsg);
				
				final RedirectButton detailsRedirect = new RedirectButton(mainTable, "details" + id, "Details", 
						"/org/smartrplace/alarmingexpert/ongoingbase.html?device=" + object.deviceId().getValue(), req);
				detailsRedirect.setToolTip("View alarm details in new tab", req);
				row.addCell("Details", detailsRedirect);
				
				if(res.exists()) {
					vh.stringEdit("Comment",  id, res.comment(), row, alert, res.comment());
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
						}
					};
					row.addCell("Assigned", widgetPlus.myDrop);
					
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
					row.addCell("Priority", prioField);
					if(pageType == KnownFaultsPageType.SUPERVISION_STANDARD)
						vh.stringEdit("Edit TT",  id, res.linkToTaskTracking(), row, alert);
					
					// TODO do we need to display the email address as well?
					final Dropdown responsibleDropdown = new Dropdown(mainTable, "responsible"+id, req) {
						
						@Override
						public void onGET(OgemaHttpRequest req) {
							final GatewaySuperiorData supData = findSuperiorData();
							if (supData == null || !supData.responsibilityContacts().isActive())
								return;
							final StringResource responsibility = object.knownFault().responsibility();
							final String email = responsibility.isActive() ? responsibility.getValue() : "";
							final NaturalPerson selected = email.isEmpty() ? null : supData.responsibilityContacts().getAllElements().stream()
								.filter(c -> email.equals(c.getSubResource("emailAddress", StringResource.class).getValue()))
								.findAny().orElse(null);
							final List<DropdownOption> options = supData.responsibilityContacts().getAllElements().stream()
								.map(contact -> new DropdownOption(
										contact.getName(), contact.userRole().isActive() ? contact.userRole().getValue() :
										contact.firstName().getValue() + " " + contact.lastName().getValue(), 
										contact.equalsLocation(selected)
								))
								.collect(Collectors.toList());
							setOptions(options, req);
							if (selected != null) {
								final String id = selected.userRole().isActive() ? selected.userRole().getValue() : selected.firstName().getValue() + " " + selected.lastName().getValue();
								setToolTip(id + ": " + email, req);
							} else {
								setToolTip(email.isEmpty() ? "Select responsible" :  email, req);
							}
						}
						
						@Override
						public void onPOSTComplete(String arg0, OgemaHttpRequest req) {
							final GatewaySuperiorData supData = findSuperiorData();
							if (supData == null || !supData.responsibilityContacts().isActive())
								return;
							final String currentSelected = getSelectedValue(req);
							final StringResource responsibility = object.knownFault().responsibility();
							if (currentSelected == null || currentSelected.isEmpty() || currentSelected.equals(DropdownData.EMPTY_OPT_ID) 
										|| supData.responsibilityContacts().getSubResource(currentSelected) == null) {
								responsibility.delete();
								return;
							}
							final NaturalPerson selected = supData.responsibilityContacts().getSubResource(currentSelected); 
							final StringResource emailRes = selected.getSubResource("emailAddress");
							final String email = emailRes.isActive() ? emailRes.getValue() : "";
							if (email.isEmpty()) { // ?
								return;
							}
							responsibility.<StringResource> create().setValue(email);
							responsibility.activate(false);
						}
						
						
					};
					responsibleDropdown.setDefaultAddEmptyOption(true);
					responsibleDropdown.setDefaultMinWidth("8em");
					responsibleDropdown.setComparator((o1, o2) ->  { // show default roles supervision and terminvereinbarung first
						if (Objects.equal(o1, o2))
							return 0;
						final boolean comp1 = o1.id().indexOf('_') > 0;
						final boolean comp2 = o2.id().indexOf('_') > 0;
						if (comp1 == comp2)
							return o1.id().compareTo(o2.id());
						return comp1 ? 1 : -1;
					});
					responsibleDropdown.triggerAction(responsibleDropdown, TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST);
					row.addCell("Responsible", responsibleDropdown);
					
				}
				
				final Dropdown followupemail = new Dropdown(mainTable, "followup" + id, req) {
					
					@Override
					public void onGET(OgemaHttpRequest req) {
						final TimeResource followup = res.dueDateForResponsibility();
						final Optional<String> custom = getDropdownOptions(req).stream()
							.map(opt -> opt.id())
							.filter(opt -> opt.startsWith("custom"))
							.findAny();
						final boolean customConfigured = custom.isPresent();
						final boolean needsCustom = followup.exists() && followup.getValue() >= appMan.getFrameworkTime();
						if (!needsCustom) {
							if (customConfigured) {
								setOptions(getDropdownOptions(req).stream()
										.filter(opt -> !opt.id().startsWith("custom"))
										.collect(Collectors.toList()), req);
							}
							return;
						}
						final long target = followup.getValue();
						final String id = "custom" + target;
						if (customConfigured && custom.get().equals(id))
							return;
						final Stream<DropdownOption> standardOpts = getDropdownOptions(req).stream()
							.filter(opt -> !opt.id().startsWith("custom"));
						final long diff = target - appMan.getFrameworkTime();
						final ZonedDateTime targetZdt = ZonedDateTime.ofInstant(Instant.ofEpochMilli(target), ZoneId.systemDefault());
						final boolean printTime = Math.abs(diff) < 36 * 3_600_000;
						final String dateTime = printTime ? DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(targetZdt) : DateTimeFormatter.ISO_LOCAL_DATE.format(targetZdt);
						final Stream<DropdownOption> customOpt = Stream.of(new DropdownOption(id, dateTime, true));
						final List<DropdownOption> newOptions = Stream.concat(customOpt, standardOpts).collect(Collectors.toList());
						setOptions(newOptions, req);
						selectSingleOption(followup.isActive() ? id : "__EMPTY_OPT__", req);
					}
					
					@Override
					public void onPOSTComplete(String data, OgemaHttpRequest req) {
						final String value = getSelectedValue(req);
						if (value == null || value.isEmpty()) // ?
							return;
						final TimeResource followup = res.dueDateForResponsibility();
						if ("__EMPTY_OPT__".equalsIgnoreCase(value)) {
							if (followup.isActive()) {
								followup.deactivate(false);
								if (alert != null && object.device().exists()) {
									alert.showAlert("Email reminder for device " + ResourceUtils.getHumanReadableName(object.device().getLocationResource()) 
										+ " has been cancelled", true, req);
								}
							}
							return;
						}
						final long now0 = appMan.getFrameworkTime();
						final ZonedDateTime now = ZonedDateTime.ofInstant(Instant.ofEpochMilli(now0), ZoneId.systemDefault());
						final long timestamp;
						if (value.startsWith("custom"))
							timestamp = Long.parseLong(value.substring("custom".length()));
						else if (value.endsWith("d") && value.length() < 5) {
							final int days = Integer.parseInt(value.substring(0, value.length()-1));
							timestamp = now.plusDays(days).toEpochSecond()*1000;
						} else if (value.equals("1min")) { // debug option
							timestamp = now.plusMinutes(1).toEpochSecond()*1000;
						} else if (value.equals("nextmonthend"))
							timestamp = now.plusMonths(1).with(TemporalAdjusters.lastDayOfMonth()).truncatedTo(ChronoUnit.DAYS).toEpochSecond()*1000;
						else if (value.equals("3months"))
							timestamp = now.plusMonths(3).toEpochSecond()*1000;
						else if (value.equals("august")) {
							final Month month = now.getMonth();
							ZonedDateTime t0 = now;
							if (month.compareTo(Month.AUGUST) > 0 || (month == Month.AUGUST && now.getDayOfMonth() == 31))
								t0 = t0.plusYears(1);
							timestamp = t0.with(Month.AUGUST).with(TemporalAdjusters.lastDayOfMonth()).truncatedTo(ChronoUnit.DAYS).toEpochSecond()*1000;
						} else {
							throw new IllegalArgumentException("unknown follow-up date " + value);
						}
						followup.<TimeResource> create().setValue(timestamp);
						if (timestamp > now0) {
							followup.activate(false);
							if (alert != null && object.device().exists()) {
								alert.showAlert("Email reminder for device " + ResourceUtils.getHumanReadableName(object.device().getLocationResource()) 
									+ " has been configured for " + ZonedDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneId.systemDefault()), true, req);
							}
						}
					}
					
				};
				followupemail.setComparator(null);
				followupemail.setDefaultOptions(Arrays.asList(
					new DropdownOption("__EMPTY_OPT__", "inactive", true),
					new DropdownOption("1d", "1 day", false),
					new DropdownOption("2d", "2 days", false),
					new DropdownOption("3d", "3 days", false),
					new DropdownOption("7d", "7 days", false),
					new DropdownOption("30d", "30 days", false),
					new DropdownOption("nextmonthend", "End of next month", false),
					new DropdownOption("3months", "3 months", false),
					new DropdownOption("august", "End of August", false)
				));
				// add a 1 minute option for debugging purposes
				if (Boolean.getBoolean("org.smartrplace.apps.alarmingconfig.devicealarmreminder.debug")) {
					followupemail.setDefaultOptions(Stream.concat(Stream.of(new DropdownOption("1min", "1 minute", false)), followupemail.getDefaultOptions().stream())
						.collect(Collectors.toList()));
				}
				followupemail.setDefaultToolTip("Send a reminder email after the specified period");
				followupemail.setDefaultMinWidth("8em");
				followupemail.triggerAction(followupemail, TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST);
				if (alert != null)
					followupemail.triggerAction(alert, TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST);
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
				if(res.assigned().isActive() &&
						(res.assigned().getValue() > 0) && (res.forRelease().getValue() == 0)) {
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
				} else {
					releaseBut = new Button(mainTable, "releaseBut"+id, "Create", req) {
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
				row.addCell("Release", releaseBut);
				
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
	
	private GatewaySuperiorData findSuperiorData() {
		final Resource r = appMan.getResourceAccess().getResource("gatewaySuperiorDataRes");
		if (r instanceof GatewaySuperiorData)
			return (GatewaySuperiorData) r;
		return appMan.getResourceAccess().getResources(GatewaySuperiorData.class).stream().findAny().orElse(null);
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
	
	private static class DeviceSelectorData extends AutocompleteData {
		
		private Collection<InstallAppDevice> options = Collections.emptySet();
		private InstallAppDevice selectedDevice;

		public DeviceSelectorData(Autocomplete autocomplete) {
			super(autocomplete);
			setMinLength(0);
		}
		
		public void setItems(Collection<InstallAppDevice> devices) {
			this.options = devices;
			if (this.selectedDevice != null && !devices.contains(this.selectedDevice))
				this.selectedDevice = null;
			final List<String> ids =
					Stream.concat(Stream.of(""), devices.stream().map(dev -> dev.deviceId().getValue())).collect(Collectors.toList());
			setOptions(ids);
		}
		
		public InstallAppDevice getSelectedItem() {
			return this.selectedDevice;
		}
		
		@Override
		public void setValue(String value) {
			final Optional<InstallAppDevice> device = 
					value != null ? this.options.stream().filter(app -> app.deviceId().isActive() && value.equals(app.deviceId().getValue())).findAny() : Optional.empty();
			this.selectedDevice = device.orElse(null);
			super.setValue(device.isPresent() ? value : null);
		}
		
		
	}
	
}
