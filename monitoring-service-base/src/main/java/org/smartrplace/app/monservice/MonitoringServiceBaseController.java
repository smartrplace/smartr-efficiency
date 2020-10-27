package org.smartrplace.app.monservice;

import org.ogema.accessadmin.api.ApplicationManagerPlus;
import org.ogema.core.application.ApplicationManager;
import org.ogema.devicefinder.api.DatapointService;
import org.ogema.devicefinder.basedata.DeviceFinderInit;
import org.smartrplace.groupalarm.std.StandardGroupAlarms;

// here the controller logic is implemented
public class MonitoringServiceBaseController {
	public final DatapointService dpService;
	public final ApplicationManager appMan;
	public final MonitoringServiceBaseApp baseApp;
	
	public final StandardGroupAlarms stdGroupAlarms;
	
	public final ApplicationManagerPlus appManPlus;
	
	public MonitoringServiceBaseController(ApplicationManager appMan, MonitoringServiceBaseApp evaluationOCApp) {
		this(appMan, evaluationOCApp, evaluationOCApp.dpService);
	}
	public MonitoringServiceBaseController(ApplicationManager appMan, MonitoringServiceBaseApp evaluationOCApp,
			DatapointService dpService) {
		this.appMan = appMan;
		this.dpService = dpService;
		this.appManPlus = new ApplicationManagerPlus(appMan);
		appManPlus.setDpService(dpService);
		this.baseApp = evaluationOCApp;
		
		this.stdGroupAlarms = new StandardGroupAlarms(appManPlus);
		dpService.alarming().registerAlarmingExtension(stdGroupAlarms);
		
		DeviceFinderInit.initAllDatapoints(appMan, dpService);
	}
	public void close() {
		// TODO Auto-generated method stub
		
	}    
}
