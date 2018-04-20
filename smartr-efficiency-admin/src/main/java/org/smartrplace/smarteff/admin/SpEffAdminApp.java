package org.smartrplace.smarteff.admin;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.References;
import org.apache.felix.scr.annotations.Service;
import org.ogema.core.application.Application;
import org.ogema.core.application.ApplicationManager;
import org.ogema.core.logging.OgemaLogger;
import org.ogema.tools.resourcemanipulator.timer.CountDownDelayedExecutionTimer;
import org.smartrplace.efficiency.api.base.SmartEffExtensionService;
import org.smartrplace.smarteff.util.SPPageUtil;

import de.iwes.timeseries.eval.base.provider.BasicEvaluationProvider;
import de.iwes.widgets.api.OgemaGuiService;
import de.iwes.widgets.api.widgets.WidgetApp;

/**
 * Template OGEMA application class
 */
@References({
	@Reference(
		name="evaluationProviders",
		referenceInterface=SmartEffExtensionService.class,
		cardinality=ReferenceCardinality.OPTIONAL_MULTIPLE,
		policy=ReferencePolicy.DYNAMIC,
		bind="addProvider",
		unbind="removeProvider"),
})
@Component(specVersion = "1.2", immediate = true)
@Service(Application.class)
public class SpEffAdminApp implements Application, ServiceAccess {
	public static final String urlPath = "/org/sp/smarteff/admin";

    private OgemaLogger log;
    private ApplicationManager appMan;
    private SpEffAdminController controller;
    private volatile boolean initDone = false;

	private WidgetApp widgetApp;

	@Reference
	private OgemaGuiService guiService;
	
	/*public ServicePage mainPage;
	public ServiceDetailPage offlineEvalPage;
	public ResTypePage resTypePage;
	public DataExplorerPage dataExPage;
	public NaviOverviewPage naviPage;
	public NavigationMenu menu;*/
	
	private final Map<String,SmartEffExtensionService> evaluationProviders = Collections.synchronizedMap(new LinkedHashMap<String,SmartEffExtensionService>());
	public Map<String, SmartEffExtensionService> getEvaluations() {
		return evaluationProviders;
	}

	public BasicEvaluationProvider basicEvalProvider = null;


	/*
     * This is the entry point to the application.
     */
 	@Override
    public void start(ApplicationManager appManager) {

        // Remember framework references for later.
        appMan = appManager;
        log = appManager.getLogger();

        // 
		widgetApp = guiService.createWidgetApp(urlPath, appManager);
        controller = new SpEffAdminController(appMan, this, widgetApp);
		
		//register a web page with dynamically generated HTML
		/*WidgetPage<?> page = widgetApp.createStartPage();
		mainPage = new ServicePage(page, controller);
		WidgetPage<?> pageDetails = widgetApp.createWidgetPage("Details.html");
		offlineEvalPage = new ServiceDetailPage(pageDetails, controller);
		WidgetPage<?> pageResTypes = widgetApp.createWidgetPage("resTypes.html");
		SmartrEffExtResourceTypeData rtd = new SmartrEffExtResourceTypeData(BaseDataService.BUILDING_DATA, null, null);
		resTypePage = new ResTypePage(pageResTypes, controller, rtd );
		WidgetPage<?> page3 = widgetApp.createWidgetPage("dataExplorer.html");
		dataExPage = new DataExplorerPage(page3, controller, controller.getUserAdmin().getAppConfigData().globalData());
		WidgetPage<?> pageNavis = widgetApp.createWidgetPage("naviOverview.html");
		NavigationPageData navi = new NavigationPageData(BaseDataService.BUILDING_NAVI_PROVIDER, null, "", null);
		naviPage = new NaviOverviewPage(pageNavis, controller, navi);

		menu = new NavigationMenu("Select Page");
		menu.addEntry("Services Overview Page", page);
		menu.addEntry("Services Details Page", pageDetails);
		menu.addEntry("Data Types", pageResTypes);
		//menu.addEntry("Data Explorer", page3);
		menu.addEntry("Navigation Pages", pageNavis);
		
		MenuConfiguration mc = page.getMenuConfiguration();
		mc.setCustomNavigation(menu);
		mc = pageDetails.getMenuConfiguration();
		mc.setCustomNavigation(menu);
		mc = pageResTypes.getMenuConfiguration();
		mc.setCustomNavigation(menu);
		mc = page3.getMenuConfiguration();
		mc.setCustomNavigation(menu);
		mc = pageNavis.getMenuConfiguration();
		mc.setCustomNavigation(menu);*/

		initDone = true;
        controller.processOpenServices();
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
    
    List<SmartEffExtensionService> providersToProcess = new CopyOnWriteArrayList<>();

    protected void addProvider(SmartEffExtensionService provider) {
    	evaluationProviders.put(SPPageUtil.buildId(provider), provider);
    	// Execute in main application thread
    	if(initDone) {
    		new CountDownDelayedExecutionTimer(appMan, 1) {
				@Override
				public void delayedExecution() {
					controller.processNewService(provider);
				}
    		};
		} else
			providersToProcess.add(provider);
    }
    
    protected void removeProvider(SmartEffExtensionService provider) {
    	evaluationProviders.remove(SPPageUtil.buildId(provider));
    	// Execute in main application thread
    	if(controller != null) new CountDownDelayedExecutionTimer(appMan, 1) {
			
			@Override
			public void delayedExecution() {
				controller.unregisterService(provider);			}
		};
    }
}
