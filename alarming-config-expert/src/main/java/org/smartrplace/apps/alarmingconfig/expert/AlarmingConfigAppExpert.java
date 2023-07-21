package org.smartrplace.apps.alarmingconfig.expert;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.ogema.core.application.Application;
import org.ogema.core.application.ApplicationManager;
import org.ogema.timeseries.eval.simple.api.AlarmingStartedService;
import org.ogema.util.controllerprovider.GenericControllerReceiver;
import org.smartrplace.apps.alarmingconfig.AlarmingConfigAppController;
import org.smartrplace.apps.alarmingconfig.AlarmingExtensionProvider;
import org.smartrplace.apps.alarmingconfig.expert.gui.DeviceDetailPageExpert;
import org.smartrplace.apps.alarmingconfig.gui.MainPage.AlarmingServiceProvider;
import org.smartrplace.apps.alarmingconfig.gui.OngoingBaseAlarmsPage;
import org.smartrplace.apps.hw.install.gui.alarm.AlarmingLevelPage;
import org.smartrplace.apps.hw.install.gui.alarm.DevelopmentTaskPage;
import org.smartrplace.apps.hw.install.gui.alarm.DeviceAlarmingPage;
import org.smartrplace.apps.hw.install.gui.alarm.DeviceKnownFaultsInstallationPage;
import org.smartrplace.apps.hw.install.gui.alarm.DeviceKnownFaultsInstallationPage.AlternativeFaultsPageTarget;
import org.smartrplace.apps.hw.install.gui.alarm.DeviceKnownFaultsPage;
import org.smartrplace.apps.hw.install.gui.alarm.DeviceKnownFaultsPage.KnownFaultsPageType;
import org.smartrplace.apps.hw.install.gui.alarm.GatewayMasterDataPage;
import org.smartrplace.apps.hw.install.gui.alarm.MajorKnownFaultsPage;
import org.smartrplace.apps.hw.install.gui.alarm.MessagingAppConfigPage;
import org.smartrplace.apps.hw.install.gui.alarm.ResponsibilityContactsPage;

import de.iwes.util.logconfig.LogHelper;
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
			menu = new NavigationMenu(" Select Page");
			
			/*WidgetPage<?> pageRes2 = widgetApp.createStartPage();
			//WidgetPage<?> pageRes2 = widgetApp.createWidgetPage("userroompermexpert.html");
			new DeviceTypePageExpert(pageRes2, controller.appManPlus, true, controller);
			menu.addEntry("1. Alarming Configuration Per Device", pageRes2);
			configMenuConfig(pageRes2.getMenuConfiguration());*/

			WidgetPage<?> pageRes9 = widgetApp.createWidgetPage("devicealarmoverview.html", false);
			//Resource base = appMan.getResourceAccess().getResource("master");
			DeviceAlarmingPage deviceOverviewPage = new DeviceAlarmingPage(pageRes9, controller); //, base);
			menu.addEntry("1. Device Alarming Overview", pageRes9);
			configMenuConfig(pageRes9.getMenuConfiguration());
			synchronized(controller.mainPageExts) {
				controller.mainPageExts.add(deviceOverviewPage);
			}

			WidgetPage<?> pageRes5 = widgetApp.createWidgetPage("devicedetails.html");
			new DeviceDetailPageExpert(pageRes5, controller.appManPlus, controller, false, false);
			menu.addEntry("2. Alarming Details Per Device", pageRes5);
			configMenuConfig(pageRes5.getMenuConfiguration());

			WidgetPage<?> pageRes11 = widgetApp.createWidgetPage("deviceknownfaultsoperation.html", true);
			synchronized (controller.accessAdminApp) {
				DeviceKnownFaultsPage knownFaultsPage = new DeviceKnownFaultsPage(pageRes11, controller,
						KnownFaultsPageType.OPERATION_STANDARD);
				synchronized(controller.mainPageExts) {
					controller.mainPageExts.add(knownFaultsPage);
				}
			}
			menu.addEntry("3. Device Issue Status", pageRes11);
			configMenuConfig(pageRes11.getMenuConfiguration());

			//if(Boolean.getBoolean("org.smartrplace.app.srcmon.isgateway")) { //&& (!Boolean.getBoolean("org.smartrplace.apps.alarmingconfig.minimalview"))) {
				WidgetPage<?> pageRes10 = widgetApp.createWidgetPage("ongoingbase.html");
				AlarmingServiceProvider serviceProv = new AlarmingServiceProvider() {
					
					@Override
					public AlarmingStartedService getStartedService() {
						return controller.alarmMan;
					}
				};
				new OngoingBaseAlarmsPage(pageRes10, controller.appManPlus, serviceProv); //, base);
				menu.addEntry("4. Active Alarms", pageRes10);
				configMenuConfig(pageRes10.getMenuConfiguration());
			//}

			WidgetPage<?> pageRes12 = widgetApp.createWidgetPage("devicedevsettings.html");
			new DevelopmentTaskPage(pageRes12, controller);
			menu.addEntry("5. Development Special Settings", pageRes12);
			configMenuConfig(pageRes12.getMenuConfiguration());
			
			WidgetPage<?> pageRes6 = widgetApp.createWidgetPage("messagingapps.html");
			new MessagingAppConfigPage(pageRes6, controller);
			menu.addEntry("6. Alarming Messaging App Configuration (Message destination groups)", pageRes6);
			configMenuConfig(pageRes6.getMenuConfiguration());

			WidgetPage<?> pageRes7 = widgetApp.createWidgetPage("escalationproviders.html");
			new AlarmingLevelPage(pageRes7, controller);
			menu.addEntry("7. Alarming Escalation Levels", pageRes7);
			configMenuConfig(pageRes7.getMenuConfiguration());

			WidgetPage<?> pageRes14 = widgetApp.createWidgetPage("deviceknownfaults.html");
			synchronized (controller.accessAdminApp) {
				DeviceKnownFaultsPage knownFaultsPage = new DeviceKnownFaultsPage(pageRes14, controller,
						KnownFaultsPageType.SUPERVISION_STANDARD);
				synchronized(controller.mainPageExts) {
					controller.mainPageExts.add(knownFaultsPage);
				}
			}
			menu.addEntry("8. Device Issue Status Supervision", pageRes14);
			configMenuConfig(pageRes14.getMenuConfiguration());
			
			WidgetPage<?> pageRes16 = widgetApp.createWidgetPage("majorknownfaults.html");
			synchronized (controller.accessAdminApp) {
				MajorKnownFaultsPage knownFaultsPage = new MajorKnownFaultsPage(pageRes16, controller.appManPlus);
				//synchronized(controller.mainPageExts) {
				//	controller.mainPageExts.add(knownFaultsPage);
				//}
				menu.addEntry("8b. Major Device Issues", pageRes16);
				configMenuConfig(pageRes16.getMenuConfiguration());
			}

			WidgetPage<?> pageInstall = widgetApp.createWidgetPage("deviceknownfaultsinstall.html");
			WidgetPage<?> pageOp = widgetApp.createWidgetPage("deviceknownfaultsop.html");
			synchronized (controller.accessAdminApp) {
				DeviceKnownFaultsInstallationPage knownFaultsPageAltInstall = new DeviceKnownFaultsInstallationPage(pageInstall, appMan, controller.accessAdminApp, 
						AlternativeFaultsPageTarget.INSTALLATION);
				DeviceKnownFaultsInstallationPage knownFaultsPageAltOp = new DeviceKnownFaultsInstallationPage(pageOp, appMan, controller.accessAdminApp, 
						AlternativeFaultsPageTarget.OPERATION);
				
				/*
				synchronized(controller.mainPageExts) {
					controller.mainPageExts.add(knownFaultsPage);
				}
				*/
			}
			menu.addEntry("9. Device Issue Status Installation", pageInstall);
			configMenuConfig(pageInstall.getMenuConfiguration());
			menu.addEntry("10. Device Issue Status Operations", pageOp);
			configMenuConfig(pageOp.getMenuConfiguration());
			
			final WidgetPage<?> pageResponsibleContacts = widgetApp.createWidgetPage("responsiblecontacts.html");
			new ResponsibilityContactsPage(pageResponsibleContacts, appMan);
			menu.addEntry("11. Responsible contacts", pageResponsibleContacts);
			configMenuConfig(pageResponsibleContacts.getMenuConfiguration());
			
			WidgetPage<?> pageRes17 = widgetApp.createWidgetPage("masterdatabase.html");
			new GatewayMasterDataPage(pageRes17, controller.appMan, true);
			menu.addEntry("12. Contact and Installation Master Database", pageRes17);
			configMenuConfig(pageRes17.getMenuConfiguration());

			WidgetPage<?> pageRes18 = widgetApp.createWidgetPage("masterdatabasereadonly.html");
			new GatewayMasterDataPage(pageRes18, controller.appMan, false);
			menu.addEntry("12b. Contact and Installation Master Database (Read Only)", pageRes18);
			configMenuConfig(pageRes18.getMenuConfiguration());

			LogHelper.logStartup(4, appMan);
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
