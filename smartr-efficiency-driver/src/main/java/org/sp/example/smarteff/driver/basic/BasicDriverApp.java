package org.sp.example.smarteff.driver.basic;

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
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.smartrplace.efficiency.api.base.SmartEffExtensionService;
import org.sp.example.samrteff.driver.basic.gui.AdminPage;

import de.iwes.timeseries.eval.api.DataProvider;
import de.iwes.timeseries.eval.garo.api.jaxb.GaRoMultiEvalDataProviderJAXB;
import de.iwes.widgets.api.OgemaGuiService;
import de.iwes.widgets.api.widgets.WidgetApp;
import de.iwes.widgets.api.widgets.WidgetPage;

/**
 * Template OGEMA application class
 */
@References({
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
public class BasicDriverApp implements Application {
	public static final String urlPath = "/org/sp/smarteff/driver";

    private OgemaLogger log;
    //private ApplicationManager appMan;

	private WidgetApp widgetApp;

	@Reference
	private OgemaGuiService guiService;
	
	private final Map<String,DataProvider<?>> dataProviders = Collections.synchronizedMap(new LinkedHashMap<String,DataProvider<?>>());
	
	public Map<String, DataProvider<?>> getDataProviders() {
		synchronized (dataProviders) {
			return new LinkedHashMap<>(dataProviders);
		}
	}
	private GaRoMultiEvalDataProviderJAXB jaxbProvider = null;

	private BundleContext bc;
	protected ServiceRegistration<SmartEffExtensionService> sr = null;

	@Activate
	void activate(BundleContext bc) {
		this.bc = bc;
	}
	 
	/*
     * This is the entry point to the application.
     */
 	@Override
    public void start(ApplicationManager appManager) {

        // Remember framework references for later.
        //appMan = appManager;
        log = appManager.getLogger();
        SPBasicDriverDataService service = new SPBasicDriverDataService(jaxbProvider);
        sr = bc.registerService(SmartEffExtensionService.class, service, null);
        
        // 
		widgetApp = guiService.createWidgetApp(urlPath, appManager);
		WidgetPage<?> page1 = widgetApp.createStartPage();
		new AdminPage(page1, appManager, jaxbProvider);
 	}

     /*
     * Callback called when the application is going to be stopped.
     */
    @Override
    public void stop(AppStopReason reason) {
    	if (sr != null) {
            sr.unregister();
        }
    	if (widgetApp != null) widgetApp.close();
        log.info("{} stopped", getClass().getName());
    }
    
    List<SmartEffExtensionService> providersToProcess = new CopyOnWriteArrayList<>();
     
    protected void addDataProvider(DataProvider<?> provider) {
    	dataProviders.put(provider.id(), provider);
    	if(provider instanceof GaRoMultiEvalDataProviderJAXB) {
    		jaxbProvider = (GaRoMultiEvalDataProviderJAXB) provider;
    	//	if(initDone) initGaroProvider((GaRoSingleEvalProvider) provider);
    	//	else earlyProviders.add((GaRoSingleEvalProvider) provider);
    	}
    }
    protected void removeDataProvider(DataProvider<?> provider) {
    	dataProviders.remove(provider.id());
    	//if(provider instanceof GaRoSingleEvalProvider)
    	//	evalResultMan.getEvalScheduler().unregisterEvaluationProvider((GaRoSingleEvalProvider) provider);
    }
}
