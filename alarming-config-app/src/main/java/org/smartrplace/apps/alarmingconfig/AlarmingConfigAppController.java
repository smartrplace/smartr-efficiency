package org.smartrplace.apps.alarmingconfig;

import org.ogema.accessadmin.api.ApplicationManagerPlus;
import org.ogema.accessadmin.api.UserPermissionService;
import org.ogema.core.application.ApplicationManager;
import org.ogema.core.logging.OgemaLogger;
import org.smartrplace.apps.alarmingconfig.gui.MainPage;
import org.smartrplace.external.accessadmin.config.AccessAdminConfig;

import de.iwes.widgets.api.widgets.WidgetApp;
import de.iwes.widgets.api.widgets.WidgetPage;

// here the controller logic is implemented
public class AlarmingConfigAppController {

	public OgemaLogger log;
    public ApplicationManager appMan;
    /** This will not be available in the constructor*/
    public UserPermissionService userPermService;
    
	public AccessAdminConfig appConfigData;
	public AlarmingConfigApp accessAdminApp;
    public final ApplicationManagerPlus appManPlus;
	
	public MainPage mainPage;
	WidgetApp widgetApp;

	
	public AlarmingConfigAppController(ApplicationManager appMan, AlarmingConfigApp initApp) {
		this.appMan = appMan;
		this.log = appMan.getLogger();
		this.accessAdminApp = initApp;
		this.userPermService = initApp.userAccService;
		this.appManPlus = new ApplicationManagerPlus(appMan);
		appManPlus.setPermMan(initApp.permMan);
		appManPlus.setUserPermService(userPermService);
		
		WidgetPage<?> pageRes10 = initApp.widgetApp.createWidgetPage("mainpage.html", true);
		mainPage = new MainPage(pageRes10, this);
		initApp.menu.addEntry("Room Setup", pageRes10);
		initApp.configMenuConfig(pageRes10.getMenuConfiguration());

		// TODO: If you need more than one page see how to add more pages as the template commented out below
		// You have to implement a class for each page.
		
		//WidgetPage<?> pageRes11 = initApp.widgetApp.createWidgetPage("usersetup.html", false);
		//mainPage2 = new MainPage2(pageRes11, this);
		//initApp.menu.addEntry("User Setup", pageRes11);
		//initApp.configMenuConfig(pageRes11.getMenuConfiguration());

		initDemands();
	}

     /*
     * register ResourcePatternDemands. The listeners will be informed about new and disappearing
     * patterns in the OGEMA resource tree
     */
    public void initDemands() {
    }

	public void close() {
	}
}
