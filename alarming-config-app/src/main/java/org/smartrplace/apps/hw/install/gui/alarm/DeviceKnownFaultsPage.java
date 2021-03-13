package org.smartrplace.apps.hw.install.gui.alarm;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.ogema.core.administration.UserAccount;
import org.ogema.core.application.ApplicationManager;
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
import org.smartrplace.util.directobjectgui.ObjectResourceGUIHelper;
import org.smartrplace.util.format.WidgetHelper;

import de.iwes.util.resource.ValueResourceHelper;
import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.alert.Alert;
import de.iwes.widgets.html.complextable.RowTemplate.Row;
import de.iwes.widgets.html.form.button.RedirectButton;
import de.iwes.widgets.html.form.dropdown.TemplateDropdown;
import de.iwes.widgets.html.form.textfield.TextField;

@SuppressWarnings("serial")
public class DeviceKnownFaultsPage extends DeviceAlarmingPage {

	@Override
	protected String getHeader() {
		return "6. Device Issue Status";
	}
	
	public DeviceKnownFaultsPage(WidgetPage<?> page, AlarmingConfigAppController controller) {
		super(page, controller);
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
		return new AlarmingDeviceTableBase(page, appManPlus, alert, pageTitle, resData, commitButton, appSelector, pe) {
			protected void addAdditionalWidgets(final InstallAppDevice object, ObjectResourceGUIHelper<InstallAppDevice,InstallAppDevice> vh, String id,
					OgemaHttpRequest req, Row row, final ApplicationManager appMan,
					PhysicalElement device, final InstallAppDevice template) {
				if(req == null) {
					vh.registerHeaderEntry("Finished");
					vh.registerHeaderEntry("Started");
					vh.registerHeaderEntry("Comment");
					vh.registerHeaderEntry("Assigned");
					vh.registerHeaderEntry("Block");
					vh.registerHeaderEntry("MinInterval");
					vh.registerHeaderEntry("Task Tracking");
					vh.registerHeaderEntry("Edit TT");
					return;
				}
				AlarmGroupData res = object.knownFault();
				res.create();
				vh.stringLabel("Finished", id, ""+res.isFinished().getValue(), row);
				vh.timeLabel("Started", id, res.ongoingAlarmStartTime(), row, 0);
				vh.stringEdit("Comment",  id, res.comment(), row, alert);
				Map<String, String> valuesToSet = new LinkedHashMap<>();
				String curVal = res.acceptedByUser().getValue();
				if(curVal != null && (!curVal.isEmpty()))
					valuesToSet.put(curVal, curVal);
				valuesToSet.put("None", "None");
				for(UserAccount user: appMan.getAdministrationManager().getAllUsers()) {
					if(curVal != null && user.getName().equals(curVal))
						continue;
					valuesToSet.put(user.getName(), user.getName());
				}
				vh.dropdown("Assigned", id, res.acceptedByUser(), row, valuesToSet);
				if(!res.linkToTaskTracking().getValue().isEmpty()) {
					RedirectButton taskLink = new RedirectButton(mainTable, "taskLink"+id, "Task Tracking",
							res.linkToTaskTracking().getValue(), req);
					row.addCell(WidgetHelper.getValidWidgetId("Task Tracking"), taskLink);
				}
				vh.stringEdit("Edit TT",  id, res.linkToTaskTracking(), row, alert);
				
				TextField intervalEdit = vh.floatEdit(res.minimumTimeBetweenAlarms(), alert, -1, Float.MAX_VALUE, "Minimum value allowed is -1");

				TemplateDropdown<String> blockingDrop = new TemplateDropdown<String>(mainTable, "blockingDrop"+id, req) {
					@Override
					public void onGET(OgemaHttpRequest req) {
						float val = res.minimumTimeBetweenAlarms().getValue();
						if(val < 0)
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
				row.addCell(WidgetHelper.getValidWidgetId("Block"), blockingDrop);
			}
			
			@Override
			public List<InstallAppDevice> getObjectsInTable(OgemaHttpRequest req) {
				List<InstallAppDevice> all = super.getObjectsInTable(req);
				return getDevicesWithKnownFault(all);
			}
		};	
	}
	
	protected List<InstallAppDevice> getDevicesWithKnownFault(List<InstallAppDevice> all) {
		List<InstallAppDevice> result = new ArrayList<>();
		for(InstallAppDevice dev: all) {
			if(dev.knownFault().exists() && dev.knownFault().ongoingAlarmStartTime().getValue() > 0) {
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
