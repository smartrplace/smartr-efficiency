package org.smartrplace.apps.alarmingconfig.expert;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.ogema.core.application.Application;
import org.ogema.core.application.ApplicationManager;
import org.ogema.util.controllerprovider.GenericControllerReceiver;
import org.smartrplace.apps.alarmingconfig.AlarmingConfigAppController;
import org.smartrplace.apps.alarmingconfig.AlarmingExtensionProvider;
import org.smartrplace.apps.alarmingconfig.expert.evalgui.DeviceEvaluationPage;
import org.smartrplace.apps.alarmingconfig.expert.gui.DeviceDetailPageEval;
import org.smartrplace.apps.alarmingconfig.expert.gui.DeviceDetailPageExpert;
import org.smartrplace.apps.alarmingconfig.expert.gui.DeviceTypePageExpert;
import org.smartrplace.apps.alarmingconfig.expert.gui.MainPageExpert;
import org.smartrplace.apps.alarmingconfig.gui.AlarmGroupPage;
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
			
			WidgetPage<?> pageRes2 = widgetApp.createStartPage();
			//WidgetPage<?> pageRes2 = widgetApp.createWidgetPage("userroompermexpert.html");
			new DeviceTypePageExpert(pageRes2, controller.appManPlus, true, controller);
			menu.addEntry("1. Alarming Configuration Per Device", pageRes2);
			configMenuConfig(pageRes2.getMenuConfiguration());

			WidgetPage<?> pageRes5 = widgetApp.createWidgetPage("devicedetails.html");
			new DeviceDetailPageExpert(pageRes5, controller.appManPlus, controller, false);
			menu.addEntry("2. Alarming Details Per Device", pageRes5);
			configMenuConfig(pageRes5.getMenuConfiguration());

			WidgetPage<?> pageRes3 = widgetApp.createWidgetPage("mainexpert.html");
			new MainPageExpert(pageRes3, controller.appManPlus);
			menu.addEntry("3. Alarming Configuration Details", pageRes3);
			configMenuConfig(pageRes3.getMenuConfiguration());
			
			WidgetPage<?> pageRes6 = widgetApp.createWidgetPage("devicedetailevals.html");
			new DeviceDetailPageEval(pageRes6, controller.appManPlus, controller);
			menu.addEntry("4. Alarming Evaluations Per Device", pageRes6);
			configMenuConfig(pageRes6.getMenuConfiguration());

			WidgetPage<?> pageRes4 = widgetApp.createWidgetPage("ongoinggroups.html");
			new AlarmGroupPage(pageRes4, controller.appManPlus);
			menu.addEntry("5. Groups of ongoing Alarms", pageRes4);
			configMenuConfig(pageRes4.getMenuConfiguration());
			
			WidgetPage<?> pageRes11 = widgetApp.createWidgetPage("deviceknownfaults.html");
			synchronized (controller.accessAdminApp) {
				DeviceKnownFaultsPage knownFaultsPage = new DeviceKnownFaultsPage(pageRes11, controller);
				synchronized(controller.mainPageExts) {
					controller.mainPageExts.add(knownFaultsPage);
				}
			}
			menu.addEntry("6. Device Issue Status", pageRes11);
			configMenuConfig(pageRes11.getMenuConfiguration());

			WidgetPage<?> pageRes12 = widgetApp.createWidgetPage("deviceevalpage.html");
			synchronized (controller.accessAdminApp) {
				DeviceEvaluationPage evalPage = new DeviceEvaluationPage(pageRes12, controller);
				synchronized(controller.mainPageExts) {
					controller.mainPageExts.add(evalPage);
				}
			}
			menu.addEntry("7. Thermostat Plus Evaluation Page", pageRes12);
			configMenuConfig(pageRes12.getMenuConfiguration());
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
