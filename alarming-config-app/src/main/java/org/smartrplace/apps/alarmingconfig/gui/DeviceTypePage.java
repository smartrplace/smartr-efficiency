package org.smartrplace.apps.alarmingconfig.gui;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.ogema.accessadmin.api.ApplicationManagerPlus;
import org.ogema.core.application.ApplicationManager;
import org.ogema.devicefinder.api.DatapointGroup;
import org.ogema.devicefinder.util.AlarmingConfigUtil;
import org.ogema.devicefinder.util.DeviceTableRaw;
import org.ogema.internationalization.util.LocaleHelper;
import org.ogema.model.extended.alarming.AlarmConfiguration;
import org.smartrplace.apps.hw.install.config.InstallAppDevice;
import org.smartrplace.gui.filtering.GenericFilterFixedSingle;
import org.smartrplace.gui.filtering.GenericFilterOption;
import org.smartrplace.gui.filtering.SingleFiltering;
import org.smartrplace.gui.filtering.SingleFiltering.OptionSavingMode;
import org.smartrplace.util.directobjectgui.ObjectResourceGUIHelper;

import de.iwes.util.resource.ResourceHelper;
import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.html.StaticTable;
import de.iwes.widgets.api.widgets.localisation.OgemaLocale;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.buttonconfirm.ButtonConfirm;
import de.iwes.widgets.html.complextable.RowTemplate.Row;

@SuppressWarnings("serial")
public class DeviceTypePage extends MainPage {
	protected final boolean showOnlyPrototype;
	protected SingleFiltering<String, AlarmConfiguration> deviceDrop;
	
	public DeviceTypePage(WidgetPage<?> page, ApplicationManagerPlus appManPlus, boolean showOnlyPrototype) {
		super(page, appManPlus);
		this.showOnlyPrototype = showOnlyPrototype;
	}

	protected String getHeader(OgemaLocale locale) {
		return "1. Alarming Configuration Per Device";
	}
	
	@Override
	public void addWidgetsAboveTable() {
		super.addWidgetsAboveTable();
		
		deviceDrop = new SingleFiltering<String, AlarmConfiguration>(
				page, "deviceDrop", OptionSavingMode.GENERAL, 10000, false) {

			@Override
			protected boolean isAttributeSinglePerDestinationObject() {
				return true;
			}
			
			@Override
			public String getAttribute(AlarmConfiguration attr) {
				InstallAppDevice iad = ResourceHelper.getFirstParentOfType(attr, InstallAppDevice.class);
				if(iad == null)
					return null;
				if(showOnlyPrototype && (!DeviceTableRaw.isTemplate(iad, null))) //(!iad.isTemplate().isActive()))
					return null;
				DatapointGroup devTypeGrp = getDeviceTypeGroup(iad);
				if(devTypeGrp != null)
					return devTypeGrp.id();
				return null;
				/*String devLoc = iad.device().getLocation();
				for(DatapointGroup dpGrp: appManPlus.dpService().getAllGroups()) {
					if(dpGrp.getType() != null && dpGrp.getType().equals("DEVICE_TYPE") && (dpGrp.getSubGroup(devLoc) != null)) {
						return dpGrp.id();
					}
				}
				return null;*/
			}
			
			@Override
			protected List<GenericFilterOption<String>> getOptionsDynamic(OgemaHttpRequest req) {
				List<GenericFilterOption<String>> result = new ArrayList<>();
				for(DatapointGroup dpGrp: appManPlus.dpService().getAllGroups()) {
					if(dpGrp.getType() != null && dpGrp.getType().equals("DEVICE_TYPE")) {
						GenericFilterOption<String> option = new GenericFilterFixedSingle<String>(
								dpGrp.id(), LocaleHelper.getLabelMap(dpGrp.label(null)));
						result.add(option);
					}
				}
				return result;
			}
			
			@Override
			protected long getFrameworkTime() {
				return appMan.getFrameworkTime();
			}
		};
		deviceDrop.registerDependentWidget(mainTable);
		
		ButtonConfirm applyTemplateButton = new ButtonConfirm(page, "applyTemplateButton") {
			@Override
			public void onPOSTComplete(String data, OgemaHttpRequest req) {
				applyTemplate(req);
				alert.showAlert("Applied template", true, req);
			}
		};
		applyTemplateButton.setDefaultConfirmMsg("Really apply settings of template"+
								" to all devices of the same type? Note that all settings will be overwritten without further confirmation!");
		applyTemplateButton.setDefaultText("Apply template");
		applyTemplateButton.registerDependentWidget(alert);
		
		ButtonConfirm applyAndCommitButton = new ButtonConfirm(page, "applyAndCommitButton") {
			@Override
			public void onPOSTComplete(String data, OgemaHttpRequest req) {
				applyTemplate(req);
				if(alarmingUpdater != null) {
					alarmingUpdater.updateAlarming();
					alert.showAlert("Updated and restarted alarming", true, req);
				} else
					alert.showAlert("Could not find alarmingManagement for update", false, req);				
			}
		};
		applyAndCommitButton.setDefaultConfirmMsg("Really apply settings of template"+
								" to all devices of the same type? Note that all settings will be overwritten without further confirmation!");
		applyAndCommitButton.setDefaultText("Apply template and Commit (Restart)");
		applyAndCommitButton.registerDependentWidget(alert);

		StaticTable secondTable = new StaticTable(1, 4);
		secondTable.setContent(0, 0, deviceDrop);
		secondTable.setContent(0, 1, applyTemplateButton);
		secondTable.setContent(0, 2, applyAndCommitButton);
		
		page.append(secondTable);

	}

	protected void applyTemplate(OgemaHttpRequest req) {
		List<InstallAppDevice> allDev = new ArrayList<>();
		InstallAppDevice template = null;
		for(InstallAppDevice dev: appMan.getResourceAccess().getResources(InstallAppDevice.class)) {
			DatapointGroup devTypeGrp = getDeviceTypeGroup(dev);
			GenericFilterFixedSingle<String> selected = (GenericFilterFixedSingle<String>) deviceDrop.getSelectedItem(req);
			if(devTypeGrp == null || (!devTypeGrp.id().equals(selected.getValue())))
				continue;
			if(DeviceTableRaw.isTemplate(dev, null)) {
				template = dev;
				continue;
			}
			allDev.add(dev);
		}
		if(template == null)
			return;
		for(InstallAppDevice dev: allDev) {
			AlarmingConfigUtil.copySettings(template, dev, appMan);
		}
		
	}
	
	@Override
	public Collection<AlarmConfiguration> getObjectsInTable(OgemaHttpRequest req) {
		Collection<AlarmConfiguration> all = super.getObjectsInTable(req);
		return deviceDrop.getFiltered(all, req);
	}
	
	@Override
	protected void addWidgetsBeforeMultiSelect(AlarmConfiguration sr,
			ObjectResourceGUIHelper<AlarmConfiguration, AlarmConfiguration> vh, String id, OgemaHttpRequest req,
			Row row, ApplicationManager appMan) {
		// TODO Auto-generated method stub
		super.addWidgetsBeforeMultiSelect(sr, vh, id, req, row, appMan);
	}
	
	public DatapointGroup getDeviceTypeGroup(InstallAppDevice iad) {
		String devLoc = iad.device().getLocation();
		for(DatapointGroup dpGrp: appManPlus.dpService().getAllGroups()) {
			if(dpGrp.getType() != null && dpGrp.getType().equals("DEVICE_TYPE") && (dpGrp.getSubGroup(devLoc) != null)) {
				return dpGrp;
			}
		}
		return null;
		
	}
}
