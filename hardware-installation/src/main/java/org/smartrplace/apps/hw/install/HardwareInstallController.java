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
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.ogema.accessadmin.api.ApplicationManagerPlus;
import org.ogema.core.application.ApplicationManager;
import org.ogema.core.application.Timer;
import org.ogema.core.application.TimerListener;
import org.ogema.core.logging.OgemaLogger;
import org.ogema.core.model.Resource;
import org.ogema.core.model.ResourceList;
import org.ogema.core.model.simple.BooleanResource;
import org.ogema.core.model.simple.IntegerResource;
import org.ogema.core.model.simple.SingleValueResource;
import org.ogema.core.recordeddata.RecordedData;
import org.ogema.core.resourcemanager.ResourceValueListener;
import org.ogema.core.timeseries.ReadOnlyTimeSeries;
import org.ogema.devicefinder.api.Datapoint;
import org.ogema.devicefinder.api.DatapointGroup;
import org.ogema.devicefinder.api.DatapointService;
import org.ogema.devicefinder.api.DeviceHandlerProvider;
import org.ogema.devicefinder.api.DeviceHandlerProviderDP;
import org.ogema.devicefinder.api.OGEMADriverPropertyService;
import org.ogema.devicefinder.api.TimedJobMemoryData;
import org.ogema.devicefinder.api.TimedJobProvider;
import org.ogema.devicefinder.util.AlarmingConfigUtil;
import org.ogema.devicefinder.util.DatapointImpl;
import org.ogema.devicefinder.util.DatapointImpl.DeviceLabelPlus;
import org.ogema.devicefinder.util.DeviceHandlerBase;
import org.ogema.devicefinder.util.DeviceTableRaw;
import org.ogema.eval.timeseries.simple.smarteff.AlarmingUtiH;
import org.ogema.eval.timeseries.simple.smarteff.AlarmingUtiH.AlarmingUpdater;
import org.ogema.model.devices.buildingtechnology.Thermostat;
import org.ogema.model.extended.alarming.DevelopmentTask;
import org.ogema.model.gateway.remotesupervision.DataLogTransferInfo;
import org.ogema.model.prototypes.PhysicalElement;
import org.ogema.simulation.shared.api.RoomInsideSimulationBase;
import org.ogema.timeseries.eval.simple.api.TimeProcUtil;
import org.ogema.timeseries.eval.simple.mon3.TimeseriesSimpleProcUtil3;
import org.ogema.tools.resource.util.LoggingUtils;
import org.ogema.tools.resourcemanipulator.timer.CountDownAbsoluteTimer;
import org.ogema.tools.resourcemanipulator.timer.CountDownDelayedExecutionTimer;
import org.smartrplace.apps.eval.timedjob.TimedJobConfig;
import org.smartrplace.apps.hw.install.config.HardwareInstallConfig;
import org.smartrplace.apps.hw.install.config.InstallAppDevice;
import org.smartrplace.apps.hw.install.config.InstallAppDeviceBase;
import org.smartrplace.apps.hw.install.deviceeval.BatteryEval;
import org.smartrplace.apps.hw.install.gui.BatteryPage;
import org.smartrplace.apps.hw.install.gui.DeviceConfigPage;
import org.smartrplace.apps.hw.install.gui.MainPage;
import org.smartrplace.apps.hw.install.gui.MainPage.ShowModeHw;
import org.smartrplace.apps.hw.install.gui.RoomSelectorDropdown;
import org.smartrplace.apps.hw.install.gui.ThermostatPage;
import org.smartrplace.apps.hw.install.gui.ThermostatPage.ThermostatPageType;
import org.smartrplace.apps.hw.install.gui.ValveLinkPage;
import org.smartrplace.apps.hw.install.prop.DriverPropertyUtils;
import org.smartrplace.autoconfig.api.DeviceTypeProvider;
import org.smartrplace.autoconfig.api.InitialConfig;
import org.smartrplace.device.export.csv.DeviceTableCSVExporter;
import org.smartrplace.external.accessadmin.config.AccessAdminConfig;
import org.smartrplace.hwinstall.basetable.HardwareTableData;
import org.smartrplace.logging.fendodb.FendoTimeSeries;
import org.smartrplace.tissue.util.logconfig.LogTransferUtil;
import org.smatrplace.apps.hw.install.gui.mainexpert.MainPageExpert;

import de.iwes.util.logconfig.LogHelper;
import de.iwes.util.resource.ResourceHelper;
import de.iwes.util.resource.ValueResourceHelper;
import de.iwes.util.timer.AbsoluteTiming;
import de.iwes.widgets.api.extended.util.UserLocaleUtil;
import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.localisation.OgemaLocale;
import de.iwes.widgets.api.widgets.navigation.NavigationMenu;

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
	public final HardwareTableData hwTableData;
	
	public final TimeseriesSimpleProcUtil3 util;
	
	public static AlarmingUpdater alarmingUpdater = null;

	/** Location of InstallAppDevice -> DeviceHandlerProvider*/
	//private final Map<String, DeviceHandlerProvider<?>> handlerByDevice = new HashMap<>();
	/** Location of InstallAppDevice -> Device simulations*/
	public final Map<String, List<RoomInsideSimulationBase>> simByDevice = new HashMap<>();
	
	public MainPage mainPage;
	public List<MainPage> mainPageExts = new ArrayList<>();
	public DeviceConfigPage deviceConfigPage;
	public ResourceList<DataLogTransferInfo> datalogs;
	//WidgetApp widgetApp;
	public volatile boolean cleanUpOnStartDone = false;
	
	public final DeviceTableCSVExporter csvExport;
	//public final PreknownDeviceCSVImporter csvPreknownImport;
	
	private final Map<String, DeviceTypeProvider<?>> deviceTypeProviders = new HashMap<>();
	public Map<String, DeviceTypeProvider<?>> getDeviceTypeProviders() {
		synchronized(deviceTypeProviders) {
			return new HashMap<String, DeviceTypeProvider<?>>(deviceTypeProviders);
		}
	}
	public void addDeviceTypeProvider(DeviceTypeProvider<?> prov) {
		synchronized(deviceTypeProviders) {
			deviceTypeProviders.put(prov.id(), prov);
		}
	}
	
	ResourceValueListener<BooleanResource> searchForHwListener = null;
	
	CountDownAbsoluteTimer maxSendingTimer;
	
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
		this.appManPlus.setGuiService(hardwareInstallApp.guiService);
		
		this.accessAdminConfigRes = appMan.getResourceAccess().getResource("accessAdminConfig");
		
		//first step init
		String localeString = ResourceHelper.getLocalGwInfo(appMan).systemLocale().getValue();
		if(localeString == null || localeString.isEmpty())
			localeString = "de"; //TODO
		UserLocaleUtil.setSystemDefaultLocale(localeString, appMan.getAdministrationManager());
		initConfigurationResource();
		this.hwTableData = new HardwareTableData(appMan);
		this.csvExport = new DeviceTableCSVExporter(appConfigData.knownDevices());
		
		cleanupOnStart();
		cleanUpOnStartDone = true;
		
		mainPage = getMainPage(page);
		final NavigationMenu menu = new NavigationMenu(" Select page");
		if(Boolean.getBoolean("org.smartrplace.app.srcmon.ieehq")) {
			//menu = new NavigationMenu(" Browse pages");
			menu.addEntry(mainPage.getHeader(), page);
			page.getMenuConfiguration().setCustomNavigation(menu);
			
			final WidgetPage<?> pageExpert2 = hardwareInstallApp.widgetApp.createWidgetPage("customerexperthwpage.html");
			MainPageExpert expertPage = new MainPageExpert(pageExpert2, this, ShowModeHw.STANDARD);
			mainPageExts.add(expertPage);
			menu.addEntry(expertPage.getHeader(), pageExpert2);
			pageExpert2.getMenuConfiguration().setCustomNavigation(menu);
		}
		
		if(Boolean.getBoolean("org.smartrplace.apps.hw.install.expert.showthermostatpage")) {
			//menu = new NavigationMenu(" Browse pages");
			menu.addEntry(mainPage.getHeader(), page);
			page.getMenuConfiguration().setCustomNavigation(menu);
			
			final WidgetPage<?> thermPage2 = hardwareInstallApp.widgetApp.createWidgetPage("thermostatpage.html");
			new ThermostatPage(thermPage2, this, ThermostatPageType.STANDARD_VIEW_ONLY);
			menu.addEntry("Thermostat Page", thermPage2);
			thermPage2.getMenuConfiguration().setCustomNavigation(menu);
		}

		if(Boolean.getBoolean("org.smartrplace.apps.hw.install.expert.fal230support")) {
			menu.addEntry(mainPage.getHeader(), page);
			page.getMenuConfiguration().setCustomNavigation(menu);
			WidgetPage<?> pageValveLink =  hardwareInstallApp.widgetApp.createWidgetPage("deviceValveLink2.html");
			new ValveLinkPage(pageValveLink, appManPlus);
			menu.addEntry("Link FAL230 valves to wall thermostats", pageValveLink);
			pageValveLink.getMenuConfiguration().setCustomNavigation(menu);
		}

		if(Boolean.getBoolean("org.smartrplace.apps.hw.install.customerbatteryexchange")) {
			menu.addEntry(mainPage.getHeader(), page);
			page.getMenuConfiguration().setCustomNavigation(menu);
			WidgetPage<?> pageBatteryBase =  hardwareInstallApp.widgetApp.createWidgetPage("batteriescust.html");
			new BatteryPage(pageBatteryBase, this, false) {
				@Override
				protected boolean suppressSearchForNewDevices() {
					return true;
				}
			};
			menu.addEntry("Battery Status Overview", pageBatteryBase);
			//pageBatteryBase.getMenuConfiguration().setCustomNavigation(menu);
			pageBatteryBase.getMenuConfiguration().setLanguageSelectionVisible(false);
			pageBatteryBase.getMenuConfiguration().setShowMessages(false);
		}

		initConfigResourceForOperation();
        initDemands();
		util = new TimeseriesSimpleProcUtil3(appMan, appManPlus.dpService(), 4, 3*TimeProcUtil.MINUTE_MILLIS);
		
		TimedJobProvider tprov = new TimedJobProvider() {
			
			@Override
			public String label(OgemaLocale locale) {
				return "ResendOpenEmptyPosDaily";
			}
			
			@Override
			public String id() {
				return "ResendOpenEmptyPosDaily";
			}
			
			@Override
			public boolean initConfigResource(TimedJobConfig config) {
				ValueResourceHelper.setCreate(config.alignedInterval(), AbsoluteTiming.DAY);
				ValueResourceHelper.setCreate(config.interval(), 1300);
				ValueResourceHelper.setCreate(config.disable(), false);
				ValueResourceHelper.setCreate(config.performOperationOnStartUpWithDelay(), -1);
				return false;
			}
			
			@Override
			public String getInitVersion() {
				return "A";
			}
			
			@Override
			public void execute(long now, TimedJobMemoryData data) {
				ThermostatPage.resendOpenEmptPos(null, dpService);
			}
			
			@Override
			public int evalJobType() {
				return 0;
			}
		};
		if(Boolean.getBoolean("org.smartrplace.apps.hw.install.gui.resendEmptyPosDaily")) {
			TimedJobMemoryData data = dpService.timedJobService().registerTimedJobProvider(tprov);
			data.res().disable().setValue(false);
			data.startTimerIfNotStarted();
		} else {
			TimedJobMemoryData data = dpService.timedJobService().getProvider("ResendOpenEmptyPosDaily");
			if(data != null) {
				ValueResourceHelper.setCreate(data.res().disable(), true);
				data.stopTimerIfRunning();
				
			}
		}
		
		BatteryEval.initWeeklyEmail(appManPlus);
	}

	protected MainPage getMainPage(WidgetPage<?> page) {
		if(!Boolean.getBoolean("org.smartrplace.driverhandler.devices.residentialmetering1"))
			return new MainPage(page, this, true);
		return new MainPage(page, this, true) {
			@Override
			protected boolean hideDeviceHandler(DeviceHandlerProvider<?> devHand) {
				return !devHand.relevantForUsers();
			}
		};
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
			appMan.getLogger().debug("{} started with new config resource", getClass().getName());
		}
		ValueResourceHelper.setIfNew(appConfigData.bulkMessageIntervalDuration(), 4*TimeProcUtil.HOUR_MILLIS);
		ValueResourceHelper.setIfNew(appConfigData.maxMessageNumBeforeBulk(), 3);
		appConfigData.knownDevelopmentTasks().create();
		cleanUpDevelopmentTasks();
		appConfigData.activate(true);
    }
    
    /*
     * register ResourcePatternDemands. The listeners will be informed about new and disappearing
     * patterns in the OGEMA resource tree
     */
    public void initDemands() {
    	searchForHwListener = new ResourceValueListener<BooleanResource>() {

			@Override
			public void resourceChanged(BooleanResource resource) {
				checkDemands();
			}
		};
		appConfigData.isInstallationActive().addValueListener(searchForHwListener, false);
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

	public String validateProposedDeviceId(String deviceId) {
		if (deviceId == null)
			return deviceId;
		for(InstallAppDevice other: appConfigData.knownDevices().getAllElements()) {
			if (other.deviceId().exists() && deviceId.equals(other.deviceId().getValue())) {
				return null;
			}
		}
		return deviceId;
	}
	
	public <T extends PhysicalElement> InstallAppDevice addDeviceIfNew(T device, DeviceHandlerProvider<T> tableProvider) {
		return addDeviceIfNew(device, tableProvider, null);
	}
		
	/*
	 * if the app needs to consider dependencies between different pattern types,
	 * they can be processed here.
	 */
	public <T extends PhysicalElement> InstallAppDevice addDeviceIfNew(T device, DeviceHandlerProvider<T> tableProvider, String proposedDeviceId) {
		for(InstallAppDevice install: appConfigData.knownDevices().getAllElements()) {
			if(install.device().equalsLocation(device)) {
				if (proposedDeviceId != null && (!install.deviceId().exists() || install.deviceId().getValue().isEmpty())) {
					// validate that the proposed id does not exist yet
					proposedDeviceId = validateProposedDeviceId(proposedDeviceId);
				}
				initializeDevice(install, device, tableProvider, proposedDeviceId); // only initialize missing resources
					//if(Boolean.getBoolean("org.smartrplace.apps.hw.install.existing.configalarming")) {
				//The init process is done only if the InitialConfig.isInitDone method is false for the device
				initAlarmingForDevice(install, tableProvider);
					//}
				return install;
			}
		}
		InstallAppDevice install = appConfigData.knownDevices().add();
		initializeDevice(install, device, tableProvider, validateProposedDeviceId(proposedDeviceId));
		if(tableProvider != null)
			startSimulation(tableProvider, install);
		updateDatapoints(tableProvider, install);
		if(appConfigData.autoLoggingActivation().getValue() == 1) {
			activateLogging(tableProvider, install, true, false);
		}
		InstallAppDevice currentTemplate = null;
		if(appConfigData.autoConfigureNewDevicesBasedOnTemplate().getValue()) {
			currentTemplate = getTemplateDevice(tableProvider);
			if(currentTemplate != null && (!currentTemplate.equalsLocation(install)))
				AlarmingConfigUtil.copySettings(currentTemplate, install, appMan);
		}
		if(currentTemplate == null && Boolean.getBoolean("org.smartrplace.apps.hw.install.init.startalarming")) {
			initAlarmingForDevice(install, tableProvider);
		}
		return install;
	}
	
	volatile CountDownDelayedExecutionTimer alarmingRestartTimer = null;
	
	protected <T extends PhysicalElement> void initAlarmingForDevice(final InstallAppDevice install, final DeviceHandlerProvider<T> tableProvider) {
		final String deviceId = install.deviceId().getValue();
		final String provVersion = tableProvider.getInitVersion();
		final String shortID = provVersion.isEmpty()?deviceId:(deviceId+"_"+provVersion); //tableProvider.getDeviceTypeShortId(install, dpService);
		if((!InitialConfig.isInitDone(shortID, appConfigData.initDoneStatus()))) {// &&
			//	(getDevices(tableProvider).size() <= 1)) {
			tableProvider.initAlarmingForDevice(install, appConfigData);
			if(!InitialConfig.isInitDone(shortID, appConfigData.initDoneStatus()))
				InitialConfig.addString(shortID, appConfigData.initDoneStatus(), deviceId);
			//we call init again after 5 minutes as we expect that all datapoint resources have been set up after 5 minutes
			//on test systems starting with replay-on-clean resources the delay usually is set to 10 to avoid interfereces with
			//editing the values manually for testing
			new CountDownDelayedExecutionTimer(appMan, Long.getLong("org.smartrplace.apps.hw.install.alarming.reinitdelay", 5*60000)) {
				
				@Override
				public void delayedExecution() {
					tableProvider.initAlarmingForDevice(install, appConfigData);
					if(alarmingRestartTimer == null) {
						alarmingRestartTimer = new CountDownDelayedExecutionTimer(appMan, 4*60000) {
							
							@Override
							public void delayedExecution() {
								if(alarmingUpdater != null) {
									alarmingUpdater.updateAlarming();
								} else
									log.warn("Could not find Alarming Management");				
								alarmingRestartTimer = null;								
							}
						};
					}
				}
			};
		}
		if(DeviceTableRaw.getTemplateForType(getDevices(tableProvider), tableProvider) == null)
			ValueResourceHelper.setCreate(install.isTemplate(), tableProvider.id());
		else if(install.isTemplate().exists()) {
			String val = install.isTemplate().getValue();
			if(!val.isEmpty() && (!val.equals(install.devHandlerInfo().getValue()))) {
				install.isTemplate().deactivate(false);
				install.isTemplate().setValue("");
			}
		}
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
	
	public <T extends PhysicalElement> void initializeDevice(InstallAppDevice install, T device,
			DeviceHandlerProvider<T> tableProvider, String proposedDeviceId) {

		if (!install.exists()) install.create();
		if (device != null && !install.device().exists()) install.device().setAsReference(device);
		if (!install.installationStatus().exists()) install.installationStatus().create();
		if (!install.deviceId().exists()) install.deviceId().create();
		if (install.deviceId().getValue().isEmpty()) {
            final String deviceId = proposedDeviceId != null ? proposedDeviceId :
                  LocalDeviceId.generateDeviceId(install, appConfigData, tableProvider, dpService);
            install.deviceId().setValue(deviceId);
		}
		if(!install.devHandlerInfo().exists()) {
			install.devHandlerInfo().create();
			install.devHandlerInfo().setValue(tableProvider.id());
		}
		install.activate(true);
	}
	
	public <T extends PhysicalElement> void startSimulations(DeviceHandlerProvider<T> tableProvider) {
		//Class<?> tableType = tableProvider.getResourceType();
		Collection<DeviceTypeProvider<?>> dtbproviders = tableProvider.getDeviceTypeProviders();
		if(dtbproviders != null) for(DeviceTypeProvider<?> prov: dtbproviders) synchronized(deviceTypeProviders) {
			addDeviceTypeProvider(prov);
		}
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
	    		String devTypeShort = tableProvider.getDeviceTypeShortId(dpService);
	    		devType.setParameter(DatapointGroup.DEVICE_TYPE_SHORT_PARAM, devTypeShort);
	    		//devType.setParameter("PROVIDER", provider);
	    	}

		}
	}
	public <T extends PhysicalElement> void startSimulation(DeviceHandlerProvider<T> tableProvider, T device) {
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
	public <T extends PhysicalElement> void startSimulationForced(DeviceHandlerProvider<T> tableProvider, InstallAppDevice appDevice) {
		@SuppressWarnings("unchecked")
		T device = (T) appDevice.device();
		startSimulation(tableProvider, appDevice, device);
	}
	
	public <T extends PhysicalElement> void startSimulation(DeviceHandlerProvider<T> tableProvider, InstallAppDevice appDevice) {
		if(Boolean.getBoolean("org.smartrplace.apps.hw.install.blockinitdatapoints"))
			return;
		startSimulationForced(tableProvider, appDevice);
	}
	@SuppressWarnings("unchecked")
	public <T extends PhysicalElement> void startSimulation(DeviceHandlerProvider<T> tableProvider, InstallAppDevice appDevice,
			T device) {
		//handlerByDevice.put(appDevice.getLocation(), tableProvider);
		//if(Boolean.getBoolean("org.smartrplace.apps.hw.install.autologging")) {
		if(appConfigData.autoConfigureNewDevicesBasedOnTemplate().getValue())
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
		try {
		sims = tableProvider.startSupportingLogicForDevice(appDevice, (T) device.getLocationResource(),
				mainPage.getRoomSimulation(device), dpService);
		} catch(ClassCastException e) {
			(new IllegalStateException("Wrong device type: "+appDevice.getName(), e)).printStackTrace();
			return;
		}
		
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
	
	public <T extends Resource> void updateDatapoints(DeviceHandlerProviderDP<?> tableProvider, InstallAppDevice appDevice) {
		String deviceLocation = appDevice.device().getLocation();
		DatapointGroup dev = dpService.getGroup(deviceLocation);
		//String devName = DeviceTableRaw.getName(appDevice, appManPlus);
		//dev.setLabel(null, devName);
		DeviceLabelPlus dvNamPs = DatapointImpl.getDeviceLabelPlus(appDevice, null, dpService, tableProvider);
		dev.setLabel(null, dvNamPs.deviceLabel);			
		dev.setParameter(DatapointGroup.DEVICE_TYPE_FULL_PARAM, appDevice.device().getResourceType().getName());
		dev.setParameter(DatapointGroup.DEVICE_UNIQUE_ID_PARAM, appDevice.deviceId().getValue());
		dev.setType("DEVICE");
		
		DatapointGroup devType = dpService.getGroup(tableProvider.id());
		devType.setLabel(null, tableProvider.label(null));
		devType.setType("DEVICE_TYPE");
		if(devType.getSubGroup(deviceLocation) == null)
			devType.addSubGroup(dev);

		try {
			Collection<Datapoint> allDps = tableProvider.getDatapoints(appDevice, dpService);
			for(Datapoint dp: allDps) {
				dev.addDatapoint(dp);
				dp.setDeviceResource((Resource) appDevice.device().getLocationResource());
				dp.addToSubRoomLocationAtomic(null, null, dvNamPs.subLoc, true);
				if(dvNamPs.room != null)
					dp.setRoom(dvNamPs.room);
				initAlarming(tableProvider, appDevice, dp);
			}
			ValueResourceHelper.setCreate(appDevice.dpNum(), allDps.size());
		} catch(Exception e) {
			appMan.getLogger().error("Could not get Datapoints for tableProvider:"+tableProvider.id(), e);
			e.printStackTrace();
		}
	}
	public <T extends Resource> void activateLogging(DeviceHandlerProviderDP<?> devHand, InstallAppDevice appDevice,
			boolean onylDefaultLoggingDps, boolean disable) {
		Collection<Datapoint> allDps = devHand.getDatapoints(appDevice, dpService);
		for(Datapoint dp: allDps) {
			if((!devHand.relevantForDefaultLogging(dp, appDevice)) && onylDefaultLoggingDps)
				continue;
			ReadOnlyTimeSeries ts = dp.getTimeSeries();
			if(ts == null || (!(ts instanceof RecordedData)))
				continue;
			if((ts instanceof FendoTimeSeries) && ((FendoTimeSeries)ts).isReadOnly())
				continue;
			RecordedData rec = (RecordedData)ts;
			if(Boolean.getBoolean("org.smartrplace.app.srcmon.isgateway") &&
					(appConfigData.autoTransferActivation().getValue() || disable)) {
				Resource res = appMan.getResourceAccess().getResource(rec.getPath());
				if(res != null && res instanceof SingleValueResource && (datalogs != null)) {
					if(disable)
						LogTransferUtil.stopTransmitLogData((SingleValueResource) res, datalogs);
					else
						LogTransferUtil.startTransmitLogData((SingleValueResource) res, datalogs);
				}
			}
			if(disable)
				LoggingUtils.deactivateLogging(rec);
			else {
				Resource dpRes = dp.getResource();
				if(Boolean.getBoolean("org.smartrplace.apps.hw.install.initnonpersistent_fromlog") && (dpRes != null)
						&& (dpRes instanceof SingleValueResource))
					LogHelper.setNonpersistentResourceWithLastLog((SingleValueResource) dp.getResource());
				LoggingUtils.activateLogging(rec, -2);
			}	
		}
	}
	
	protected <T extends Resource> void initAlarming(DeviceHandlerProviderDP<?> tableProvider, InstallAppDevice appDevice, Datapoint dp) {
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
	
	public void cleanupOnStart() {
		cleanupOnStart(false);
	}
	/** !!! Only call this with deleteIfDevHandMissing=true when all device handlers have been started up !!!
	 * 
	 * @param deleteIfDevHandMissing
	 */
	public void cleanupOnStart(boolean deleteIfDevHandMissing) {
		List<String> knownDevLocs = new ArrayList<>();
		for(InstallAppDevice install: appConfigData.knownDevices().getAllElements()) {
			if(!install.device().exists()) {
				install.delete();
				continue;
			} else if(knownDevLocs.contains(install.device().getLocation())) {
				install.delete();
				continue;
			} else if((!install.devHandlerInfo().exists()) || install.devHandlerInfo().getValue().isEmpty()) {
				install.delete();
				continue;
			}
			String devHandToRemove = System.getProperty("org.smartrplace.apps.hw.install.devHandDataToReset");
			if(devHandToRemove != null && devHandToRemove.equals(install.devHandlerInfo().getValue())) {
				System.out.println("Deleting "+install.getLocation()+ " for device "+install.device().getLocation()+" ...");
				install.delete();
				System.out.println("     DONE deletion: "+install.getLocation());
				continue;					
			}

			if(deleteIfDevHandMissing) {
				DeviceHandlerProviderDP<Resource> devHand = dpService.getDeviceHandlerProvider(install);
				if(devHand == null) {
					install.delete();
					continue;
				}
			}
			knownDevLocs.add(install.device().getLocation());
			if(install.isTrash().getValue()) {
				DeviceHandlerProviderDP<Resource> devHand = dpService.getDeviceHandlerProvider(install);
				if(devHand == null) {
					install.device().getLocationResource().deactivate(true);										
				} else try {
					for(Resource dev: devHand.devicesControlled(install)) {
						dev.getLocationResource().deactivate(true);					
					}
				} catch(ClassCastException e) {
					install.delete();
				}
			} else if(Boolean.getBoolean("org.smartrplace.apps.hw.install.removeA") && install.deviceId().getValue().endsWith("_A")) {
				String oldDeviceId = install.deviceId().getValue();
				String newDeviceId = oldDeviceId.substring(0, oldDeviceId.length()-2);
				if(!LocalDeviceId.isDeviceIdUsed(newDeviceId, appConfigData))
					install.deviceId().setValue(newDeviceId);
			}
		}
		
	}
	
	protected void initConfigResourceForOperation() {
		if(Boolean.getBoolean("org.smartrplace.apps.hw.install.init.reinitconfig"))  {
			//TODO: Delete does not directly take effect
			appConfigData.initDoneStatus().setValue("");
			//appConfigData.initDoneStatus().delete();
		}
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
		if(Boolean.getBoolean("org.smartrplace.apps.hw.install.init.alarmtesting.forcestartalarming")) {
			//We have to configure all alarming resources first, it is not easy to determine when this is finished, though
			//!! Note: This only works if isLAlarmingActive is not true before e.g. by loading from replay-on-clean
			new CountDownDelayedExecutionTimer(appMan, 10000) {
				@Override
				public void delayedExecution() {
					if(appConfigData.isAlarmingActive().getValue())
						throw new IllegalStateException("Alarming must not be activated e.g. via replay-on-clean when testing with special test configuration!");
					ValueResourceHelper.setCreate(appConfigData.isAlarmingActive(), true);
				}
			};
		}
		InitialConfig.addString("A", appConfigData.initDoneStatus());
	}
	
	public <T extends Resource> Collection<InstallAppDevice> getDevices(DeviceHandlerProviderDP<?> devHand) {
		return mainPage.getDevices(devHand);
		/*boolean includeInactiveDevices = appConfigData.includeInactiveDevices().getValue();
		return getDevices(tableProvider, includeInactiveDevices, false);*/
	}
	public <T extends PhysicalElement> Collection<InstallAppDevice> getDevices(DeviceHandlerProvider<T> tableProvider,
//			boolean includeInactiveDevices,
			boolean includeTrash) {
		return mainPage.getDevices(tableProvider, includeTrash);
	}
	/*public <T extends Resource> List<InstallAppDevice> getDevices(DeviceHandlerProvider<T> tableProvider,
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
		}
		return result;
	}*/
	public DeviceHandlerProviderDP<Resource> getDeviceHandler(InstallAppDevice source) {
		return dpService.getDeviceHandlerProvider(source);
		//return handlerByDevice.get(source.getLocation());
	}
	
	public DeviceHandlerProvider<?> getDeviceHandlerForTrash(InstallAppDevice install) {
		return hwInstApp.getTableProviders().get(install.devHandlerInfo().getValue());
	}
	
	public InstallAppDevice getTemplateDevice(InstallAppDevice source) {
		DeviceHandlerProviderDP<?> devHand = getDeviceHandler(source); //handlerByDevice.get(source.getLocation());
		return getTemplateDevice(devHand);
	}
	public InstallAppDevice getTemplateDevice(DeviceHandlerProviderDP<?> devHand) {
		for(InstallAppDevice dev: getDevices(devHand)) {
			if(dev.isTemplate().isActive() && dev.isTemplate().getValue().equals(devHand.id()))
				return dev;
		}
		return null;
	}
	
	protected void cleanUpDevelopmentTasks() {
		for(DevelopmentTask devT: appConfigData.knownDevelopmentTasks().getAllElements()) {
			for(InstallAppDeviceBase templ: devT.templates().getAllElements()) {
				if(!templ.device().exists() && templ.alarms().size() > 0) {
					SingleValueResource sensorVal = templ.alarms().getAllElements().get(0).sensorVal();
					if(!sensorVal.exists())
						continue;
					String sensorLoc = sensorVal.getLocation();
					for(InstallAppDevice iad: dpService.managedDeviceResoures(null, true, true)) {
						if(sensorLoc.startsWith(iad.device().getLocation())) {
							templ.device().setAsReference(iad.device());
							break;
						}
					}
					if(!templ.device().exists()) {
						System.out.println("No device for template found: "+templ.devHandlerInfo().getValue()+" / "+templ.getLocation());
						templ.delete();
					}
				}
			}
		}
	}
	
	public boolean restoreMaxSendingTimerAfterRestart() {
		Collection<InstallAppDevice> all = getDevices(null);
		long now = appMan.getFrameworkTime();
		boolean result = false;
		for(InstallAppDevice dev: all) {
			if(dev.isTrash().getValue())
				continue;
			if(dev.device() instanceof Thermostat) {
				if(updateMaxSendingTimer((Thermostat) dev.device(), now, false))
					result = true;
			}
		}
		if(appConfigData.maxSendModeUntil().getValue() <= now) {
			if(maxSendingTimer != null) {
				 maxSendingTimer.destroy();
				 maxSendingTimer = null;
			}
			if(appConfigData.maxSendModeUntil().getValue() > 0) {
				DeviceHandlerBase.setOpenIntervalConfigs(appConfigData.sendIntervalMode(), all, null, false, appMan.getResourceAccess());					
				appConfigData.maxSendModeUntil().setValue(-1);
			}			
		}
		return result;
	}

	public boolean setMaxSendingTimerForAllThermostats(boolean state) {
		Collection<InstallAppDevice> all = getDevices(null);
		long now = appMan.getFrameworkTime();
		boolean result = false;
		for(InstallAppDevice dev: all) {
			if(dev.isTrash().getValue())
				continue;
			if(dev.device() instanceof Thermostat) {
				Thermostat device = (Thermostat) dev.device();
				BooleanResource maxSendUntil = DeviceHandlerBase.getMaxSendSingle(device);
				if(state)
					ValueResourceHelper.setCreate(maxSendUntil, true);
				else {
					maxSendUntil.setValue(false);
					if(maxSendingTimer != null) {
						 maxSendingTimer.destroy();
						 maxSendingTimer = null;					
						 appConfigData.maxSendModeUntil().setValue(-1);
					}
				}
				if(updateMaxSendingTimer(device, now, true))
					result = true;
			}
		}
		return result;
	}

	public static final long MAX_SEND_TIME = 48*TimeProcUtil.HOUR_MILLIS;
	public static final long MAX_SEND_TIME_RESET = 47*TimeProcUtil.HOUR_MILLIS;
	/**
	 * 
	 * @param device
	 * @param now
	 * @param thermostatWasJustUpated only relevant if thermostat is set to max sending. If true then the timer will be restarted
	 * 		if already running for more than an hour to get back to the full duration. If false then the max sending period will
	 * 		just be continued, usually after a restrt of the system.
	 * @return
	 */
	public boolean updateMaxSendingTimer(Thermostat device, long now, boolean thermostatWasJustUpated) {
		synchronized (appConfigData) {
			BooleanResource maxSendUntil = DeviceHandlerBase.getMaxSendSingle(device);
			if(maxSendUntil != null && maxSendUntil.getValue()) {
				if(appConfigData.maxSendModeUntil().getValue() - now < MAX_SEND_TIME_RESET) {
					ValueResourceHelper.setCreate(appConfigData.maxSendModeUntil(), now+MAX_SEND_TIME);
				}
				if(appConfigData.maxSendModeUntil().getValue() > now) {
					DeviceHandlerBase.setSendIntervalByMode(device.getLocationResource(), 3, false, appMan.getResourceAccess());
					if(maxSendingTimer != null &&
							Math.abs(maxSendingTimer.getNextRunTime() - appConfigData.maxSendModeUntil().getValue()) > TimeProcUtil.MINUTE_MILLIS) {
						 maxSendingTimer.destroy();
						 maxSendingTimer = null;					
					}
					if(maxSendingTimer == null) {
						maxSendingTimer = new CountDownAbsoluteTimer(appMan, appConfigData.maxSendModeUntil().getValue(), true,
								new TimerListener() {
									
									@Override
									public void timerElapsed(Timer timer) {
										Collection<InstallAppDevice> all = getDevices(null);
										DeviceHandlerBase.setOpenIntervalConfigs(appConfigData.sendIntervalMode(), all, null, false, appMan.getResourceAccess());
										appConfigData.maxSendModeUntil().setValue(-1);
									}
								});
					}
				}
				return true;
			} else {
				DeviceHandlerBase.setSendIntervalByMode(device.getLocationResource(), appConfigData.sendIntervalMode(), false, appMan.getResourceAccess());
				return false;
			}			
		}
	}
}
