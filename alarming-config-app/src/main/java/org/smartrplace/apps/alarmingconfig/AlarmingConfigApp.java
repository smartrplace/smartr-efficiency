package org.smartrplace.apps.alarmingconfig;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.ogema.accessadmin.api.UserPermissionService;
import org.ogema.accesscontrol.PermissionManager;
import org.ogema.core.application.Application;
import org.ogema.core.application.ApplicationManager;
import org.ogema.core.logging.OgemaLogger;
import org.ogema.devicefinder.api.DatapointService;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

import de.iwes.widgets.api.OgemaGuiService;
import de.iwes.widgets.api.widgets.WidgetApp;
import de.iwes.widgets.api.widgets.navigation.MenuConfiguration;
import de.iwes.widgets.api.widgets.navigation.NavigationMenu;
import de.iwes.widgets.messaging.MessageReader;

/**
 * Template OGEMA application class
 */
@Component(specVersion = "1.2", immediate = true)
@Service(Application.class)
public class AlarmingConfigApp implements Application {
	public static final String urlPath = "/org/smartrplace/alarmingconfig";

    private OgemaLogger log;
    private ApplicationManager appMan;
    private AlarmingConfigAppController controller;

	public WidgetApp widgetApp;
	public NavigationMenu menu;

	@Reference
	public OgemaGuiService guiService;

	@Reference
	public PermissionManager permMan;

	private BundleContext bc;
	protected ServiceRegistration<UserPermissionService> srUserAccService = null;
	
	@Reference
	public UserPermissionService userAccService;

	@Reference
	public DatapointService dpService;
	
	@Reference
	MessageReader mr;

	@Activate
	   void activate(BundleContext bc) {
	    this.bc = bc;
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
		menu = new NavigationMenu("Select Page");

        controller = new AlarmingConfigAppController(appMan, this);
        log.info("{} started", getClass().getName());
     }

     /*
     * Callback called when the application is going to be stopped.
     */
    @Override
    public void stop(AppStopReason reason) {
    	if (widgetApp != null) widgetApp.close();
		if (controller != null)
    		controller.close();
        log.info("{} stopped", getClass().getName());
    }
    
 	void configMenuConfig(MenuConfiguration mc) {
		mc.setCustomNavigation(menu);
		mc.setLanguageSelectionVisible(false);
		mc.setNavigationVisible(false); 		
 	}
}
