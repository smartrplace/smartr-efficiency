package org.smartrplace.apps.hw.install.gui.alarm;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
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

import org.ogema.core.application.ApplicationManager;
import org.ogema.core.model.Resource;
import org.ogema.core.model.simple.FloatResource;
import org.ogema.core.model.simple.IntegerResource;
import org.ogema.core.model.simple.SingleValueResource;
import org.ogema.core.model.simple.TimeResource;
import org.ogema.core.model.units.VoltageResource;
import org.ogema.devicefinder.api.DatapointGroup;
import org.ogema.devicefinder.api.DatapointService;
import org.ogema.devicefinder.api.DeviceHandlerProvider;
import org.ogema.devicefinder.api.DeviceHandlerProviderDP;
import org.ogema.devicefinder.api.InstalledAppsSelector;
import org.ogema.devicefinder.util.AlarmingConfigUtil;
import org.ogema.devicefinder.util.DeviceHandlerBase;
import org.ogema.devicefinder.util.DeviceTableBase;
import org.ogema.devicefinder.util.DpGroupUtil;
import org.ogema.model.devices.buildingtechnology.Thermostat;
import org.ogema.model.extended.alarming.AlarmConfiguration;
import org.ogema.model.extended.alarming.AlarmGroupData;
import org.ogema.model.extended.alarming.DevelopmentTask;
import org.ogema.model.prototypes.PhysicalElement;
import org.ogema.tools.resource.util.ValueResourceUtils;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.smartrplace.apps.alarmingconfig.AlarmingConfigAppController;
import org.smartrplace.apps.hw.install.config.InstallAppDevice;
import org.smartrplace.apps.hw.install.gui.ThermostatPage;
import org.smartrplace.util.directobjectgui.ObjectGUIHelperBase.ValueResourceDropdownFlex;
import org.smartrplace.util.directobjectgui.ObjectResourceGUIHelper;
import org.smartrplace.util.format.WidgetHelper;
import org.smartrplace.util.virtualdevice.ChartsUtil;
import org.smartrplace.util.virtualdevice.ChartsUtil.GetPlotButtonResult;
import org.smartrplace.widget.extensions.GUIUtilHelper;

import de.iwes.util.resource.ResourceHelper;
import de.iwes.util.resource.ValueResourceHelper;
import de.iwes.widgets.api.extended.html.bricks.PageSnippet;
import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.dynamics.TriggeredAction;
import de.iwes.widgets.api.widgets.dynamics.TriggeringAction;
import de.iwes.widgets.api.widgets.html.StaticTable;
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
import de.iwes.widgets.html.popup.Popup;
import de.iwes.widgets.resource.widget.textfield.ValueResourceTextField;

@SuppressWarnings("serial")
public class DeviceKnownFaultsPage extends DeviceAlarmingPage {
	//@Deprecated //Currently not used, but still available in the details section
	
	public static enum KnownFaultsPageType {
		OPERATION_STANDARD,
		SUPERVISION_STANDARD
	}
	final KnownFaultsPageType pageType;
	private final Popup lastMessagePopup;
	private final Label lastMessageDevice;
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
		this.lastMessage = new Label(page, "lastMessage");
		
		final StaticTable tab = new StaticTable(2, 2, new int[]{3, 9});
		tab.setContent(0, 0, "Device").setContent(0, 1, lastMessageDevice)
			.setContent(1, 0, "Message").setContent(1,1, lastMessage);
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
		topTable.setContent(1, 5, switchAllDeviceBut);
		ButtonConfirm releaseAllUnassigned = new ButtonConfirm(page, "releaseAllUnassigned") {
			@Override
			public void onPOSTComplete(String data, OgemaHttpRequest req) {
				if(devHandAcc != null)  {
					Collection<DeviceHandlerProvider<?>> allProvs = devHandAcc.getTableProviders().values();
					for(DeviceHandlerProvider<?> pe: allProvs) {
						List<InstallAppDevice> allforPe = getDevicesSelected(pe, req);
						releaseAllUnassigned(allforPe);
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
						releaseAllDependent(allforPe);
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
		final AlarmingDeviceTableBase result = new AlarmingDeviceTableBase(page, appManPlus, alert, pageTitle, resData, commitButton, appSelector, pe, showAlarmCtrl) {
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
				final ValueData valueData = getValueData(object, appMan);
				final Label valueField = new Label(mainTable, "valueField" + id, req);
				valueField.setText(valueData.message, req);
				row.addCell("value", valueField);
				if (valueData.responsibleResource != null)
					valueField.setToolTip("Value resource: " + valueData.responsibleResource.getLocationResource(), req);
				
				vh.timeLabel("Started", id, res.ongoingAlarmStartTime(), row, 0);
				final Button showMsg = new Button(mainTable, "msg" + id, req) {
					
					@Override
					public void onPOSTComplete(String data, OgemaHttpRequest req) {
						lastMessage.setText(res.lastMessage().getValue(), req);
						lastMessageDevice.setText(object.deviceId().getValue(), req);
					}
					
				};
				showMsg.setDefaultText("Last message");
				showMsg.setDefaultToolTip("Show the last alarm message sent for this device, which contains some details about the source of the alarm.");
				showMsg.triggerAction(lastMessagePopup, TriggeringAction.POST_REQUEST, TriggeredAction.SHOW_WIDGET, req);
				showMsg.triggerAction(lastMessageDevice,  TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST, req);
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
					final ValueResourceTextField<IntegerResource> prioField 
						= new ValueResourceTextField<IntegerResource>(mainTable, "prio" + id, res.getSubResource("priority", IntegerResource.class), req);
					prioField.setDefaultToolTip("Alarm priority, with 0 = lowest priority, 10 = very high priority.");
					prioField.setDefaultWidth("4em");
					prioField.triggerAction(prioField, TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST, req);
					row.addCell("Priority", prioField);
					if(pageType == KnownFaultsPageType.SUPERVISION_STANDARD)
						vh.stringEdit("Edit TT",  id, res.linkToTaskTracking(), row, alert);
				}
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
							res.delete();
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
							res.delete();
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
	
	/**
	 * Generate a message to be shown in the value column. This entails selecting the most relevant
	 * sensor value in alarm state, according to some predefined prioritization.
	 * @param knownDevice
	 * @param appMan
	 * @return
	 */
	private static ValueData getValueData(InstallAppDevice knownDevice, ApplicationManager appMan) {
		final SingleValueResource mainValue = DeviceKnownFaultsPage.getMainSensorValue(knownDevice, appMan.getAppID().getBundle().getBundleContext());
		final VoltageResource batteryVoltage = DeviceHandlerBase.getBatteryVoltage(knownDevice.device());
		final IntegerResource rssiDevice = ResourceHelper.getSubResourceOfSibbling(knownDevice.device(),
				"org.ogema.drivers.homematic.xmlrpc.hl.types.HmMaintenance", "rssiDevice", IntegerResource.class);
		final FloatResource valveState = knownDevice.device() instanceof Thermostat ? 
				((Thermostat) knownDevice.device()).valve().getSubResource("eq3state") : null;
		final Map<SingleValueResource, String> priorityResources = new LinkedHashMap<>();
		// if we find an alarm for any of the below resources, its value or last update is shown in the value field;
		// otherwise we select an arbitrary alarm from the list
		priorityResources.put(batteryVoltage, "low battery voltage");
		priorityResources.put(mainValue, "sensor value range violation");
		priorityResources.put(rssiDevice, "rssi value low");
		priorityResources.put(valveState, "thermostat valve state problematic");
		String valueFieldText = "";
		SingleValueResource responsibleResource = null;
		AlarmStatus status = null;
		String explanation = null;
		for (Map.Entry<SingleValueResource, String> prioEntry: priorityResources.entrySet()) {
			final SingleValueResource prio = prioEntry.getKey();
			final AlarmStatus status0 = DeviceKnownFaultsPage.findAlarmForSensorValue(prio, knownDevice, appMan);
			if (status0 == null || (!status0.valueViolation && !status0.contactViolation))
				continue;
			status = status0;
			responsibleResource = prio;
			explanation = prioEntry.getValue();
			break;
		}
		if (responsibleResource == null) {
			// find any alarm in alarm state
			final Optional<AlarmStatus> statusOpt = knownDevice.alarms().getAllElements().stream()
				.map(alarm -> statusForAlarm(alarm, appMan))
				.filter(alarm -> alarm.valueViolation || alarm.contactViolation)
				.findAny();
			if (statusOpt.isPresent()) {
				status = statusOpt.get();
				responsibleResource = status.config.sensorVal();
			}
		}
		if (responsibleResource == null)  {
			responsibleResource = mainValue;
			explanation = "no problem detected";
		}
		if (responsibleResource != null) {
			if (status == null || status.valueViolation) {
				valueFieldText = "Value: " + ValueResourceUtils.getValue(responsibleResource);
				if (explanation != null)
					valueFieldText += " (" + explanation + ")";
			}
			if (status != null && status.contactViolation) {
				if (status.valueViolation)
					valueFieldText += " (";
				final long lastContact = responsibleResource.getLastUpdateTime();
				valueFieldText += "Last contact: " + DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ssZ", Locale.ENGLISH).format(
						ZonedDateTime.ofInstant(Instant.ofEpochMilli(lastContact), ZoneId.of("Z")));
				if (status.valueViolation)
					valueFieldText += ")";
			}
		}
		return new ValueData(responsibleResource, valueFieldText);
	}
	
	private static AlarmStatus statusForAlarm(AlarmConfiguration cfg, ApplicationManager appMan) {
		final SingleValueResource prio = cfg.sensorVal();
		boolean valueViolation = false;
		boolean contactViolation = false;
		if (prio instanceof IntegerResource || prio instanceof FloatResource || prio instanceof TimeResource) {
			final float value = ValueResourceUtils.getFloatValue(prio);
			valueViolation = (cfg.lowerLimit().isActive() && value < cfg.lowerLimit().getValue()) ||
					(cfg.upperLimit().isActive() && value > cfg.upperLimit().getValue());
		}
		final FloatResource maxInterval = cfg.maxIntervalBetweenNewValues();
		if (maxInterval.isActive() && maxInterval.getValue() > 0) {
			final long interval = appMan.getFrameworkTime() - prio.getLastUpdateTime();
			contactViolation = interval > maxInterval.getValue() * 60 * 1000;
		}
		return new AlarmStatus(cfg, valueViolation, contactViolation);
	}
	
	private static AlarmStatus findAlarmForSensorValue(SingleValueResource prio, InstallAppDevice object, ApplicationManager appMan) {
		if (prio == null || !prio.isActive())
			return null;
		final Optional<AlarmConfiguration> alarmConfigOpt = object.alarms().getAllElements().stream()
				.filter(cfg -> prio.equalsLocation(cfg.sensorVal()))
				.findAny();
		if (!alarmConfigOpt.isPresent())
			return null;
		return statusForAlarm(alarmConfigOpt.get(), appMan);
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
	
	public static void releaseAllUnassigned(DatapointService dpService) {
		Collection<InstallAppDevice> all = dpService.managedDeviceResoures(null, false, false);
		releaseAllUnassigned(all);
	}
	
	public static void releaseAllUnassigned(Collection<InstallAppDevice> allforPe) {
		for(InstallAppDevice iad: allforPe) {
			AlarmGroupData res = iad.knownFault();
			if((!res.assigned().isActive()) || (res.assigned().getValue() <= 0)) {
				res.delete();
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

	public static void releaseAllDependent(Collection<InstallAppDevice> allforPe) {
		for(InstallAppDevice iad: allforPe) {
			AlarmGroupData res = iad.knownFault();
			if(res.assigned().getValue() == AlarmingConfigUtil.ASSIGNMENT_DEPDENDENT) {
				res.delete();
			}
		}		
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private static SingleValueResource getMainSensorValue(InstallAppDevice config, BundleContext ctx) {
		final ServiceReference<DatapointService> dpServiceRef = ctx.getServiceReference(DatapointService.class);
		if (dpServiceRef != null) {
			try {
				final DatapointService dpService = ctx.getService(dpServiceRef);
				final DeviceHandlerProviderDP provider = dpService.getDeviceHandlerProvider(config);
				final SingleValueResource res = provider.getMainSensorValue(config);
				if (res != null)
					return res;
			} catch (Exception ignore) {
			} finally {
				ctx.ungetService(dpServiceRef);
			}
		}
		final PhysicalElement device = config.device();
		final Collection<ServiceReference<DeviceHandlerProviderDP>> references;
		try {
			references = ctx.getServiceReferences(DeviceHandlerProviderDP.class, null);
		} catch (InvalidSyntaxException e) {
			throw new RuntimeException(e);
		}
		List<SingleValueResource> candidates = null;
		for (ServiceReference<DeviceHandlerProviderDP> ref: references) {
			try {
				final DeviceHandlerProviderDP service = ctx.getService(ref);
				try {
					if (!service.getResourceType().isAssignableFrom(device.getResourceType()))
						continue;
					final SingleValueResource res = service.getMainSensorValue(device, config);
					if (res != null) {
						final Resource alarmStatus = res.getSubResource("alarmStatus");
						if (alarmStatus != null && alarmStatus instanceof IntegerResource && ((IntegerResource) alarmStatus).getValue() > 0)
							return res;
						if (candidates == null)
							candidates = new ArrayList<>(4);
						candidates.add(res);
					}
				} finally {
					ctx.ungetService(ref);
				}
			} catch (Exception ignore) {}
			
		}
		if (candidates != null)
			return candidates.get(0);
		final String typeName = device.getResourceType().getSimpleName();
		switch (typeName) {
		case "GatewayDevice":  
			return device.getSubResource("systemRestart");  // ?
		}
		return null;
	}
	
	private static class AlarmStatus {
		
		AlarmStatus(AlarmConfiguration config, boolean valueViolation, boolean contactViolation) {
			this.config = config;
			this.valueViolation = valueViolation;
			this.contactViolation = contactViolation;
		}
		
		final AlarmConfiguration config;
		final boolean valueViolation;
		final boolean contactViolation;
		
	}
	
	private static class ValueData {
		
		public ValueData(SingleValueResource responsibleResource, String message) {
			this.responsibleResource = responsibleResource;
			this.message = message;
		}
		final SingleValueResource responsibleResource; // may be null
		final String message;
		
	}
	
	
	
}
