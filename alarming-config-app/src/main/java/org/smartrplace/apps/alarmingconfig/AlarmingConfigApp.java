package org.smartrplace.apps.alarmingconfig;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

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
import org.ogema.devicefinder.api.DatapointService;
import org.ogema.devicefinder.api.DeviceHandlerProvider;
import org.ogema.util.controllerprovider.GenericControllerProvider;
import org.osgi.framework.ServiceRegistration;
import org.smartrplace.hwinstall.basetable.DeviceHandlerAccess;
import org.smartrplace.hwinstall.basetable.HardwareTablePage;

import de.iwes.widgets.api.OgemaGuiService;
import de.iwes.widgets.api.widgets.WidgetApp;
import de.iwes.widgets.api.widgets.navigation.MenuConfiguration;
import de.iwes.widgets.api.widgets.navigation.NavigationMenu;
import de.iwes.widgets.messaging.MessageReader;

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
		name="servletProviders",
		referenceInterface=AlarmingExtensionProvider.class,
		cardinality=ReferenceCardinality.OPTIONAL_MULTIPLE,
		policy=ReferencePolicy.DYNAMIC,
		bind="addExtProvider",
		unbind="removeExtProvider")
})
@Component(specVersion = "1.2", immediate = true)
@Service(Application.class)
public class AlarmingConfigApp implements Application, DeviceHandlerAccess {
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

	//private BundleContext bc;
	protected ServiceRegistration<UserPermissionService> srUserAccService = null;
	
	@Reference
	public UserPermissionService userAccService;

	@Reference
	public DatapointService dpService;
	
	@Reference
	MessageReader mr;

	/*@Activate
	   void activate(BundleContext bc) {
	    this.bc = bc;
	 }*/
	
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
 	
    protected final GenericControllerProvider<AlarmingConfigAppController> controllerProvider;
    public AlarmingConfigApp() {
 		controllerProvider = new GenericControllerProvider<AlarmingConfigAppController>(
 				"org.smartrplace.apps.alarmingconfig.expert.AlarmingConfigAppExpert");
 	}
     protected void addExtProvider(AlarmingExtensionProvider  provider) {
     	controllerProvider.addExtProvider(provider);
     }
     protected void removeExtProvider(AlarmingExtensionProvider provider) {
        	controllerProvider.removeExtProvider(provider);
     }
     
 	private final Map<String, DeviceHandlerProvider<?>> tableProviders = Collections.synchronizedMap(new LinkedHashMap<String,DeviceHandlerProvider<?>>());
 	public Map<String, DeviceHandlerProvider<?>> getTableProviders() {
 		synchronized (tableProviders) {
 			return new LinkedHashMap<>(tableProviders);
 		}
 	}
    protected void addTableProvider(DeviceHandlerProvider<?> provider) {
     	tableProviders.put(provider.id(), provider);
     	synchronized (this) {
 	    	if(controller != null && controller.mainPage != null) {
 	    		controller.deviceOverviewPage.updateTables();
 	    		for(HardwareTablePage mp: controller.mainPageExts) {
 	    			mp.updateTables();
 	    		}
 	    	}
     	}
     }
     protected void removeTableProvider(DeviceHandlerProvider<?> provider) {
     	tableProviders.remove(provider.id());
     }

}
