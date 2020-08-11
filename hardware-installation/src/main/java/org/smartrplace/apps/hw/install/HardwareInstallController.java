/**
 * ﻿Copyright 2018 Smartrplace UG
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.smartrplace.apps.hw.install;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.ogema.accessadmin.api.ApplicationManagerPlus;
import org.ogema.core.application.ApplicationManager;
import org.ogema.core.logging.OgemaLogger;
import org.ogema.core.model.Resource;
import org.ogema.core.model.ResourceList;
import org.ogema.core.model.simple.SingleValueResource;
import org.ogema.core.recordeddata.RecordedData;
import org.ogema.core.timeseries.ReadOnlyTimeSeries;
import org.ogema.devicefinder.api.Datapoint;
import org.ogema.devicefinder.api.DatapointGroup;
import org.ogema.devicefinder.api.DatapointService;
import org.ogema.devicefinder.api.DeviceHandlerProvider;
import org.ogema.devicefinder.util.DeviceTableRaw;
import org.ogema.model.gateway.remotesupervision.DataLogTransferInfo;
import org.ogema.tools.resource.util.LoggingUtils;
import org.smartrplace.apps.hw.install.config.HardwareInstallConfig;
import org.smartrplace.apps.hw.install.config.InstallAppDevice;
import org.smartrplace.apps.hw.install.gui.DeviceConfigPage;
import org.smartrplace.apps.hw.install.gui.MainPage;
import org.smartrplace.apps.hw.install.gui.RoomSelectorDropdown;
import org.smartrplace.tissue.util.logconfig.LogTransferUtil;

import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.localisation.LocaleDictionary;

// here the controller logic is implemented
public class HardwareInstallController {

	public final OgemaLogger log;
    public final ApplicationManager appMan;
    public final ApplicationManagerPlus appManPlus;
    //private final ResourcePatternAccess advAcc;
    public final DatapointService dpService;

	public HardwareInstallConfig appConfigData;
	public final HardwareInstallApp hwInstApp;
	
	/** Location of InstallAppDevice -> DeviceHandlerProvider*/
	public final Map<String, DeviceHandlerProvider<?>> handlerByDevice = new HashMap<>();
	
	public MainPage mainPage;
	public List<MainPage> mainPageExts = new ArrayList<>();
	public DeviceConfigPage deviceConfigPage;
	public ResourceList<DataLogTransferInfo> datalogs;
	//WidgetApp widgetApp;

	public HardwareInstallController(ApplicationManager appMan, WidgetPage<?> page, HardwareInstallApp hardwareInstallApp,
			DatapointService dpService) {
		this.appMan = appMan;
		this.log = appMan.getLogger();
		//this.advAcc = appMan.getResourcePatternAccess();
		this.hwInstApp = hardwareInstallApp;		
		this.dpService = dpService;
		this.datalogs = LogTransferUtil.getDataLogTransferInfo(appMan);
		this.appManPlus = new ApplicationManagerPlus(appMan);
		this.appManPlus.setDpService(dpService);
		
		initConfigurationResource();
		cleanupOnStart();
		mainPage = getMainPage(page);
        initDemands();
		if(hardwareInstallApp == null)
			return;
		hardwareInstallApp.menu.addEntry("Device Setup and Configuration", page);
		hardwareInstallApp.configMenuConfig(page.getMenuConfiguration());
		
		WidgetPage<LocaleDictionary> page2 = hardwareInstallApp.widgetApp.createWidgetPage("deviceConfig.html");
		deviceConfigPage = new DeviceConfigPage(page2, this);
		hardwareInstallApp.menu.addEntry("Hardware Driver Configuration", page2);
		hardwareInstallApp.configMenuConfig(page2.getMenuConfiguration());
	}

	protected MainPage getMainPage(WidgetPage<?> page) {
		return new MainPage(page, this);
	}
	
    /*
     * This app uses a central configuration resource, which is accessed here
     */
    private void initConfigurationResource() {
		String name = HardwareInstallConfig.class.getSimpleName().substring(0, 1).toLowerCase()+HardwareInstallConfig.class.getSimpleName().substring(1);
		appConfigData = appMan.getResourceAccess().getResource(name);
		if (appConfigData != null) { // resource already exists (appears in case of non-clean start)
			appMan.getLogger().debug("{} started with previously-existing config resource", getClass().getName());
		}
		else {
			appConfigData = (HardwareInstallConfig) appMan.getResourceManagement().createResource(name, HardwareInstallConfig.class);
			appConfigData.isInstallationActive().create();
			appConfigData.knownDevices().create();
			appConfigData.room().create();
			appConfigData.room().setValue(RoomSelectorDropdown.ALL_DEVICES_ID);
			//appConfigData.installationStatusFilter().create();
			//appConfigData.installationStatusFilter().setValue(InstallationStatusFilterDropdown.FILTERS.ALL.name);
			appConfigData.activate(true);
			appMan.getLogger().debug("{} started with new config resource", getClass().getName());
		}
    }
    
    /*
     * register ResourcePatternDemands. The listeners will be informed about new and disappearing
     * patterns in the OGEMA resource tree
     */
    public void initDemands() {
		if(appConfigData.isInstallationActive().getValue()) {
			startDemands();
		}
    }
    public void checkDemands() {
    	if(appConfigData.isInstallationActive().getValue())
     		startDemands();
    	else
    		closeDemands();
    }
    
    public boolean demandsActivated = false;
    public void startDemands() {
		demandsActivated = true;
		if(hwInstApp != null) {
			for(DeviceHandlerProvider<?> devhand: hwInstApp.getTableProviders().values()) {
				devhand.addPatternDemand(mainPage);
			}
		}
    }

	public void closeDemands() {
		if(!demandsActivated) return;
		demandsActivated = false;
		if(hwInstApp != null) {
			for(DeviceHandlerProvider<?> devhand: hwInstApp.getTableProviders().values()) {
				devhand.removePatternDemand();
			}
		}
    }
	public void close() {
		closeDemands();
	}
		
	/*
	 * if the app needs to consider dependencies between different pattern types,
	 * they can be processed here.
	 */
	public <T extends Resource> InstallAppDevice addDeviceIfNew(T device, DeviceHandlerProvider<T> tableProvider) {
		for(InstallAppDevice install: appConfigData.knownDevices().getAllElements()) {
			if(install.device().equalsLocation(device)) {
				initializeDevice(install, device, tableProvider); // only initialize missing resources
				return install;
			}
		}
		InstallAppDevice install = appConfigData.knownDevices().add();
		initializeDevice(install, device, tableProvider);
		if(tableProvider != null)
			startSimulation(tableProvider, install);
		updateDatapoints(tableProvider, install);
		if(appConfigData.autoLoggingActivation().getValue() == 1) {
			activateLogging(tableProvider, install, true, false);
		}
		return install;
	}
	public InstallAppDevice removeDevice(Resource device) {
		//TODO
		return null;
	}
	
	private <T extends Resource> void initializeDevice(InstallAppDevice install, T device,
			DeviceHandlerProvider<T> tableProvider) {

		if (!install.exists()) install.create();
		if (device != null && !install.device().exists()) install.device().setAsReference(device);
		if (!install.installationStatus().exists()) install.installationStatus().create();
		if (!install.deviceId().exists()) install.deviceId().create();
		if (install.deviceId().getValue().isEmpty())
			install.deviceId().setValue(LocalDeviceId.generateDeviceId(install, appConfigData, tableProvider,
					dpService));
		install.activate(true);
	}
	
	public <T extends Resource> void startSimulations(DeviceHandlerProvider<T> tableProvider) {
		Class<?> tableType = tableProvider.getResourceType();
		for(InstallAppDevice install: appConfigData.knownDevices().getAllElements()) {
			if(install.device().getResourceType().equals(tableType)) {
				startSimulation(tableProvider, install);
			}
		}
	}
	public <T extends Resource> void startSimulation(DeviceHandlerProvider<T> tableProvider, T device) {
		for(InstallAppDevice install: appConfigData.knownDevices().getAllElements()) {
			if(install.device().equalsLocation(device)) {
				startSimulation(tableProvider, install, device);
				break;
			}
		}
	}
	protected Map<String, Set<String>> simulationsStarted = new HashMap<>();
	public <T extends Resource> void startSimulation(DeviceHandlerProvider<T> tableProvider, InstallAppDevice appDevice) {
		@SuppressWarnings("unchecked")
		T device = (T) appDevice.device();
		startSimulation(tableProvider, appDevice, device);
	}
	
	@SuppressWarnings("unchecked")
	public <T extends Resource> void startSimulation(DeviceHandlerProvider<T> tableProvider, InstallAppDevice appDevice,
			T device) {
		handlerByDevice.put(appDevice.getLocation(), tableProvider);
		//if(Boolean.getBoolean("org.smartrplace.apps.hw.install.autologging")) {
		updateDatapoints(tableProvider, appDevice);
		if(appConfigData.autoLoggingActivation().getValue() == 2) {
			activateLogging(tableProvider, appDevice, true, false);
		}
		if(!Boolean.getBoolean("org.ogema.devicefinder.api.simulateRemoteGateway"))
			return;
		Set<String> deviceSimsStarted = simulationsStarted.get(tableProvider.id());
		if(deviceSimsStarted == null) {
			deviceSimsStarted = new HashSet<>();
			simulationsStarted.put(tableProvider.id(), deviceSimsStarted);
		}
		if(deviceSimsStarted.contains(device.getLocation()))
			return;
		deviceSimsStarted.add(device.getLocation());
		tableProvider.startSimulationForDevice((T) device.getLocationResource(),
				mainPage.getRoomSimulation(device), dpService);
	}
	
	public <T extends Resource> void updateDatapoints(DeviceHandlerProvider<T> tableProvider, InstallAppDevice appDevice) {
		DatapointGroup dev = dpService.getGroup(appDevice.device().getLocation());
		String devName = DeviceTableRaw.getName(appDevice, appManPlus);
		dev.setLabel(null, devName);
		dev.setType("DEVICE");
		for(Datapoint dp: tableProvider.getDatapoints(appDevice, dpService)) {
			dev.addDatapoint(dp);
			dp.setDeviceResource(appDevice.device().getLocationResource());
		}
	}
	public <T extends Resource> void activateLogging(DeviceHandlerProvider<T> tableProvider, InstallAppDevice appDevice,
			boolean onylDefaultLoggingDps, boolean disable) {
		for(Datapoint dp: tableProvider.getDatapoints(appDevice, dpService)) {
			if((!tableProvider.relevantForDefaultLogging(dp)) && onylDefaultLoggingDps)
				continue;
			ReadOnlyTimeSeries ts = dp.getTimeSeries();
			if(ts == null || (!(ts instanceof RecordedData)))
				continue;
			RecordedData rec = (RecordedData)ts;
			if(Boolean.getBoolean("org.smartrplace.app.srcmon.isgateway") &&
					(appConfigData.autoTransferActivation().getValue() || disable)) {
				Resource res = appMan.getResourceAccess().getResource(rec.getPath());
				if(res != null && res instanceof SingleValueResource) {
					if(disable)
						LogTransferUtil.stopTransmitLogData((SingleValueResource) res, datalogs);
					else
						LogTransferUtil.startTransmitLogData((SingleValueResource) res, datalogs);
				}
			}
			if(disable)
				LoggingUtils.deactivateLogging(rec);
			else
				LoggingUtils.activateLogging(rec, -2);
		}		
	}
	
	public void cleanupOnStart() {
		List<String> knownDevLocs = new ArrayList<>();
		for(InstallAppDevice install: appConfigData.knownDevices().getAllElements()) {
			if(knownDevLocs.contains(install.device().getLocation()))
				install.delete();
			else
				knownDevLocs.add(install.device().getLocation());
		}
		
	}
}
