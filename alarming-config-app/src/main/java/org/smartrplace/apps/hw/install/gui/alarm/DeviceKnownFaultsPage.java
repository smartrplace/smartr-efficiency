package org.smartrplace.apps.hw.install.gui.alarm;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.ogema.core.application.ApplicationManager;
import org.ogema.core.model.simple.IntegerResource;
import org.ogema.devicefinder.api.DatapointGroup;
import org.ogema.devicefinder.api.DeviceHandlerProvider;
import org.ogema.devicefinder.api.InstalledAppsSelector;
import org.ogema.devicefinder.util.AlarmingConfigUtil;
import org.ogema.devicefinder.util.DeviceTableBase;
import org.ogema.devicefinder.util.DpGroupUtil;
import org.ogema.model.extended.alarming.AlarmGroupData;
import org.ogema.model.prototypes.PhysicalElement;
import org.smartrplace.apps.alarmingconfig.AlarmingConfigAppController;
import org.smartrplace.apps.hw.install.config.InstallAppDevice;
import org.smartrplace.apps.hw.install.gui.MainPage;
import org.smartrplace.apps.hw.install.gui.MainPage.GetPlotButtonResult;
import org.smartrplace.util.directobjectgui.ObjectDetailPopupButton;
import org.smartrplace.util.directobjectgui.ObjectGUIHelperBase.ValueResourceDropdownFlex;
import org.smartrplace.util.directobjectgui.ObjectResourceGUIHelper;
import org.smartrplace.util.format.WidgetHelper;

import de.iwes.util.resource.ValueResourceHelper;
import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.alert.Alert;
import de.iwes.widgets.html.buttonconfirm.ButtonConfirm;
import de.iwes.widgets.html.complextable.RowTemplate.Row;
import de.iwes.widgets.html.form.button.Button;
import de.iwes.widgets.html.form.button.RedirectButton;

@SuppressWarnings("serial")
public class DeviceKnownFaultsPage extends DeviceAlarmingPage {
	//@Deprecated //Currently not used, but still available in the details section
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
		return "3. Device Issue Status";
	}
	
	public DeviceKnownFaultsPage(WidgetPage<?> page, AlarmingConfigAppController controller) {
		super(page, controller);
		
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
						for(InstallAppDevice iad: allforPe) {
							AlarmGroupData res = iad.knownFault();
							if((!res.assigned().isActive()) || (res.assigned().getValue() <= 0)) {
								res.delete();
							}
						}
					}
				}
			}
		};
		releaseAllUnassigned.setDefaultText("Relase all Unassigned");
		releaseAllUnassigned.setDefaultConfirmMsg("Really release all known issues that are not assigned?");
		topTable.setContent(1, 6, releaseAllUnassigned);
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
			installFilterDrop.registerDependentWidget(tableLoc.getMainTable());
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
		final AlarmingDeviceTableBase result = new AlarmingDeviceTableBase(page, appManPlus, alert, pageTitle, resData, commitButton, appSelector, pe) {
			protected void addAdditionalWidgets(final InstallAppDevice object, ObjectResourceGUIHelper<InstallAppDevice,InstallAppDevice> vh, String id,
					OgemaHttpRequest req, Row row, final ApplicationManager appMan,
					PhysicalElement device, final InstallAppDevice template) {
				if(req == null) {
					vh.registerHeaderEntry("Finished");
					vh.registerHeaderEntry("Started");
					vh.registerHeaderEntry("Comment");
					vh.registerHeaderEntry("Assigned");
					//vh.registerHeaderEntry("Block");
					//vh.registerHeaderEntry("MinInterval");
					vh.registerHeaderEntry("Task Tracking");
					vh.registerHeaderEntry("Edit TT");
					vh.registerHeaderEntry("Plot");
					vh.registerHeaderEntry("Release");
					vh.registerHeaderEntry("Details");
					vh.inDetailSection(true);
					vh.registerHeaderEntry("Diagnosis");
					return;
				}
				AlarmGroupData res = object.knownFault();
				res.create();
				if(row == null) {
					//TODO: There is still a bug in the detail popup support so that for each table the popup is not adapted when
					//another detail button is clicked until the page is reloaded.
					//Another issue: only widgets generated via the vh helper can be added to the popup, no widgets that otherwise
					//would be added directly to the row. This should be possible by calling
					//popTableData.add(new WidgetEntryData(widgetId, newWidget));
					vh.dropdown("Diagnosis",  id, res.diagnosis(), row, dignosisVals);
					return;
				}
				vh.stringLabel("Finished", id, ""+res.isFinished().getValue(), row);
				vh.timeLabel("Started", id, res.ongoingAlarmStartTime(), row, 0);
				vh.stringEdit("Comment",  id, res.comment(), row, alert);
				/*Map<String, String> valuesToSet = new LinkedHashMap<>();
				String curVal = res.acceptedByUser().getValue();
				if(curVal != null && (!curVal.isEmpty()))
					valuesToSet.put(curVal, curVal);
				valuesToSet.put("None", "None");
				for(String role: AlarmGroupData.USER_ROLES) {
					if(curVal != null && role.equals(curVal))
						continue;
					valuesToSet.put(role, role);					
				}
				vh.dropdown("Assigned", id, res.acceptedByUser(), row, valuesToSet);*/
				//vh.dropdown("Assigned", id, res.assigned(), row, AlarmingConfigUtil.ASSIGNEMENT_ROLES);
				ValueResourceDropdownFlex<IntegerResource> widgetPlus = new ValueResourceDropdownFlex<IntegerResource>(
						"Assigned"+id, vh, AlarmingConfigUtil.ASSIGNEMENT_ROLES) {
					public void onGET(OgemaHttpRequest req) {
						myDrop.selectItem(res.assigned(), req);
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
				vh.stringEdit("Edit TT",  id, res.linkToTaskTracking(), row, alert);
				
				vh.floatEdit(res.minimumTimeBetweenAlarms(), alert, -1, Float.MAX_VALUE, "Minimum value allowed is -1");
				//TextField intervalEdit = vh.floatEdit(res.minimumTimeBetweenAlarms(), alert, -1, Float.MAX_VALUE, "Minimum value allowed is -1");

				/*TemplateDropdown<String> blockingDrop = new TemplateDropdown<String>(mainTable, "blockingDrop"+id, req) {
					@Override
					public void onGET(OgemaHttpRequest req) {
						float val = res.minimumTimeBetweenAlarms().getValue();
						if(Float.isNaN(val)) {
							setAddEmptyOption(true, "not set", req);
							selectItem(null, req);
						} else if(val < 0)
							selectItem("Blocking", req);
						else if(val == 0)
							selectItem("No-Block", req);
						else
							selectItem("Retard", req);
					}
					@Override
					public void onPOSTComplete(String data, OgemaHttpRequest req) {
						String val = getSelectedItem(req);
						if(val.equals("Blocking")) {
							ValueResourceHelper.setCreate(res.minimumTimeBetweenAlarms(), -1);
							intervalEdit.disable(req);
						} else if(val.equals("No-Block")) {
							ValueResourceHelper.setCreate(res.minimumTimeBetweenAlarms(), 0);
							intervalEdit.disable(req);
						} else {
							if(res.minimumTimeBetweenAlarms().getValue() <= 0)
								ValueResourceHelper.setCreate(res.minimumTimeBetweenAlarms(), 14*1440);
							intervalEdit.enable(req);
						}
					}
				};
				blockingDrop.setDefaultItems(valuesToSetBlock.values());
				blockingDrop.registerDependentWidget(intervalEdit);
				row.addCell(WidgetHelper.getValidWidgetId("Block"), blockingDrop);*/
				
				Button releaseBut;
				if(res.assigned().isActive() &&
						(res.assigned().getValue() > 0)) {
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
				} else {
					releaseBut = new Button(mainTable, "releaseBut"+id, "Release", req) {
						@Override
						public void onPOSTComplete(String data, OgemaHttpRequest req) {
							//TODO: In the future we may want to keep this information in a log of solved issues
							res.delete();
							//res.ongoingAlarmStartTime().setValue(-1);
						}
					};
				}
				row.addCell("Release", releaseBut);
				
				final GetPlotButtonResult logResult = MainPage.getPlotButton(id, object, appManPlus.dpService(), appMan, false, vh, row, req, pe,
						ScheduleViewerConfigProvAlarm.getInstance(), null);
				row.addCell("Plot", logResult.plotButton);
				
				ObjectDetailPopupButton<InstallAppDevice, InstallAppDevice> detailBut = new ObjectDetailPopupButton<InstallAppDevice, InstallAppDevice>(mainTable, "detailBut"+id, "Details", req, popMore1,
						object, appMan, pid(), knownWidgets, this);
				row.addCell("Details", detailBut);
				vh.dropdown("Diagnosis",  id, res.diagnosis(), row, dignosisVals);				
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
				int[] actAlarms = AlarmingConfigUtil.getActiveAlarms(dev);
				if(actAlarms[1] > 0)
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
}
