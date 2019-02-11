package org.smartrplace.smarteff.stdadmin;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.felix.scr.annotations.Activate;
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
import org.ogema.tools.timeseriesimport.api.TimeseriesImport;
import org.ogema.util.jsonresult.management.api.EvalResultManagement;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.smartrplace.efficiency.api.base.SmartEffExtensionService;
import org.smartrplace.extensionservice.ApplicationManagerSpExtMinimal;
import org.smartrplace.smarteff.access.api.GenericPageConfigurationProvider;
import org.smartrplace.smarteff.admin.ServiceAccess;
import org.smartrplace.smarteff.admin.SpEffAdminController;
import org.smartrplace.smarteff.stdadmin.util.BaseDataServiceAdmin;
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
	@Reference(
			name="pageConfigProviders",
			referenceInterface=GenericPageConfigurationProvider.class,
			cardinality=ReferenceCardinality.OPTIONAL_MULTIPLE,
			policy=ReferencePolicy.DYNAMIC,
			bind="addPageConfigProvider",
			unbind="removePageConfigProvider"),
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
	
	@Reference
	TimeseriesImport csvImport;
	
	@Reference
	public EvalResultManagement evalResultMan;
	@Override
	public EvalResultManagement evalResultMan() {
		return evalResultMan;
	}

	private BundleContext bc;
	protected ServiceRegistration<SmartEffExtensionService> sr = null;
	protected ServiceRegistration<ApplicationManagerSpExtMinimal> srAppManSPMin = null;

    private final Map<String,SmartEffExtensionService> evaluationProviders = Collections.synchronizedMap(new LinkedHashMap<String,SmartEffExtensionService>());
	public Map<String, SmartEffExtensionService> getEvaluations() {
		return evaluationProviders;
	}

	public BasicEvaluationProvider basicEvalProvider = null;

    @Activate
    void activate(BundleContext bc) {
    	this.bc = bc;
    }

	/* This is the entry point to the application.
     */
 	@Override
    public void start(ApplicationManager appManager) {

        // Remember framework references for later.
        appMan = appManager;
        log = appManager.getLogger();

        // 
		widgetApp = guiService.createWidgetApp(urlPath, appManager);
        controller = new SpEffAdminController(appMan, this, widgetApp, csvImport);
		
        BaseDataServiceAdmin dataService = new BaseDataServiceAdmin(controller);
        sr = bc.registerService(SmartEffExtensionService.class, dataService, null);
        srAppManSPMin = bc.registerService(ApplicationManagerSpExtMinimal.class, controller.appManExtMin, null);
	
        initDone = true;
        controller.processOpenServices();
 	}

     /*
     * Callback called when the application is going to be stopped.
     */
    @Override
    public void stop(AppStopReason reason) {
        if (sr != null) {
            sr.unregister();
        }
        if (srAppManSPMin != null) {
            srAppManSPMin.unregister();
        }
    	if (widgetApp != null) widgetApp.close();
		if (controller != null)
    		controller.close();
        log.info("{} stopped", getClass().getName());
    }
    
    private List<SmartEffExtensionService> providersToProcess = new CopyOnWriteArrayList<>();
    private List<GenericPageConfigurationProvider> pageConfigProvidersToProcess = new CopyOnWriteArrayList<>();

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

    private final Map<String,GenericPageConfigurationProvider> pageConfigProviders = Collections.synchronizedMap(new LinkedHashMap<String,GenericPageConfigurationProvider>());
    protected void addPageConfigProvider(GenericPageConfigurationProvider provider) {
    	pageConfigProviders.put(provider.id(), provider);
    	// Execute in main application thread
    	if(initDone) {
    		new CountDownDelayedExecutionTimer(appMan, 1) {
				@Override
				public void delayedExecution() {
					controller.processNewPageConfigService(provider);
				}
    		};
		} else
			pageConfigProvidersToProcess.add(provider);
    }
    
    protected void removePageConfigProvider(GenericPageConfigurationProvider provider) {
    	pageConfigProviders.remove(provider.id());
    	// Execute in main application thread
    	if(controller != null) new CountDownDelayedExecutionTimer(appMan, 1) {
			
			@Override
			public void delayedExecution() {
				controller.unregisterPageProviderService(provider);			}
		};
    }

	@Override
	public Map<String, GenericPageConfigurationProvider> getPageConfigProviders() {
		return pageConfigProviders;
	}

	@Override
	public List<SmartEffExtensionService> providersToProcess() {
		return providersToProcess;
	}

	@Override
	public List<GenericPageConfigurationProvider> pageConfigProvidersToProcess() {
		return pageConfigProvidersToProcess;
	}
}
