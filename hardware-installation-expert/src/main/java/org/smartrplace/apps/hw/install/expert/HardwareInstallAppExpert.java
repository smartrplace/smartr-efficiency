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
package org.smartrplace.apps.hw.install.expert;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.ogema.core.application.Application;
import org.ogema.core.application.ApplicationManager;
import org.ogema.core.logging.OgemaLogger;
import org.ogema.devicefinder.api.DeviceHandlerProvider;
import org.ogema.util.controllerprovider.GenericControllerReceiver;
import org.smartrplace.alarming.extension.BatteryAlarmingExtension;
import org.smartrplace.apps.hw.install.HWInstallExtensionProvider;
import org.smartrplace.apps.hw.install.HardwareInstallController;
import org.smartrplace.apps.hw.install.config.InstallAppDevice;
import org.smartrplace.apps.hw.install.gui.DeviceConfigPage;
import org.smartrplace.apps.hw.install.gui.DeviceTypeConfigPage;
import org.smartrplace.apps.hw.install.gui.eval.TimedJobsPage;
import org.smartrplace.apps.hw.install.gui.expert.BatteryPage;
import org.smartrplace.apps.hw.install.gui.expert.ConfigurationPageHWInstall;
import org.smartrplace.apps.hw.install.gui.expert.MainPageExpert;
import org.smartrplace.apps.hw.install.gui.expert.MainPageExpertProps;
import org.smartrplace.apps.hw.install.gui.expert.MainPageExpertTrash;
import org.smartrplace.apps.hw.install.gui.expert.ThermostatPage;
import org.smartrplace.apps.hw.install.gui.prop.DriverPropertyPageAll;
import org.smartrplace.apps.hw.install.gui.prop.PropertyPage;

import de.iwes.util.logconfig.LogHelper;
import de.iwes.widgets.api.OgemaGuiService;
import de.iwes.widgets.api.widgets.WidgetApp;
import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.localisation.LocaleDictionary;
import de.iwes.widgets.api.widgets.navigation.NavigationMenu;

/**
 * Template OGEMA application class
 */
@Component(specVersion = "1.2", immediate = true)
@Service({Application.class, HWInstallExtensionProvider.class})
public class HardwareInstallAppExpert implements Application, HWInstallExtensionProvider {
	protected OgemaLogger log;
    protected ApplicationManager appMan;
    //protected HardwareInstallController controller;

    protected WidgetApp widgetApp;
	public static final String urlPath = "/org/smartrplace/hardwareinstall/expert";

	@Reference
	private OgemaGuiService guiService;

	public BatteryAlarmingExtension batAlarmExt;
	
	//@Reference
	//DatapointService dpService;
	
	GenericControllerReceiver<HardwareInstallController> controllerRecv =
			new GenericControllerReceiver<HardwareInstallController>() {

		@Override
		protected void controllerAndAppmanAvailable(HardwareInstallController controller,
				ApplicationManager appMan) {
			widgetApp = guiService.createWidgetApp(urlPath, appMan);
			final WidgetPage<?> page = widgetApp.createStartPage();
			MainPageExpert expertPage = new MainPageExpert(page, controller);
			controller.mainPageExts.add(expertPage);
			
			for(InstallAppDevice dev: controller.appConfigData.knownDevices().getAllElements()) {
				if(dev.isTrash().getValue()) {
					DeviceHandlerProvider<?> devHand = controller.handlerByDevice.get(dev.getLocation());
					expertPage.performTrashOperation(dev, devHand);
				}
			}

			
			batAlarmExt = new BatteryAlarmingExtension(appMan);
			controller.dpService.alarming().registerAlarmingExtension(batAlarmExt);

			//WidgetPage<LocaleDictionary> rssiPageBase = widgetApp.createWidgetPage("rssipage.hmtl");
			//new RSSIPage(rssiPageBase, controller);

			final NavigationMenu menu = new NavigationMenu(" Browse pages");
			menu.addEntry("Expert page", page);
			//menu.addEntry("Communication quality page", rssiPageBase);
			page.getMenuConfiguration().setCustomNavigation(menu);
			//rssiPageBase.getMenuConfiguration().setCustomNavigation(menu);

			WidgetPage<?> configPagebase = widgetApp.createWidgetPage("configPage.hmtl");
			new ConfigurationPageHWInstall(configPagebase, controller);
			menu.addEntry("Configuration Page", configPagebase);
			configPagebase.getMenuConfiguration().setCustomNavigation(menu);
			
			WidgetPage<LocaleDictionary> page2 = widgetApp.createWidgetPage("deviceConfig.html");
			new DeviceConfigPage(page2, controller);
			menu.addEntry("Hardware Driver Configuration", page2);
			page2.getMenuConfiguration().setCustomNavigation(menu);

			WidgetPage<?> thermPage = widgetApp.createWidgetPage("thermostatDetails.hmtl");
			new ThermostatPage(thermPage, controller);
			menu.addEntry("Thermostat Debugging", thermPage);
			thermPage.getMenuConfiguration().setCustomNavigation(menu);
			
			WidgetPage<?> batteryPage = widgetApp.createWidgetPage("batteryStates.hmtl");
			new BatteryPage(batteryPage, controller);
			menu.addEntry("Battery Overview", batteryPage);
			batteryPage.getMenuConfiguration().setCustomNavigation(menu);

			WidgetPage<?> trashPage = widgetApp.createWidgetPage("trashDevices.hmtl");
			synchronized(controller.mainPageExts) {
				controller.mainPageExts.add(new MainPageExpertTrash(trashPage, controller));
			}
			menu.addEntry("Trash Devices", trashPage);
			trashPage.getMenuConfiguration().setCustomNavigation(menu);
			
			WidgetPage<?> dpropPage = widgetApp.createWidgetPage("driverPropServices.hmtl");
			new DriverPropertyPageAll(dpropPage, controller);
			menu.addEntry("DriverProperty Services Overivew", dpropPage);
			dpropPage.getMenuConfiguration().setCustomNavigation(menu);

			WidgetPage<?> propResPage = widgetApp.createWidgetPage("driverPropRes.hmtl");
			new PropertyPage(propResPage, controller);
			menu.addEntry("DriverProperties per Resource", propResPage);
			propResPage.getMenuConfiguration().setCustomNavigation(menu);
			
			WidgetPage<?> propDevicePage = widgetApp.createWidgetPage("deviceProperties.hmtl");
			synchronized(controller.mainPageExts) {
				controller.mainPageExts.add(new MainPageExpertProps(propDevicePage, controller));
			}
			menu.addEntry("Device Setup and Installation with Properties",propDevicePage);
			propDevicePage.getMenuConfiguration().setCustomNavigation(menu);

			WidgetPage<?> timedJobPage = widgetApp.createWidgetPage("timedjobs.hmtl");
			new TimedJobsPage(timedJobPage, controller.appManPlus);
			menu.addEntry("Evaluation and Timed Jobs Overview", timedJobPage);
			timedJobPage.getMenuConfiguration().setCustomNavigation(menu);

			WidgetPage<?> page3 = widgetApp.createWidgetPage("deviceTypeConfig.html");
			new DeviceTypeConfigPage(page3, controller);
			menu.addEntry("Device Configuration based on Device Type Database", page3);
			page3.getMenuConfiguration().setCustomNavigation(menu);

	        LogHelper.logStartup(64, appMan);
		}
	};
	
    /*
     * This is the entry point to the application.
     */
 	@Override
    public void start(ApplicationManager appManager) {

        // Remember framework references for later.
        appMan = appManager;
        log = appManager.getLogger();
		controllerRecv.setAppman(appManager);
		//controller = new HardwareInstallControllerExpert(appMan, page, widgetApp, dpService);
     }

     /*
     * Callback called when the application is going to be stopped.
     */
    @Override
    public void stop(AppStopReason reason) {
    	if (widgetApp != null) widgetApp.close();
		//if (controller != null)
    	//	controller.close();
        log.info("{} stopped", getClass().getName());
    }

	@Override
	public void setController(HardwareInstallController controller) {
 		controllerRecv.setController(controller);
	}
}
