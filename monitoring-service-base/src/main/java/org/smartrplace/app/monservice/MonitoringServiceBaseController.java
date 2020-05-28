package org.smartrplace.app.monservice;

import org.ogema.core.application.ApplicationManager;
import org.ogema.devicefinder.api.DatapointService;
import org.ogema.devicefinder.basedata.DeviceFinderInit;

// here the controller logic is implemented
public class MonitoringServiceBaseController {
	public final DatapointService dpService;
	public final ApplicationManager appMan;
	public final MonitoringServiceBaseApp baseApp;
	
	public MonitoringServiceBaseController(ApplicationManager appMan, MonitoringServiceBaseApp evaluationOCApp) {
		this(appMan, evaluationOCApp, evaluationOCApp.dpService);
	}
	public MonitoringServiceBaseController(ApplicationManager appMan, MonitoringServiceBaseApp evaluationOCApp,
			DatapointService dpService) {
		this.appMan = appMan;
		this.dpService = dpService;
		this.baseApp = evaluationOCApp;
		DeviceFinderInit.getAllDatapoints(appMan, dpService);
	}
	public void close() {
		// TODO Auto-generated method stub
		
	}    
}
