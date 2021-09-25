package org.smartrplace.external.expert.accessadmin;

import java.util.Arrays;
import java.util.List;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.ogema.accessadmin.api.UserPermissionService;
import org.ogema.accesscontrol.PermissionManager;
import org.ogema.core.application.Application;
import org.ogema.core.application.ApplicationManager;
import org.ogema.util.controllerprovider.GenericControllerReceiver;
import org.smartrplace.external.accessadmin.AccessAdminController;
import org.smartrplace.external.accessadmin.AccessAdminExtensionProvider;
import org.smartrplace.external.accessadmin.expert.gui.UserRoomGroupPermissionPageExpert;
import org.smartrplace.external.accessadmin.gui.UserStatusPermissionPage;

import de.iwes.widgets.api.OgemaGuiService;
import de.iwes.widgets.api.widgets.WidgetApp;
import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.localisation.OgemaLocale;
import de.iwes.widgets.api.widgets.navigation.MenuConfiguration;
import de.iwes.widgets.api.widgets.navigation.NavigationMenu;

/**
 * Template OGEMA application class
 */
@Component(specVersion = "1.2", immediate = true)
@Service({Application.class, AccessAdminExtensionProvider.class})
public class AccessAdminAppExpert implements Application, AccessAdminExtensionProvider {
	public static final String urlPath = "/org/smartrplace/external/accessadminexpert";

 	public WidgetApp widgetApp;
	public NavigationMenu menu;

	@Reference
	private OgemaGuiService guiService;

	@Reference
	public PermissionManager permMan;

	
    /*
     * This is the entry point to the application.
     */
 	@Override
    public void start(ApplicationManager appManager) {

        // Remember framework references for later.
		controllerRecv.setAppman(appManager);
     }

	GenericControllerReceiver<AccessAdminController> controllerRecv =
			new GenericControllerReceiver<AccessAdminController>() {

		@Override
		protected void controllerAndAppmanAvailable(AccessAdminController controller,
				ApplicationManager appMan) {
			//register a web page with dynamically generated HTML
			widgetApp = guiService.createWidgetApp(urlPath, appMan);
			//final WidgetPage<?> page = widgetApp.createStartPage();
			menu = new NavigationMenu("Select Page");
			
			WidgetPage<?> pageRes6 = widgetApp.createWidgetPage("userstatus.html", true);
			new UserStatusPermissionPage(pageRes6, controller) {
				@Override
				protected String getHeader(OgemaLocale locale) {
					return "1. User App Mapping (Superadmin)";
				}
				
				@Override
				protected List<String> getPermissionNames() {
					return Arrays.asList(UserPermissionService.APP_ACCESS_PERMISSIONS_FOR_SUPERADMIN);
				}
			};
			menu.addEntry("1. User App Mapping (Superadmin)", pageRes6);
			configMenuConfig(pageRes6.getMenuConfiguration());

			WidgetPage<?> pageRes2 = widgetApp.createWidgetPage("userroomgroupmap.html");
			//WidgetPage<?> pageRes2 = widgetApp.createWidgetPage("userroompermexpert.html");
			new UserRoomGroupPermissionPageExpert(pageRes2, controller);
			menu.addEntry("4. User - Room Group Mapping", pageRes2);
			configMenuConfig(pageRes2.getMenuConfiguration());
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
	public void setController(AccessAdminController controller) {
 		controllerRecv.setController(controller);
	}


}
