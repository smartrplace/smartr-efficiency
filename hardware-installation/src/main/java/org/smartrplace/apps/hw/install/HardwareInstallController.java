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
import org.ogema.core.model.simple.IntegerResource;
import org.ogema.core.model.simple.SingleValueResource;
import org.ogema.core.recordeddata.RecordedData;
import org.ogema.core.timeseries.ReadOnlyTimeSeries;
import org.ogema.devicefinder.api.DPRoom;
import org.ogema.devicefinder.api.Datapoint;
import org.ogema.devicefinder.api.DatapointGroup;
import org.ogema.devicefinder.api.DatapointService;
import org.ogema.devicefinder.api.DeviceHandlerProvider;
import org.ogema.devicefinder.api.OGEMADriverPropertyService;
import org.ogema.devicefinder.util.AlarmingConfigUtil;
import org.ogema.devicefinder.util.DatapointImpl;
import org.ogema.devicefinder.util.DeviceTableRaw;
import org.ogema.eval.timeseries.simple.smarteff.AlarmingUtiH;
import org.ogema.model.gateway.remotesupervision.DataLogTransferInfo;
import org.ogema.model.locations.Room;
import org.ogema.simulation.shared.api.RoomInsideSimulationBase;
import org.ogema.tools.resource.util.LoggingUtils;
import org.smartrplace.apps.hw.install.config.HardwareInstallConfig;
import org.smartrplace.apps.hw.install.config.InstallAppDevice;
import org.smartrplace.apps.hw.install.devicetypes.InitialConfig;
import org.smartrplace.apps.hw.install.gui.DeviceConfigPage;
import org.smartrplace.apps.hw.install.gui.MainPage;
import org.smartrplace.apps.hw.install.gui.RoomSelectorDropdown;
import org.smartrplace.apps.hw.install.prop.DriverPropertyUtils;
import org.smartrplace.external.accessadmin.config.AccessAdminConfig;
import org.smartrplace.tissue.util.logconfig.LogTransferUtil;

import de.iwes.util.resource.ValueResourceHelper;
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
	public final AccessAdminConfig accessAdminConfigRes;
	public final HardwareInstallApp hwInstApp;
	
	/** Location of InstallAppDevice -> DeviceHandlerProvider*/
	public final Map<String, DeviceHandlerProvider<?>> handlerByDevice = new HashMap<>();
	/** Location of InstallAppDevice -> Device simulations*/
	public final Map<String, List<RoomInsideSimulationBase>> simByDevice = new HashMap<>();
	
	public MainPage mainPage;
	public List<MainPage> mainPageExts = new ArrayList<>();
	public DeviceConfigPage deviceConfigPage;
	public ResourceList<DataLogTransferInfo> datalogs;
	//WidgetApp widgetApp;
	public volatile boolean cleanUpOnStartDone = false;
	
	
	//OGEMADriverPropertyService administration
	// Resource location -> services that have added at least one property to the resource
	public Map<String, Set<OGEMADriverPropertyService<?>>> knownResources = new HashMap<>();
	// Service -> Resource locations for which the service added at least one property to the resource
	public Map<OGEMADriverPropertyService<?>, Set<String>> usedServices = new HashMap<>();
	public <R extends Resource> void addPropServiceEntry(R res, OGEMADriverPropertyService<R> object) {
		Set<OGEMADriverPropertyService<?>> services = knownResources.get(res.getLocation());
		if(services == null) {
			services = new HashSet<>();
			knownResources.put(res.getLocation(), services);
		}
		services.add(object);
		Set<String> ress = usedServices.get(object);
		if(ress == null) {
			ress = new HashSet<>();
			usedServices.put(object, ress);
		}
		ress.add(res.getLocation());
	}
	public <T extends Resource> void initKnownResources(OGEMADriverPropertyService<T> dPropService) {
		List<T> ress = DriverPropertyUtils.getResourcesWithProperties(appMan, dPropService.getDataPointResourceType());

		Set<String> existing = usedServices.get(dPropService);
		if(existing == null) {
			existing = new HashSet<>();
			usedServices.put(dPropService, existing);
		}
		for(T res: ress) {
			Set<OGEMADriverPropertyService<?>> services = knownResources.get(res.getLocation());
			if(services == null) {
				services = new HashSet<>();
				knownResources.put(res.getLocation(), services);
			}
			services.add(dPropService);

			existing.add(res.getLocation());
		}
		
	}
	
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
		
		this.accessAdminConfigRes = appMan.getResourceAccess().getResource("accessAdminConfig");
				
		initConfigurationResource();
		cleanupOnStart();
		cleanUpOnStartDone = true;
		mainPage = getMainPage(page);
		initConfigResourceForOperation();
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
				if(Boolean.getBoolean("org.smartrplace.apps.hw.install.existing.configalarming")) {
					initAlarmingForDevice(install, tableProvider);
				}
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
		if(appConfigData.autoConfigureNewDevicesBasedOnTemplate().getValue()) {
			InstallAppDevice currentTemplate = getTemplateDevice(tableProvider);
			if(currentTemplate != null && (!currentTemplate.equalsLocation(install)))
				AlarmingConfigUtil.copySettings(currentTemplate, install, appMan);
		}
		if(Boolean.getBoolean("org.smartrplace.apps.hw.install.init.startalarming")) {
			initAlarmingForDevice(install, tableProvider);
		}
		return install;
	}
	
	protected <T extends Resource> void initAlarmingForDevice(InstallAppDevice install, DeviceHandlerProvider<T> tableProvider) {
		String shortID = tableProvider.getDeviceTypeShortId(install, dpService);
		if((!InitialConfig.isInitDone(shortID, appConfigData.initDoneStatus())) &&
				(getDevices(tableProvider).size() <= 1)) {
			tableProvider.initAlarmingForDevice(install, appConfigData);
			ValueResourceHelper.setCreate(install.isTemplate(), tableProvider.id());
		}
		//mark init done for sure
		if(!InitialConfig.isInitDone(shortID, appConfigData.initDoneStatus()))
			InitialConfig.addString(shortID, appConfigData.initDoneStatus());			
	}
	
	public InstallAppDevice removeDevice(Resource device) {
		for(InstallAppDevice install: appConfigData.knownDevices().getAllElements()) {
			if(install.device().equalsLocation(device)) {
				List<RoomInsideSimulationBase> sims = simByDevice.get(install.getLocation());
				if(sims != null) {
					for(RoomInsideSimulationBase sim: sims)
						sim.close();
					simByDevice.remove(install.getLocation());
				}
				return install;
			}
		}
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
		if(!install.devHandlerInfo().exists()) {
			install.devHandlerInfo().create();
			install.devHandlerInfo().setValue(tableProvider.id());
		}
		install.activate(true);
	}
	
	public <T extends Resource> void startSimulations(DeviceHandlerProvider<T> tableProvider) {
		//Class<?> tableType = tableProvider.getResourceType();
		for(InstallAppDevice install: appConfigData.knownDevices().getAllElements()) {
			if(!tableProvider.id().equals(install.devHandlerInfo().getValue()))
				continue;
			if(install.isTrash().getValue())
				continue;
			startSimulation(tableProvider, install);
			/*for(ResourcePattern<?> pat: tableProvider.getAllPatterns()) {
				if(pat.model.equalsLocation(install.device())) {
					startSimulation(tableProvider, install);
					break;
				}
			}*/
			//if(install.device().getResourceType().equals(tableType)) {
			//	startSimulation(tableProvider, install);
			//}
	       	if(!Boolean.getBoolean("org.smartrplace.app.srcmon.isgateway")) {
	    		DatapointGroup devType = dpService.getGroup(tableProvider.id());
	    		devType.setLabel(null, tableProvider.label(null));
	    		devType.setType("DEVICE_TYPE");
	    		String devTypeShort = tableProvider.getDeviceTypeShortId(null, dpService);
	    		devType.setParameter(DatapointGroup.DEVICE_TYPE_SHORT_PARAM, devTypeShort);
	    		//devType.setParameter("PROVIDER", provider);
	    	}

		}
	}
	public <T extends Resource> void startSimulation(DeviceHandlerProvider<T> tableProvider, T device) {
		for(InstallAppDevice install: appConfigData.knownDevices().getAllElements()) {
			if(install.isTrash().getValue())
				continue;
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
		Set<String> deviceSimsStarted = simulationsStarted.get(tableProvider.id());
		if(deviceSimsStarted == null) {
			deviceSimsStarted = new HashSet<>();
			simulationsStarted.put(tableProvider.id(), deviceSimsStarted);
		}
		if(deviceSimsStarted.contains(device.getLocation()))
			return;
		deviceSimsStarted.add(device.getLocation());
		List<RoomInsideSimulationBase> sims = simByDevice.get(appDevice.getLocation());
		if(sims != null) {
			for(RoomInsideSimulationBase sim: sims)
				sim.close();
		}
		sims = tableProvider.startSupportingLogicForDevice(appDevice, (T) device.getLocationResource(),
				mainPage.getRoomSimulation(device), dpService);
		
		if(Boolean.getBoolean("org.ogema.devicefinder.api.simulateRemoteGateway")) {
			List<RoomInsideSimulationBase> newSims = tableProvider.startSimulationForDevice(appDevice, (T) device.getLocationResource(),
					mainPage.getRoomSimulation(device), dpService);
			if(newSims != null && (!newSims.isEmpty())) {
				if(sims == null)
					sims = new ArrayList<>();
				sims.addAll(newSims);
			}
		}
		if(sims != null)
			simByDevice.put(appDevice.getLocation(), sims);
	}
	
	public <T extends Resource> void updateDatapoints(DeviceHandlerProvider<T> tableProvider, InstallAppDevice appDevice) {
		String deviceLocation = appDevice.device().getLocation();
		DatapointGroup dev = dpService.getGroup(deviceLocation);
		String devName = DeviceTableRaw.getName(appDevice, appManPlus);
		dev.setLabel(null, devName);
		dev.setParameter(DatapointGroup.DEVICE_TYPE_FULL_PARAM, appDevice.device().getResourceType().getName());
		dev.setParameter(DatapointGroup.DEVICE_UNIQUE_ID_PARAM, appDevice.deviceId().getValue());
		dev.setType("DEVICE");
		
		DatapointGroup devType = dpService.getGroup(tableProvider.id());
		devType.setLabel(null, tableProvider.label(null));
		devType.setType("DEVICE_TYPE");
		if(devType.getSubGroup(deviceLocation) == null)
			devType.addSubGroup(dev);
		String devTypeShort = tableProvider.getDeviceTypeShortId(appDevice, dpService);
		
		Room roomRes = appDevice.device().location().room();
		final DPRoom room;
		if(roomRes.exists()) {
			room = dpService.getRoom(roomRes.getLocation());
			room.setResource(roomRes);
		} else 
			room = null;
		
		if(devTypeShort != null && (devTypeShort.equals("UNK")))
			devTypeShort = null;
		
		String subLoc = null;
		if(appDevice.installationLocation().isActive()) {
			if(devTypeShort != null)
				subLoc = devTypeShort+"-"+appDevice.installationLocation().getValue();
			else
				subLoc = appDevice.installationLocation().getValue();
		} else if(devTypeShort != null) {
			subLoc = devTypeShort;
		}
		
		String devName2 = DatapointImpl.getDeviceLabel(null,
				room!=null?room.label(null):Datapoint.UNKNOWN_ROOM_NAME, subLoc, null);
		dev.setLabel(null, devName2);			

		for(Datapoint dp: tableProvider.getDatapoints(appDevice, dpService)) {
			dev.addDatapoint(dp);
			dp.setDeviceResource(appDevice.device().getLocationResource());
			dp.setSubRoomLocation(null, null, subLoc);
			if(room != null)
				dp.setRoom(room);
			initAlarming(tableProvider, appDevice, dp);
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
	
	protected <T extends Resource> void initAlarming(DeviceHandlerProvider<T> tableProvider, InstallAppDevice appDevice, Datapoint dp) {
		if(!appDevice.alarms().isActive()) {
			appDevice.alarms().create().activate(false);
		}
		Resource dpRes1 = dp.getResource();
		if(dpRes1 == null || (!dpRes1.exists()) || (!(dpRes1 instanceof SingleValueResource)))
			return;
		SingleValueResource dpRes = (SingleValueResource)dpRes1;
		AlarmingUtiH.getOrCreateReferencingSensorVal(dpRes, appDevice.alarms());
		IntegerResource alarmStat = AlarmingConfigUtil.getAlarmStatus(dpRes, false);
		if(!alarmStat.isActive()) {
			ValueResourceHelper.setCreate(alarmStat, 0);
			alarmStat.activate(true);
		}
		//dpRes.addDecorator(AlarmingService.ALARMSTATUS_RES_NAME, IntegerResource.class).activate(false);
	}
	
	protected void cleanupOnStart() {
		List<String> knownDevLocs = new ArrayList<>();
		for(InstallAppDevice install: appConfigData.knownDevices().getAllElements()) {
			if(!install.device().exists())
				install.delete();
			else if(knownDevLocs.contains(install.device().getLocation()))
				install.delete();
			else
				knownDevLocs.add(install.device().getLocation());
		}
		
	}
	
	protected void initConfigResourceForOperation() {
		if(!Boolean.getBoolean("org.smartrplace.apps.hw.install.init.startoperation"))
			return;
		//String status = appConfigData.initDoneStatus().getValue();
		//if(status != null && status.contains("A,"))
		if(InitialConfig.isInitDone("A", appConfigData.initDoneStatus()))
			return;
		if(!appConfigData.autoTransferActivation().exists())
			ValueResourceHelper.setCreate(appConfigData.autoTransferActivation(), true);
		if(!appConfigData.autoLoggingActivation().exists())
			ValueResourceHelper.setCreate(appConfigData.autoLoggingActivation(), 2);
		if(!appConfigData.autoConfigureNewDevicesBasedOnTemplate().exists())
			ValueResourceHelper.setCreate(appConfigData.autoConfigureNewDevicesBasedOnTemplate(), true);
		InitialConfig.addString("A", appConfigData.initDoneStatus());
	}
	
	public <T extends Resource> List<InstallAppDevice> getDevices(DeviceHandlerProvider<T> tableProvider) {
		boolean includeInactiveDevices = appConfigData.includeInactiveDevices().getValue();
		return getDevices(tableProvider, includeInactiveDevices, false);
	}
	public <T extends Resource> List<InstallAppDevice> getDevices(DeviceHandlerProvider<T> tableProvider,
			boolean includeInactiveDevices, boolean includeTrash) {
		List<InstallAppDevice> result = new ArrayList<>();
		Class<T> tableType = null;
		//List<ResourcePattern<T>> allPatterns = null;
		if(tableProvider != null) {
			if(includeInactiveDevices)
				tableType = tableProvider.getResourceType();
			//else
			//	allPatterns = tableProvider.getAllPatterns();
		}
		for(InstallAppDevice install: appConfigData.knownDevices().getAllElements()) {
			if((!includeTrash) && install.isTrash().getValue())
				continue;
			if(tableProvider == null) {
				result.add(install);
				continue;
			}
			if(includeInactiveDevices) {
				if(install.device().getResourceType().equals(tableType)) {				
					result.add(install);
				}
			} else {
				if(tableProvider.id().equals(install.devHandlerInfo().getValue()))	{
					result.add(install);
				}
			}
			/*} else 	{
				for(ResourcePattern<?> pat: allPatterns) {
					if(pat.model.equalsLocation(install.device())) {
						result.add(install);
						break;
					}
				}
			}*/
		}
		return result;
	}

	public InstallAppDevice getTemplateDevice(InstallAppDevice source) {
		DeviceHandlerProvider<?> devHand = handlerByDevice.get(source.getLocation());
		return getTemplateDevice(devHand);
	}
	public InstallAppDevice getTemplateDevice(DeviceHandlerProvider<?> devHand) {
		for(InstallAppDevice dev: getDevices(devHand)) {
			if(dev.isTemplate().isActive() && dev.isTemplate().getValue().equals(devHand.id()))
				return dev;
		}
		return null;
	}
}
