package org.sp.smarteff.monitoring.kpireporting;

import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.ogema.model.jsonresult.MultiKPIEvalConfiguration;
import org.ogema.tools.resource.util.ValueResourceUtils;
import org.ogema.util.kpieval.provider.EvalProviderMessagingBase;
import org.smartrplace.app.monbase.MonitoringController;

import com.iee.app.evaluationofflinecontrol.OfflineEvaluationControlController;

import de.iwes.timeseries.eval.api.EvaluationProvider;
import de.iwes.timeseries.eval.garo.multibase.GaRoSingleEvalProvider;
import de.iwes.util.resource.ValueResourceHelper;

@Service(EvaluationProvider.class)
@Component
public class EvalProviderMonitoringBase extends EvalProviderMessagingBase {
	public static EvalProviderMonitoringBase instance = null;
	protected static List<KPIPageDefinitionWithEmail> pagesWithEmails = new ArrayList<>();
	
	/** Adapt these values to your provider*/
    public final static String ID = "default_monitoringbase_provider";
    public final static String LABEL = "Monitoring base KPI page and message registration";
    public final static String DESCRIPTION = "Monitoring base: Registering KPI pages with messages";

    public static MonitoringController controller;
    
    public EvalProviderMonitoringBase() {
		super(ID, LABEL, DESCRIPTION);
		if(instance == null)
			instance = this;
	}

 	@Override
	public List<KPIPageDefinitionWithEmail> getPages() {
		return pagesWithEmails;
	}

 	public static void addPageWithEmailDefinition(KPIPageDefinitionWithEmail page) {
 		pagesWithEmails.add(page);
 		if(instance != null) {
 			instance.updatePageAndMessageProviders();
 		}
 	}

	@Override
	protected void addOrUpdatePageConfigFromProvider(KPIPageDefinitionWithEmail def, GaRoSingleEvalProvider eval) {
		controller.addOrUpdatePageConfigFromProvider(def.kpiPageDefinition, eval);
		
		MultiKPIEvalConfiguration configLoc = controller.getOrCreateEvalConfig(
				EvalProviderMessagingBase.DEFAULT_QUALITY_EVALPROVIDER_ID,
				null, ChronoUnit.DAYS, null, false,
				OfflineEvaluationControlController.APP_AUTO_SUBID);
		if(def.gatewayIdsToEvaluate != null) {
			for(String gw: def.gatewayIdsToEvaluate) {
				ValueResourceUtils.appendValueIfUnique(configLoc.gwIds(), gw, true);
				ValueResourceUtils.appendValueIfUnique(controller.appConfigData.gatewaysToUse(), gw, true);
				ValueResourceUtils.appendValueIfUnique(controller.appConfigData.gatewaysToShow(), gw, true);
			}
			ValueResourceHelper.setCreate(configLoc.performAutoQueuing(), true);
		}
		
	}
}
