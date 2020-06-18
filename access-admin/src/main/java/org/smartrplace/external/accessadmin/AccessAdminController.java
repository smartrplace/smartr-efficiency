package org.smartrplace.external.accessadmin;

import org.ogema.core.application.ApplicationManager;
import org.ogema.core.logging.OgemaLogger;
import org.smartrplace.external.accessadmin.config.AccessAdminConfig;
import org.smartrplace.external.accessadmin.gui.MainPage;

import de.iwes.widgets.api.widgets.WidgetApp;
import de.iwes.widgets.api.widgets.WidgetPage;

// here the controller logic is implemented
public class AccessAdminController {

	public OgemaLogger log;
    public ApplicationManager appMan;

	public AccessAdminConfig appConfigData;
	public AccessAdminApp accessAdminApp;
	
	public MainPage mainPage;
	WidgetApp widgetApp;

	public AccessAdminController(ApplicationManager appMan, WidgetPage<?> page, AccessAdminApp accessAdminApp) {
		this.appMan = appMan;
		this.log = appMan.getLogger();
		this.accessAdminApp = accessAdminApp;
		
		mainPage = new MainPage(page, appMan);

		initConfigurationResource();
        initDemands();
	}

    /*
     * This app uses a central configuration resource, which is accessed here
     */
    private void initConfigurationResource() {
		//TODO provide Util?
		String name = AccessAdminConfig.class.getSimpleName().substring(0, 1).toLowerCase()+AccessAdminConfig.class.getSimpleName().substring(1);
		appConfigData = appMan.getResourceAccess().getResource(name);
		if (appConfigData != null) { // resource already exists (appears in case of non-clean start)
			appMan.getLogger().debug("{} started with previously-existing config resource", getClass().getName());
		}
		else {
			appConfigData = (AccessAdminConfig) appMan.getResourceManagement().createResource(name, AccessAdminConfig.class);
			appConfigData.name().create();
			//TODO provide different sample, provide documentation in code
			appConfigData.name().setValue("sampleName");
			appConfigData.activate(true);
			appMan.getLogger().debug("{} started with new config resource", getClass().getName());
		}
    }
    
    /*
     * register ResourcePatternDemands. The listeners will be informed about new and disappearing
     * patterns in the OGEMA resource tree
     */
    public void initDemands() {
    }

	public void close() {
	}

	/*
	 * if the app needs to consider dependencies between different pattern types,
	 * they can be processed here.
	 */
	public void processInterdependies() {
		// TODO Auto-generated method stub
		
	}
}
