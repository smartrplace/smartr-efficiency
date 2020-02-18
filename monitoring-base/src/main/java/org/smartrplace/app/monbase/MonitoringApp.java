package org.smartrplace.app.monbase;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.felix.scr.annotations.Reference;
import org.ogema.core.application.Application;
import org.ogema.core.application.ApplicationManager;
import org.ogema.core.logging.OgemaLogger;
import org.ogema.core.model.schedule.Schedule;
import org.ogema.model.connections.ElectricityConnection;
import org.ogema.model.devices.sensoractordevices.SensorDevice;
import org.ogema.model.locations.Room;
import org.ogema.model.sensors.Sensor;
import org.ogema.util.jsonresult.management.api.EvalResultManagement;
import org.smartrplace.app.monbase.gui.OfflineControlGUI;
import org.smartrplace.app.monbase.power.EnergyEvaluationIntervalTable;
import org.smartrplace.app.monbase.power.EnergyEvaluationTable;
import org.smartrplace.extensionservice.SmartEffTimeSeries;
import org.smartrplace.util.format.WidgetPageFormatter;

import com.iee.app.evaluationofflinecontrol.OfflineEvalServiceAccess;
import com.iee.app.evaluationofflinecontrol.gui.MainPage;

import de.iwes.timeseries.eval.api.DataProvider;
import de.iwes.timeseries.eval.api.EvaluationProvider;
import de.iwes.timeseries.eval.api.TimeSeriesData;
import de.iwes.timeseries.eval.base.provider.BasicEvaluationProvider;
import de.iwes.timeseries.eval.garo.multibase.GaRoSingleEvalProvider;
import de.iwes.timeseries.eval.generic.gatewayBackupAnalysis.GatewayBackupAnalysisAccess;
import de.iwes.util.format.StringFormatHelper;
import de.iwes.widgets.api.OgemaGuiService;
import de.iwes.widgets.api.services.MessagingService;
import de.iwes.widgets.api.widgets.WidgetApp;
import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.localisation.OgemaLocale;
import de.iwes.widgets.api.widgets.navigation.MenuConfiguration;
import de.iwes.widgets.api.widgets.navigation.NavigationMenu;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import extensionmodel.smarteff.api.base.SmartEffUserData;
import extensionmodel.smarteff.api.common.BuildingUnit;
import extensionmodel.smarteff.monitoring.AlarmConfigBase;

/**
 * Template OGEMA application class
 */
/*@References({
	@Reference(
		name="evaluationProviders",
		referenceInterface=EvaluationProvider.class,
		cardinality=ReferenceCardinality.OPTIONAL_MULTIPLE,
		policy=ReferencePolicy.DYNAMIC,
		bind="addEvalProvider",
		unbind="removeEvalProvider"),
	@Reference(
		name="dataProviders",
		referenceInterface=DataProvider.class,
		cardinality=ReferenceCardinality.OPTIONAL_MULTIPLE,
		policy=ReferencePolicy.DYNAMIC,
		bind="addDataProvider",
		unbind="removeDataProvider"),
})
@Component(specVersion = "1.2", immediate = true)
@Service(Application.class)*/
public class MonitoringApp implements Application, OfflineEvalServiceAccess {
	public static final String urlPath = "/com/example/app/monapp";
	public static WidgetPageFormatter STANDARD_PAGE_FORMATTER = new WidgetPageFormatter();

    private OgemaLogger log;
    private ApplicationManager appMan;
    private MonitoringController controller;

	public WidgetApp widgetApp;
	public NavigationMenu menu;

	@Reference
	OgemaGuiService guiService;
	
	@Reference
	public EvalResultManagement evalResultMan;
	
	public MainPage mainPage;
	public OfflineControlGUI offlineEval;
	
	private final Map<String,EvaluationProvider> evaluationProviders = Collections.synchronizedMap(new LinkedHashMap<String,EvaluationProvider>());
	private final Map<String,DataProvider<?>> dataProviders = Collections.synchronizedMap(new LinkedHashMap<String,DataProvider<?>>());
	
	public Map<String, EvaluationProvider> getEvaluations() {
		synchronized (evaluationProviders) {
			return new LinkedHashMap<>(evaluationProviders);
		}
	}
	public Map<String, DataProvider<?>> getDataProviders() {
		synchronized (dataProviders) {
			System.out.println("Monitoring-App - Dataproviders: "+StringFormatHelper.getListToPrint(new ArrayList<String>(dataProviders.keySet())));
			return new LinkedHashMap<>(dataProviders);
		}
	}

	public BasicEvaluationProvider basicEvalProvider = null;
	EvaluationProvider eval;
    /*
     * This is the entry point to the application.
     */
 	@Override
    public void start(ApplicationManager appManager) {

        // Remember framework references for later.
        appMan = appManager;
        log = appManager.getLogger();

        // 
       controller = new MonitoringController(appMan, this) {

		@Override
		public String getRoomLabel(String resLocation, OgemaLocale locale) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public String getAllRoomLabel(OgemaLocale locale) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public List<String> getAllRooms(OgemaLocale locale) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public Schedule getManualDataEntrySchedule(String room, String label) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public Map<String, List<String>> getDatatypesBase() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public Map<String, List<String>> getComplexOptions() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public boolean isTimeSeriesInRoom(TimeSeriesData tsdBase, String room) {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public String getAlarmingDomain() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public String getTsName(AlarmConfigBase ac) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public BuildingUnit getBuildingUnitByRoom(Sensor device, Room room, SmartEffUserData user) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public SensorDevice getHeatMeterDevice(String subPath) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public ElectricityConnection getElectrictiyMeterDevice(String subPath) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public List<SmartEffTimeSeries> getManualTimeSeries(BuildingUnit bu) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public String getLabel(AlarmConfigBase ac, boolean isOverall) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		protected void initManualResources() {
			// TODO Auto-generated method stub
			
		}

		@Override
		protected boolean activateAlarming() {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		protected void registerStaticTimeSeriesViewerLinks() {
			// TODO Auto-generated method stub
			
		}

		@Override
		public List<String> getGwIDs(OgemaHttpRequest req) {
			// TODO Auto-generated method stub
			return null;
		}


       };
		
		//register a web page with dynamically generated HTML
		widgetApp = guiService.createWidgetApp(urlPath, appManager);
		WidgetPage<?> page = widgetApp.createStartPage();
		offlineEval = new OfflineControlGUI(page, controller);
		
		menu = new NavigationMenu("Select Page");
		menu.addEntry("Chart-Konfiguration", page);
		
		configMenuConfig(page.getMenuConfiguration());

		//Only offer energy pages on gateway
		if(Boolean.getBoolean("org.smartrplace.app.monitoring.gui.offerManualTimeseries")) {
			WidgetPage<?> page3 = widgetApp.createWidgetPage("energyTable.html");
			EnergyEvaluationTable eet = new EnergyEvaluationTable(page3, controller);
			configMenuConfig(page3.getMenuConfiguration());
			WidgetPage<?> page2 = widgetApp.createWidgetPage("energyIntervals.html");
			new EnergyEvaluationIntervalTable(page2, controller, eet);
			menu.addEntry("Energieauswertung", page2);
			configMenuConfig(page2.getMenuConfiguration());
			
			STANDARD_PAGE_FORMATTER.formatPage(page2);
			STANDARD_PAGE_FORMATTER.formatPage(page3);
		}

		controller.registerExistingMultiPages();
		
		//initDone = true;
		//for(GaRoSingleEvalProvider p: earlyProviders) initGaroProvider(p);
	}
 	
 	private void configMenuConfig(MenuConfiguration mc) {
		mc.setCustomNavigation(menu);
		mc.setLanguageSelectionVisible(false);
		mc.setNavigationVisible(false); 		
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
    
    protected void addEvalProvider(EvaluationProvider provider) {
    	if(!(provider.id().equals("extended-quality_eval_provider")
    			|| provider.id().equals("basic-humidity_eval_provider")
//    			|| provider.id().equals("basic-quality_eval_provider")
    			)) return;
    	evaluationProviders.put(provider.id(), provider);
    	if(provider instanceof GaRoSingleEvalProvider) {
    		//if(initDone) initGaroProvider((GaRoSingleEvalProvider) provider);
    		//else earlyProviders.add((GaRoSingleEvalProvider) provider);
    	}
     	if((provider instanceof BasicEvaluationProvider)&&(basicEvalProvider == null)) {
    		basicEvalProvider = (BasicEvaluationProvider) provider;
    	}
    }
    
    /*private void initGaroProvider(GaRoSingleEvalProvider provider) {
   		evalResultMan.getEvalScheduler().registerEvaluationProvider(provider);
    }*/
    
    protected void removeEvalProvider(EvaluationProvider provider) {
    	evaluationProviders.remove(provider.id());
    	/*try {
    	if(provider instanceof GaRoSingleEvalProvider)
    		evalResultMan.getEvalScheduler().unregisterEvaluationProvider((GaRoSingleEvalProvider) provider);
    	} catch(NullPointerException e) {
    		//ignore
    	}*/
    }
    
    protected void addDataProvider(DataProvider<?> provider) {
    	dataProviders.put(provider.id(), provider);
    	//if(provider instanceof GaRoMultiEvalDataProvider) {
    	//	if(initDone) initGaroProvider((GaRoSingleEvalProvider) provider);
    	//	else earlyProviders.add((GaRoSingleEvalProvider) provider);
    	//}
    }
    protected void removeDataProvider(DataProvider<?> provider) {
    	dataProviders.remove(provider.id());
    	//if(provider instanceof GaRoSingleEvalProvider)
    	//	evalResultMan.getEvalScheduler().unregisterEvaluationProvider((GaRoSingleEvalProvider) provider);
    }
    
	@Override
	public WidgetApp getWidgetApp() {
		return widgetApp;
	}
	@Override
	public NavigationMenu getMenu() {
		return menu;
	}
	@Override
	public EvalResultManagement evalResultMan() {
		return evalResultMan;
	}
	@Override
	public GatewayBackupAnalysisAccess gatewayParser() {
		return null;
		//return gatewayParser;
	}

	public MessagingService messageService() {
		return guiService.getMessagingService();
	}
}
