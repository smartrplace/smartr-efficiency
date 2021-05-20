package org.smartrplace.apps.alarmingconfig.expert;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.ogema.core.application.Application;
import org.ogema.core.application.ApplicationManager;
import org.ogema.util.controllerprovider.GenericControllerReceiver;
import org.smartrplace.apps.alarmingconfig.AlarmingConfigAppController;
import org.smartrplace.apps.alarmingconfig.AlarmingExtensionProvider;
import org.smartrplace.apps.alarmingconfig.expert.gui.DeviceDetailPageExpert;
import org.smartrplace.apps.alarmingconfig.gui.OngoingBaseAlarmsPage;
import org.smartrplace.apps.hw.install.gui.alarm.DeviceAlarmingPage;
import org.smartrplace.apps.hw.install.gui.alarm.DeviceKnownFaultsPage;

import de.iwes.widgets.api.OgemaGuiService;
import de.iwes.widgets.api.widgets.WidgetApp;
import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.navigation.MenuConfiguration;
import de.iwes.widgets.api.widgets.navigation.NavigationMenu;

/**
 * Template OGEMA application class
 */
@Component(specVersion = "1.2", immediate = true)
@Service({Application.class, AlarmingExtensionProvider.class})
public class AlarmingConfigAppExpert implements Application, AlarmingExtensionProvider {
	public static final String urlPath = "/org/smartrplace/alarmingexpert";

	public WidgetApp widgetApp;
	public NavigationMenu menu;

	@Reference
	public OgemaGuiService guiService;

 	@Override
    public void start(ApplicationManager appManager) {

        // Remember framework references for later.
		controllerRecv.setAppman(appManager);
     }

	GenericControllerReceiver<AlarmingConfigAppController> controllerRecv =
			new GenericControllerReceiver<AlarmingConfigAppController>() {

		@Override
		protected void controllerAndAppmanAvailable(AlarmingConfigAppController controller,
				ApplicationManager appMan) {
			//register a web page with dynamically generated HTML
			widgetApp = guiService.createWidgetApp(urlPath, appMan);
			//final WidgetPage<?> page = widgetApp.createStartPage();
			menu = new NavigationMenu("Select Page");
			
			/*WidgetPage<?> pageRes2 = widgetApp.createStartPage();
			//WidgetPage<?> pageRes2 = widgetApp.createWidgetPage("userroompermexpert.html");
			new DeviceTypePageExpert(pageRes2, controller.appManPlus, true, controller);
			menu.addEntry("1. Alarming Configuration Per Device", pageRes2);
			configMenuConfig(pageRes2.getMenuConfiguration());*/

			WidgetPage<?> pageRes9 = widgetApp.createWidgetPage("devicealarmoverview.html", true);
			//Resource base = appMan.getResourceAccess().getResource("master");
			DeviceAlarmingPage deviceOverviewPage = new DeviceAlarmingPage(pageRes9, controller); //, base);
			menu.addEntry("1. Device Alarming Overview", pageRes9);
			configMenuConfig(pageRes9.getMenuConfiguration());
			synchronized(controller.mainPageExts) {
				controller.mainPageExts.add(deviceOverviewPage);
			}

			//Note: There is only an Expert version of the page that contains resource location information
			//TODO: Provide version without this here and move this version to superadmin
			WidgetPage<?> pageRes5 = widgetApp.createWidgetPage("devicedetails.html");
			new DeviceDetailPageExpert(pageRes5, controller.appManPlus, controller, false);
			menu.addEntry("2. Alarming Details Per Device Expert", pageRes5);
			configMenuConfig(pageRes5.getMenuConfiguration());

			WidgetPage<?> pageRes11 = widgetApp.createWidgetPage("deviceknownfaults.html");
			synchronized (controller.accessAdminApp) {
				DeviceKnownFaultsPage knownFaultsPage = new DeviceKnownFaultsPage(pageRes11, controller);
				synchronized(controller.mainPageExts) {
					controller.mainPageExts.add(knownFaultsPage);
				}
			}
			menu.addEntry("3. Device Issue Status", pageRes11);
			configMenuConfig(pageRes11.getMenuConfiguration());

			if(Boolean.getBoolean("org.smartrplace.app.srcmon.isgateway")  && (!Boolean.getBoolean("org.smartrplace.apps.alarmingconfig.minimalview"))) {
				WidgetPage<?> pageRes10 = widgetApp.createWidgetPage("ongoingbase.html");
				new OngoingBaseAlarmsPage(pageRes10, controller.appManPlus); //, base);
				menu.addEntry("4. Active Alarms", pageRes10);
				configMenuConfig(pageRes10.getMenuConfiguration());
			}
		}
	};

	/*
     * Callback called when the application is going to be stopped.
     */
    @Override
    public void stop(AppStopReason reason) {
    	if (widgetApp != null) widgetApp.close();
    }
    
 	void configMenuConfig(MenuConfiguration mc) {
		mc.setCustomNavigation(menu);
		mc.setLanguageSelectionVisible(false);
		mc.setNavigationVisible(false); 		
 	}

	@Override
	public void setController(AlarmingConfigAppController controller) {
 		controllerRecv.setController(controller);
	}


 }
