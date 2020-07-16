package org.smartrplace.apps.hw.install.gui;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.ogema.core.application.ApplicationManager;
import org.ogema.core.model.Resource;
import org.ogema.devicefinder.api.DeviceHandlerProvider;
import org.ogema.devicefinder.api.DriverHandlerProvider;
import org.ogema.devicefinder.api.DriverHandlerProvider.DriverDeviceConfig;
import org.ogema.devicefinder.api.InstalledAppsSelector;
import org.ogema.devicefinder.util.DeviceTableRaw;
import org.smartrplace.apps.hw.install.HardwareInstallController;
import org.smartrplace.apps.hw.install.config.InstallAppDevice;
import org.smartrplace.util.directobjectgui.ObjectResourceGUIHelper;

import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.complextable.RowTemplate.Row;

/** The page inherits ObjectGUITablePage, but does not trigger its own build as it does not use its mainTable*/
public class DeviceConfigPage extends DeviceTablePageFragmentRaw<InstallAppDevice,InstallAppDevice> implements InstalledAppsSelector { //extends DeviceTablePageFragment implements InstalledAppsSelector {
	//protected final HardwareInstallController controller;
	//protected final WidgetPage<?> page;
	
	public DeviceConfigPage(WidgetPage<?> page, HardwareInstallController controller) {
		super(page, controller, true, null);
		//this.page = page;
		//this.controller = controller;
		
		//build page: As we do not call triggerBuild we have to do these steps here
		addWidgetsAboveTable();
		if(isAlertNew) page.append(alert);

		updateTables();

		//More tables may come in later, so we should not put anything at the end of the page
}
	
	Set<String> providersDone = new HashSet<>();
	public void updateTables() {
		synchronized(providersDone) {
		if(controller.hwInstApp != null) for(DriverHandlerProvider pe: controller.hwInstApp.getDriverProviders().values()) {
			String id = pe.id();
			if(providersDone.contains(id))
				continue;
			providersDone.add(id);
			DeviceTableRaw<DriverHandlerProvider, Resource> initTable = pe.getDriverInitTable(page, alert);
			if(initTable != null)
				initTable.triggerPageBuild();
			DeviceTableRaw<DriverDeviceConfig, InstallAppDevice> perDeviceTable = pe.getDriverPerDeviceConfigurationTable(page, alert, this, true);
			if(perDeviceTable != null)
				perDeviceTable.triggerPageBuild();
		}
		}
	}
	
	@Override
		protected String getHeader() {
			return "Hardware Driver Configuration";
		}
	@Override
	public List<InstallAppDevice> getDevicesSelected() {
		List<InstallAppDevice> all = roomsDrop.getDevicesSelected();
		if (installFilterDrop != null)  // FIXME seems to always be null here
			all = installFilterDrop.getDevicesSelected(all);
		return all;
	}

	@Override
	public InstallAppDevice getInstallResource(Resource device) {
		for(InstallAppDevice dev: controller.appConfigData.knownDevices().getAllElements()) {
			if(dev.device().equalsLocation(device))
				return dev;
		}
		return null;
	}
	
	@Override
	public <T extends Resource> InstallAppDevice addDeviceIfNew(T model, DeviceHandlerProvider<T> tableProvider) {
		throw new UnsupportedOperationException("addDevice not relevant for DriverProviders!");
	}

	@Override
	public <T extends Resource> InstallAppDevice removeDevice(T model) {
		throw new UnsupportedOperationException("removeDevice not relevant for DriverProviders!");	}

	@Override
	public void addWidgets(InstallAppDevice object, ObjectResourceGUIHelper<InstallAppDevice, InstallAppDevice> vh,
			String id, OgemaHttpRequest req, Row row, ApplicationManager appMan) {
		throw new UnsupportedOperationException("addWidgets not relevant for DeviceConfigPage!");
	}

	@Override
	public <T extends Resource> void startSimulation(DeviceHandlerProvider<T> tableProvider, T device) {
		throw new UnsupportedOperationException("Start simulation not relevant for DriverProviders!");
		
	}

	@Override
	protected Class<? extends Resource> getResourceType() {
		throw new UnsupportedOperationException("getResourceType not relevant for DeviceConfigPage!");
	}

	@Override
	public Collection<InstallAppDevice> getObjectsInTable(OgemaHttpRequest req) {
		throw new UnsupportedOperationException("getObjectsInTable not relevant for DeviceConfigPage!");
	}
}


