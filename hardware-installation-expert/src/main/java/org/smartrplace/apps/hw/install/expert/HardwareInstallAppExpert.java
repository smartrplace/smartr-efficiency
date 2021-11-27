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

import java.util.ArrayList;
import java.util.Collection;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.ogema.core.application.Application;
import org.ogema.core.application.ApplicationManager;
import org.ogema.core.logging.OgemaLogger;
import org.ogema.devicefinder.api.DeviceHandlerProvider;
import org.ogema.devicefinder.api.TimedJobMemoryData;
import org.ogema.util.controllerprovider.GenericControllerReceiver;
import org.smartrplace.alarming.extension.BatteryAlarmingExtension;
import org.smartrplace.apps.hw.install.HWInstallExtensionProvider;
import org.smartrplace.apps.hw.install.HardwareInstallController;
import org.smartrplace.apps.hw.install.config.InstallAppDevice;
import org.smartrplace.apps.hw.install.gui.DeviceConfigPage;
import org.smartrplace.apps.hw.install.gui.DeviceTypeConfigPage;
import org.smartrplace.apps.hw.install.gui.MainPage.ShowModeHw;
import org.smartrplace.apps.hw.install.gui.eval.TimedEvalJobsPage;
import org.smartrplace.apps.hw.install.gui.eval.TimedJobsPage;
import org.smartrplace.apps.hw.install.gui.expert.BatteryPage;
import org.smartrplace.apps.hw.install.gui.expert.ConfigurationPageChartExport;
import org.smartrplace.apps.hw.install.gui.expert.ConfigurationPageHWInstall;
import org.smartrplace.apps.hw.install.gui.expert.MainPageExpert;
import org.smartrplace.apps.hw.install.gui.expert.MainPageExpertProps;
import org.smartrplace.apps.hw.install.gui.expert.MainPageExpertTrash;
import org.smartrplace.apps.hw.install.gui.expert.ThermostatPage;
import org.smartrplace.apps.hw.install.gui.expert.WindowStatusPage;
import org.smartrplace.apps.hw.install.gui.prop.DriverPropertyPageAll;
import org.smartrplace.apps.hw.install.gui.prop.PropertyPage;

import de.iwes.util.logconfig.LogHelper;
import de.iwes.widgets.api.OgemaGuiService;
import de.iwes.widgets.api.widgets.WidgetApp;
import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.localisation.LocaleDictionary;
import de.iwes.widgets.api.widgets.localisation.OgemaLocale;
import de.iwes.widgets.api.widgets.navigation.NavigationMenu;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;

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
			int showModeOrder = controller.appConfigData.showModePageOrder().getValue();
			
			final NavigationMenu menu = new NavigationMenu(" Browse pages");

			widgetApp = guiService.createWidgetApp(urlPath, appMan);
			final WidgetPage<?> page = widgetApp.createStartPage();
			MainPageExpert expertPage = new MainPageExpert(page, controller, (showModeOrder == 0)?ShowModeHw.STANDARD:ShowModeHw.KNI);
			controller.mainPageExts.add(expertPage);
			menu.addEntry(expertPage.getHeader(), page);
			page.getMenuConfiguration().setCustomNavigation(menu);
			
			widgetApp = guiService.createWidgetApp(urlPath, appMan);
			final WidgetPage<?> pageExp2 = widgetApp.createWidgetPage("mainExpert2.html");
			MainPageExpert expertPage2 = new MainPageExpert(pageExp2, controller, (showModeOrder != 0)?ShowModeHw.STANDARD:ShowModeHw.KNI);
			controller.mainPageExts.add(expertPage2);
			menu.addEntry(expertPage2.getHeader(), pageExp2);
			pageExp2.getMenuConfiguration().setCustomNavigation(menu);
			
			widgetApp = guiService.createWidgetApp(urlPath, appMan);
			final WidgetPage<?> pageExp3 = widgetApp.createWidgetPage("mainExpertNetwork.html");
			MainPageExpert expertPage3 = new MainPageExpert(pageExp3, controller, ShowModeHw.NETWORK) {
				protected boolean showOnlyBaseColsHWT() {
					return true;
				}
			};
			controller.mainPageExts.add(expertPage3);
			menu.addEntry(expertPage3.getHeader(), pageExp3);
			pageExp3.getMenuConfiguration().setCustomNavigation(menu);

			for(InstallAppDevice dev: controller.appConfigData.knownDevices().getAllElements()) {
				if(dev.isTrash().getValue()) {
					DeviceHandlerProvider<?> devHand = controller.getDeviceHandler(dev);
					expertPage.performTrashOperation(dev, devHand);
				}
			}

			
			batAlarmExt = new BatteryAlarmingExtension(appMan);
			controller.dpService.alarming().registerAlarmingExtension(batAlarmExt);

			//WidgetPage<LocaleDictionary> rssiPageBase = widgetApp.createWidgetPage("rssipage.hmtl");
			//new RSSIPage(rssiPageBase, controller);

			//menu.addEntry("Communication quality page", rssiPageBase);
			//page.getMenuConfiguration().setCustomNavigation(menu);
			//rssiPageBase.getMenuConfiguration().setCustomNavigation(menu);

			WidgetPage<?> configPagebase = widgetApp.createWidgetPage("configPage.hmtl");
			new ConfigurationPageHWInstall(configPagebase, controller);
			menu.addEntry("Configuration Page", configPagebase);
			configPagebase.getMenuConfiguration().setCustomNavigation(menu);
			
			WidgetPage<?> configChartExport = widgetApp.createWidgetPage("configChartExport.hmtl");
			new ConfigurationPageChartExport(configChartExport, controller);
			menu.addEntry("Chart Export Configuration Page", configChartExport);
			configChartExport.getMenuConfiguration().setCustomNavigation(menu);

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

			WidgetPage<?> thermWindowPage = widgetApp.createWidgetPage("thermostatWindows.hmtl");
			WindowStatusPage twpage = new WindowStatusPage(thermWindowPage, controller);
			menu.addEntry(twpage.getHeader(), thermWindowPage);
			thermWindowPage.getMenuConfiguration().setCustomNavigation(menu);

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
			new TimedJobsPage(timedJobPage, controller.appManPlus, false);
			menu.addEntry("Evaluation and Timed Jobs Overview", timedJobPage);
			timedJobPage.getMenuConfiguration().setCustomNavigation(menu);

			WidgetPage<?> timedJobPage2 = widgetApp.createWidgetPage("timedjobsnoneval.hmtl");
			new TimedJobsPage(timedJobPage2, controller.appManPlus, false) {
				protected String getHeader(OgemaLocale locale) {
					return "Base Timed Jobs Overview";
				};
				
				public Collection<TimedJobMemoryData> getObjectsInTable(OgemaHttpRequest req) {
					Collection<TimedJobMemoryData> all = dpService.timedJobService().getAllProviders();
					ArrayList<TimedJobMemoryData> result = new ArrayList<>();
					for(TimedJobMemoryData obj: all) {
						if(obj.prov().evalJobType() <= 0) {
							result.add(obj);
						}
					}
					return result;
				};
			};
			menu.addEntry("Base Timed Jobs Overview", timedJobPage2);
			timedJobPage2.getMenuConfiguration().setCustomNavigation(menu);

			WidgetPage<?> timedJobPageEval = widgetApp.createWidgetPage("timedjobseval.hmtl");
			new TimedEvalJobsPage(timedJobPageEval, controller.appManPlus);
			menu.addEntry("Evaluation Jobs Details", timedJobPageEval);
			timedJobPageEval.getMenuConfiguration().setCustomNavigation(menu);

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
