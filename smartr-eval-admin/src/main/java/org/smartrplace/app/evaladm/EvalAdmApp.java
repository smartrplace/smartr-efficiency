package org.smartrplace.app.evaladm;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.References;
import org.apache.felix.scr.annotations.Service;
import org.ogema.core.application.Application;
import org.ogema.core.application.ApplicationManager;
import org.ogema.core.logging.OgemaLogger;
import org.ogema.util.jsonresult.management.api.EvalResultManagement;
import org.smartrplace.app.evaladm.gui.EvalButtonConfigServiceProvider;
import org.smartrplace.app.evaladm.gui.MainPage;
import org.smartrplace.extensionservice.ApplicationManagerSpExtMinimal;
import org.smartrplace.smarteff.access.api.EvalButtonConfigService;

import com.iee.app.evaluationofflinecontrol.OfflineEvalServiceAccessBase;

import de.iwes.timeseries.eval.api.DataProvider;
import de.iwes.timeseries.eval.api.EvaluationProvider;
import de.iwes.timeseries.eval.base.provider.BasicEvaluationProvider;
import de.iwes.timeseries.eval.garo.multibase.GaRoSingleEvalProvider;
import de.iwes.widgets.api.OgemaGuiService;
import de.iwes.widgets.api.services.MessagingService;
import de.iwes.widgets.api.widgets.WidgetApp;
import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.navigation.MenuConfiguration;
import de.iwes.widgets.api.widgets.navigation.NavigationMenu;

/**
 * Template OGEMA application class
 */
@References({
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
@Service(Application.class)
public class EvalAdmApp implements Application, OfflineEvalServiceAccessBase, EvalButtonConfigServiceProvider {
	public static final String urlPath = "/com/example/app/smartrevaladm";

    private OgemaLogger log;
    private ApplicationManager appMan;
    private EvalAdmController controller;

	public WidgetApp widgetApp;
	public NavigationMenu menu;

	@Reference
	private OgemaGuiService guiService;
	
	@Reference
	public EvalResultManagement evalResultMan;
	
	@Reference
	public ApplicationManagerSpExtMinimal appManSpExt;
	//private volatile boolean initDone = false;
	//List<GaRoSingleEvalProvider> earlyProviders = new ArrayList<>();
	
	@Reference(cardinality=ReferenceCardinality.OPTIONAL_UNARY)
	public EvalButtonConfigService evalButtonConfigService;
	
	public MainPage mainPage;
	
	private final Map<String,EvaluationProvider> evaluationProviders = Collections.synchronizedMap(new LinkedHashMap<String,EvaluationProvider>());
	private final Map<String,DataProvider<?>> dataProviders = Collections.synchronizedMap(new LinkedHashMap<String,DataProvider<?>>());
	
	public Map<String, EvaluationProvider> getEvaluations() {
		synchronized (evaluationProviders) {
			return new LinkedHashMap<>(evaluationProviders);
		}
	}
	public Map<String, DataProvider<?>> getDataProviders() {
		synchronized (dataProviders) {
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
        controller = new EvalAdmController(appMan, this);
		
		//register a web page with dynamically generated HTML
		widgetApp = guiService.createWidgetApp(urlPath, appManager);
		WidgetPage<?> page = widgetApp.createStartPage();
		mainPage = new MainPage(page, controller);
		
		menu = new NavigationMenu("Select Page");
		menu.addEntry("Schedule Viewer Control", page);
		
		MenuConfiguration mc = page.getMenuConfiguration();
		mc.setCustomNavigation(menu);
		mc = page.getMenuConfiguration();
		mc.setCustomNavigation(menu);
		controller.registerExistingMultiPages();
		
		//initDone = true;
		//for(GaRoSingleEvalProvider p: earlyProviders) initGaroProvider(p);
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

	public MessagingService messageService() {
		return guiService.getMessagingService();
	}
	@Override
	public EvalButtonConfigService getService() {
		return evalButtonConfigService;
	}
}
