package org.sp.smarteff.monitoring.alarming;


import java.util.List;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.ogema.core.application.Application;
import org.ogema.core.application.ApplicationManager;
import org.ogema.core.logging.OgemaLogger;
import org.ogema.core.model.Resource;
import org.osgi.framework.BundleContext;

import de.iwes.widgets.api.OgemaGuiService;
import de.iwes.widgets.api.widgets.WidgetApp;
import de.iwes.widgets.api.widgets.WidgetPage;
import extensionmodel.smarteff.api.base.SmartEffUserData;
import extensionmodel.smarteff.api.base.SmartEffUserDataNonEdit;

@Component(specVersion = "1.2", immediate = true)
@Service(Application.class)
public class AlarmingConfigApp implements Application {
	public static final String urlPath = "/org/sp/alarming";

    private OgemaLogger log;
    private ApplicationManager appMan;

	private WidgetApp widgetApp;

	@Reference
	private OgemaGuiService guiService;
	

	private BundleContext bc;


    @Activate
    void activate(BundleContext bc) {
    	this.bc = bc;
    }

	/* This is the entry point to the application.
     */
 	@Override
    public void start(ApplicationManager appMan) {

        // Remember framework references for later.
        this.appMan = appMan;
        log = appMan.getLogger();

        // 
		widgetApp = guiService.createWidgetApp(urlPath, appMan);
        
		Resource base = appMan.getResourceAccess().getResource("master");
        WidgetPage alarmConfig = widgetApp.createWidgetPage("alarmConfig.html");
        StandAloneAlarmingEditPage p = new StandAloneAlarmingEditPage(alarmConfig, appMan, null, base);
        System.out.println(alarmConfig.getFullUrl());
 	}

     /*
     * Callback called when the application is going to be stopped.
     */
    @Override
    public void stop(AppStopReason reason) {
    	if (widgetApp != null) widgetApp.close();
        log.info("{} stopped", getClass().getName());
    }
    
   

}