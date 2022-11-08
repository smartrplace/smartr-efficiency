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
import org.ogema.devicefinder.api.DeviceHandlerProviderDP;
import org.ogema.util.controllerprovider.GenericControllerReceiver;
import org.smartrplace.alarming.extension.BatteryAlarmingExtension;
import org.smartrplace.apps.hw.install.HWInstallExtensionProvider;
import org.smartrplace.apps.hw.install.HardwareInstallController;
import org.smartrplace.apps.hw.install.config.InstallAppDevice;
import org.smartrplace.apps.hw.install.gui.DeviceTypeConfigPage;
import org.smartrplace.apps.hw.install.gui.MainPage.ShowModeHw;
import org.smartrplace.apps.hw.install.gui.PreKnownDevicePage;
import org.smartrplace.apps.hw.install.gui.ValveLinkPage;
import org.smartrplace.apps.hw.install.gui.expert.BatteryPage;
import org.smartrplace.apps.hw.install.gui.expert.CCUPage;
import org.smartrplace.apps.hw.install.gui.expert.ConfigurationPageHWInstall;
import org.smartrplace.apps.hw.install.gui.expert.DeviceHandlerPage;
import org.smartrplace.apps.hw.install.gui.expert.MainPageExpertTrash;
import org.smartrplace.apps.hw.install.gui.expert.ThermostatPage;
import org.smartrplace.apps.hw.install.gui.expert.ThermostatPage.ThermostatPageType;
import org.smatrplace.apps.hw.install.gui.mainexpert.MainPageExpert;

import de.iwes.util.logconfig.LogHelper;
import de.iwes.widgets.api.OgemaGuiService;
import de.iwes.widgets.api.widgets.WidgetApp;
import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.navigation.NavigationMenu;

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
			//int showModeOrder = controller.appConfigData.showModePageOrder().getValue();
			
			final NavigationMenu menu = new NavigationMenu(" Browse pages");

			widgetApp = guiService.createWidgetApp(urlPath, appMan);
			final WidgetPage<?> page = widgetApp.createStartPage();
			//MainPageExpert expertPage = new MainPageExpert(page, controller, (showModeOrder == 0)?ShowModeHw.STANDARD:ShowModeHw.KNI);
			MainPageExpert expertPage = new MainPageExpert(page, controller, ShowModeHw.STANDARD);
			controller.mainPageExts.add(expertPage);
			menu.addEntry(expertPage.getHeader(), page);
			page.getMenuConfiguration().setCustomNavigation(menu);
			
			//Clean up trash devices
			for(InstallAppDevice dev: controller.appConfigData.knownDevices().getAllElements()) {
				if(dev.isTrash().getValue()) {
					DeviceHandlerProviderDP<?> devHand = controller.getDeviceHandler(dev);
					expertPage.performTrashOperation(dev, devHand);
				}
			}
			
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
			
			batAlarmExt = new BatteryAlarmingExtension(appMan);
			controller.dpService.alarming().registerAlarmingExtension(batAlarmExt);

			WidgetPage<?> configPagebase = widgetApp.createWidgetPage("configPage.hmtl");
			new ConfigurationPageHWInstall(configPagebase, controller, true);
			menu.addEntry("Configuration Page Basic", configPagebase);
			configPagebase.getMenuConfiguration().setCustomNavigation(menu);
			
			WidgetPage<?> thermPage2 = widgetApp.createWidgetPage("thermostatDetails2.hmtl");
			new ThermostatPage(thermPage2, controller, ThermostatPageType.STANDARD_VIEW_ONLY);
			menu.addEntry("Thermostat Page", thermPage2);
			thermPage2.getMenuConfiguration().setCustomNavigation(menu);

			WidgetPage<?> thermPageAuto = widgetApp.createWidgetPage("thermostatAuto.hmtl");
			new ThermostatPage(thermPageAuto, controller, ThermostatPageType.AUTO_MODE);
			menu.addEntry("Thermostat Auto-Mode and Valve Adapt Management", thermPageAuto);
			thermPageAuto.getMenuConfiguration().setCustomNavigation(menu);

			WidgetPage<?> ccuPage = widgetApp.createWidgetPage("ccutDetails.hmtl");
			new CCUPage(ccuPage, controller);
			menu.addEntry("CCU Details", ccuPage);
			ccuPage.getMenuConfiguration().setCustomNavigation(menu);

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
			
			WidgetPage<?> devHandPage = widgetApp.createWidgetPage("devHands.hmtl");
			DeviceHandlerPage dhpage = new DeviceHandlerPage(devHandPage, controller.appManPlus);
			menu.addEntry(dhpage.getHeader(null), devHandPage);
			devHandPage.getMenuConfiguration().setCustomNavigation(menu);

			if(Boolean.getBoolean("org.smartrplace.apps.hw.install.expert.fal230support")) {
				WidgetPage<?> pageValveLink = widgetApp.createWidgetPage("deviceValveLink.html");
				new ValveLinkPage(pageValveLink, controller.appManPlus);
				menu.addEntry("Link FAL230 valves to wall thermostats", pageValveLink);
				pageValveLink.getMenuConfiguration().setCustomNavigation(menu);
			}

			WidgetPage<?> pagePre = widgetApp.createWidgetPage("devicePreData.html");
			new PreKnownDevicePage(pagePre, controller, false);
			menu.addEntry("Preknown Devices", pagePre);
			pagePre.getMenuConfiguration().setCustomNavigation(menu);
 
			WidgetPage<?> pageHmTeach = widgetApp.createWidgetPage("hmTeach.html");
			new PreKnownDevicePage(pageHmTeach, controller, true);
			menu.addEntry("Homematic Teach-In Page", pageHmTeach);
			pageHmTeach.getMenuConfiguration().setCustomNavigation(menu);

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
