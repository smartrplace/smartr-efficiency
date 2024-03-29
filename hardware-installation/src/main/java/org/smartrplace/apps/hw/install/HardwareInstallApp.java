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

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.References;
import org.apache.felix.scr.annotations.Service;
import org.ogema.core.application.Application;
import org.ogema.core.application.ApplicationManager;
import org.ogema.core.logging.OgemaLogger;
import org.ogema.devicefinder.api.DatapointService;
import org.ogema.devicefinder.api.DeviceHandlerProvider;
import org.ogema.devicefinder.api.DriverHandlerProvider;
import org.ogema.devicefinder.api.OGEMADriverPropertyService;
import org.ogema.tools.driver.api.CCUAccessI;
import org.ogema.util.controllerprovider.GenericControllerProvider;
import org.smartrplace.apps.hw.install.experimental.HardwareInstallPageTest;
import org.smartrplace.apps.hw.install.gui.MainPage;
import org.smartrplace.hwinstall.basetable.DeviceHandlerAccess;
import org.smartrplace.tissue.util.resource.GatewaySyncResourceService;

import de.iwes.widgets.api.OgemaGuiService;
import de.iwes.widgets.api.widgets.WidgetApp;
import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.navigation.MenuConfiguration;
import de.iwes.widgets.api.widgets.navigation.NavigationMenu;

/**
 * Template OGEMA application class
 */
@References({
	@Reference(
		name="tableProviders",
		referenceInterface=DeviceHandlerProvider.class,
		cardinality=ReferenceCardinality.OPTIONAL_MULTIPLE,
		policy=ReferencePolicy.DYNAMIC,
		bind="addTableProvider",
		unbind="removeTableProvider"),
	@Reference(
		name="driverProviders",
		referenceInterface=DriverHandlerProvider.class,
		cardinality=ReferenceCardinality.OPTIONAL_MULTIPLE,
		policy=ReferencePolicy.DYNAMIC,
		bind="addDriverProvider",
		unbind="removeDriverProvider"),
	@Reference(
		name="servletProviders",
		referenceInterface=HWInstallExtensionProvider.class,
		cardinality=ReferenceCardinality.OPTIONAL_MULTIPLE,
		policy=ReferencePolicy.DYNAMIC,
		bind="addExtProvider",
		unbind="removeExtProvider"),
	@Reference(
		name="propertyServices",
		referenceInterface=OGEMADriverPropertyService.class,
		cardinality=ReferenceCardinality.OPTIONAL_MULTIPLE,
		policy=ReferencePolicy.DYNAMIC,
		bind="addDriverPropertyProvider",
		unbind="removeDriverPropertyProvider"),
	@Reference(
		name="cmsConnector",
		referenceInterface=CCUAccessI.class,
		cardinality=ReferenceCardinality.OPTIONAL_MULTIPLE,
		policy=ReferencePolicy.DYNAMIC,
		bind="addCCUAccess",
		unbind="removeCCUAccess"),
	//@Reference(
	//	name="alarmingInternal",
	//	referenceInterface=AlarmingStartedService.class,
	//	cardinality=ReferenceCardinality.OPTIONAL_MULTIPLE,
	//	policy=ReferencePolicy.DYNAMIC,
	//	bind="addAlarmingStarted",
	//	unbind="removeAlarmingStarted"),
	@Reference(
		name="gwReplicationConnector",
		referenceInterface=GatewaySyncResourceService.class,
		cardinality=ReferenceCardinality.OPTIONAL_MULTIPLE,
		policy=ReferencePolicy.DYNAMIC,
		bind="addGwSyncAccess",
		unbind="removeGwSyncAccess")
})

@Component(specVersion = "1.2", immediate = true)
@Service(Application.class)
public class HardwareInstallApp implements Application, DeviceHandlerAccess {
	public static final String urlPath = "/org/smartrplace/hardwareinstall";

	protected OgemaLogger log;
    protected ApplicationManager appMan;
    protected HardwareInstallController controller;

    protected WidgetApp widgetApp;
	public NavigationMenu menu;

	private final Map<String, DeviceHandlerProvider<?>> tableProviders = Collections.synchronizedMap(new LinkedHashMap<String,DeviceHandlerProvider<?>>());
	public Map<String, DeviceHandlerProvider<?>> getTableProviders() {
		synchronized (tableProviders) {
			return new LinkedHashMap<>(tableProviders);
		}
	}
	private final Map<String,DriverHandlerProvider> driverProviders = Collections.synchronizedMap(new LinkedHashMap<String,DriverHandlerProvider>());
	public Map<String,DriverHandlerProvider> getDriverProviders() {
		synchronized (driverProviders) {
			return new LinkedHashMap<>(driverProviders);
		}
	}
	private final Map<String,OGEMADriverPropertyService<?>> dPropertyProviders = Collections.synchronizedMap(new LinkedHashMap<String,OGEMADriverPropertyService<?>>());
	public LinkedHashMap<String, OGEMADriverPropertyService<?>> getDPropertyProviders() {
		synchronized (dPropertyProviders) {
			return new LinkedHashMap<>(dPropertyProviders);
		}
	}

	@Reference
	public OgemaGuiService guiService;

	@Reference
	DatapointService dpService;
	
	volatile HardwareInstallPageTest testPageImpl; 
	
	public volatile CCUAccessI ccuAccess = null;
	
	public volatile GatewaySyncResourceService gwSync = null;

	//public volatile AlarmingStartedService alarmingInternal = null;

	protected final GenericControllerProvider<HardwareInstallController> controllerProvider;
    public HardwareInstallApp() {
		controllerProvider = new GenericControllerProvider<HardwareInstallController>(
				"org.smartrplace.apps.hw.install.expert.HardwareInstallAppExpert",
				"org.smartrplace.apps.hw.install.superadmin.HardwareInstallAppSuperadmin");
	}
	/*
     * This is the entry point to the application.
     */
 	@Override
    public void start(ApplicationManager appManager) {

        // Remember framework references for later.
        appMan = appManager;
        log = appManager.getLogger();

		//register a web page with dynamically generated HTML
		widgetApp = guiService.createWidgetApp(urlPath, appManager);
		final WidgetPage<?> page = widgetApp.createStartPage();
		page.showOverlay(true);
		synchronized (this) {
			controller = new HardwareInstallController(appMan, page, this, dpService);
			for(DeviceHandlerProvider<?> tableP: tableProviders.values()) {
	    		controller.startSimulations(tableP);			
			}
			for(OGEMADriverPropertyService<?> dPProv: dPropertyProviders.values()) {
	    		controller.initKnownResources(dPProv);			
			}
			controllerProvider.setController(controller);
		}
		final WidgetPage<?> testPage = widgetApp.createWidgetPage("index2.html");
		this.testPageImpl = new HardwareInstallPageTest(appManager, testPage);
		NavigationMenu menu = page.getMenuConfiguration().getCustomNavigation();
		if (menu == null) {
			menu = new NavigationMenu(" Select Page");
			menu.addEntry("Main page", page);
		}
		menu.addEntry("Test page", testPage);
		page.getMenuConfiguration().setCustomNavigation(menu);
		testPage.getMenuConfiguration().setCustomNavigation(menu);
		synchronized (tableProviders) {
			tableProviders.values().forEach(testPageImpl::addDeviceHandler);
		}
     }

     /*
     * Callback called when the application is going to be stopped.
     */
    @Override
    public void stop(AppStopReason reason) {
    	if (widgetApp != null) widgetApp.close();
		if (controller != null)
    		controller.close();
		this.testPageImpl = null;
        log.info("{} stopped", getClass().getName());
    }
    
    protected void addTableProvider(DeviceHandlerProvider<?> provider) {
    	synchronized (this) {
    		tableProviders.put(provider.id(), provider);
    		if(Boolean.getBoolean("org.smartplace.app.srcmon.server.issuperior"))
    			leadDeviceHandlerFound = true;
    		else if((!leadDeviceHandlerFound) && provider.id().equals("org.smartrplace.homematic.devicetable.CO2SensorHmHandler"))
    			leadDeviceHandlerFound = true;
	    	if(controller != null && controller.demandsActivated) {
	    		provider.addPatternDemand(controller.mainPage);
	    	}
	    	if(controller != null) {
	    		controller.startSimulations(provider);
	    	}
	    	if(controller != null && controller.mainPage != null) {
	    		controller.mainPage.updateTables();
				synchronized(controller.mainPageExts) {
		    		for(MainPage mp: controller.mainPageExts) {
		    			mp.updateTables();
		    		}
				}
	    	}
	    	final HardwareInstallPageTest testPage =this.testPageImpl;
	    	if (testPage != null) {
	    		testPage.addDeviceHandler(provider);
	    	}
    	}
    }
    protected void removeTableProvider(DeviceHandlerProvider<?> provider) {
    	synchronized (tableProviders) {
	    	tableProviders.remove(provider.id());
	    	final HardwareInstallPageTest testPage =this.testPageImpl;
	    	if (testPage != null)
	    		testPage.removeDeviceHandler(provider);
    	}
    }
    
    protected boolean leadDeviceHandlerFound = false;
    @Override
    public boolean leadHandlerFound() {
    	return leadDeviceHandlerFound;
    }
    
    protected void addDriverProvider(DriverHandlerProvider provider) {
    	driverProviders.put(provider.id(), provider);
    	synchronized (this) {
    		//No relevance for providers not to be registered by us
    		List<DeviceHandlerProvider<?>> tableProvsLoc = provider.getDeviceHandlerProviders(true);
    		for(DeviceHandlerProvider<?> tableP: tableProvsLoc) {
    			addTableProvider(tableP);
    		}
	    	if(controller != null && controller.deviceConfigPage != null) {
	    		controller.deviceConfigPage.updateTables();
	    	}
    	}
    }
    protected void removeDriverProvider(DriverHandlerProvider provider) {
    	driverProviders.remove(provider.id());
    }

 	void configMenuConfig(MenuConfiguration mc) {
		mc.setCustomNavigation(menu);
		mc.setLanguageSelectionVisible(false);
		mc.setNavigationVisible(false); 		
 	}
 	
    protected void addExtProvider(HWInstallExtensionProvider  provider) {
    	controllerProvider.addExtProvider(provider);
    }
    protected void removeExtProvider(HWInstallExtensionProvider provider) {
       	controllerProvider.removeExtProvider(provider);
    }
    
    protected void addDriverPropertyProvider(OGEMADriverPropertyService<?>  provider) {
    	dPropertyProviders.put(provider.id(), provider);
    	if(controller != null)
    		controller.initKnownResources(provider);
    }
    protected void removeDriverPropertyProvider(OGEMADriverPropertyService<?> provider) {
    	dPropertyProviders.remove(provider.id());
    }

    protected void addCCUAccess(CCUAccessI provider) {
    	if(ccuAccess == null) {
    		ccuAccess = provider;
    	}
    }
    protected void removeCCUAccess(CCUAccessI provider) {
       	if(ccuAccess != null && ccuAccess.equals(provider))
    		ccuAccess = null;
    }
    
    protected void addGwSyncAccess(GatewaySyncResourceService provider) {
    	if(gwSync == null) {
    		gwSync = provider;
    	}
    }
    protected void removeGwSyncAccess(GatewaySyncResourceService provider) {
       	if(gwSync != null && gwSync.equals(provider))
       		gwSync = null;
    }
    
    /*protected void addAlarmingStarted(AlarmingStartedService provider) {
    	if(alarmingInternal == null) {
    		alarmingInternal = provider;
    	}
    }
    protected void removeAlarmingStarted(AlarmingStartedService provider) {
       	if(alarmingInternal != null && alarmingInternal.equals(provider))
       		alarmingInternal = null;
    }*/
 }
