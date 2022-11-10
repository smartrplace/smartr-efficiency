package org.smartrplace.external.accessadmin;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.References;
import org.apache.felix.scr.annotations.Service;
import org.ogema.accessadmin.api.UserPermissionService;
import org.ogema.accesscontrol.PermissionManager;
import org.ogema.core.application.Application;
import org.ogema.core.application.ApplicationManager;
import org.ogema.core.logging.OgemaLogger;
import org.ogema.util.controllerprovider.GenericControllerProvider;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

import de.iwes.widgets.api.OgemaGuiService;
import de.iwes.widgets.api.widgets.WidgetApp;
import de.iwes.widgets.api.widgets.navigation.MenuConfiguration;
import de.iwes.widgets.api.widgets.navigation.NavigationMenu;

/**
 * Template OGEMA application class
 */
@References({
	@Reference(
		name="servletProviders",
		referenceInterface=AccessAdminExtensionProvider.class,
		cardinality=ReferenceCardinality.OPTIONAL_MULTIPLE,
		policy=ReferencePolicy.DYNAMIC,
		bind="addExtProvider",
		unbind="removeExtProvider")
})@Component(specVersion = "1.2", immediate = true)
@Service(Application.class)
public class AccessAdminApp implements Application {
	public static final String urlPath = "/org/smartrplace/external/accessadmin";

    private OgemaLogger log;
    private ApplicationManager appMan;
    private AccessAdminController controller;

	public WidgetApp widgetApp;
	public NavigationMenu menu;

	@Reference
	private OgemaGuiService guiService;

	@Reference
	public PermissionManager permMan;

	private BundleContext bc;
	protected ServiceRegistration<UserPermissionService> srUserAccService = null;
	UserPermissionServiceImpl userAccService;

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
		//final WidgetPage<?> page = widgetApp.createStartPage();
		menu = new NavigationMenu("Select Page");

        controller = new AccessAdminController(appMan, this);
        userAccService = new UserPermissionServiceImpl(controller);
	    controller.userPermService = userAccService;
	    controller.appManPlus.setUserPermService(userAccService);
        srUserAccService = bc.registerService(UserPermissionService.class, userAccService, null);

		controllerProvider.setController(controller);
		
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
	
    protected final GenericControllerProvider<AccessAdminController> controllerProvider;
    public AccessAdminApp() {
		controllerProvider = new GenericControllerProvider<AccessAdminController>(
				"org.smartrplace.external.expert.accessadmin.AccessAdminAppExpert");
	}
    protected void addExtProvider(AccessAdminExtensionProvider  provider) {
    	controllerProvider.addExtProvider(provider);
    }
    protected void removeExtProvider(AccessAdminExtensionProvider provider) {
       	controllerProvider.removeExtProvider(provider);
    }

}
