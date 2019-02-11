package org.smartrplace.app.evaladm;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.ogema.core.application.ApplicationManager;
import org.smartrplace.extensionservice.ApplicationManagerSpExtMinimal;

import com.iee.app.evaluationofflinecontrol.OfflineEvaluationControlController;
import com.iee.app.evaluationofflinecontrol.config.KPIPageConfig;
import com.iee.app.evaluationofflinecontrol.gui.KPIPageGWOverviewMultiKPI.PageConfig;

import de.iwes.timeseries.eval.garo.multibase.GaRoSingleEvalProvider;

// here the controller logic is implemented
public class EvalAdmController extends OfflineEvaluationControlController {
    public ApplicationManagerSpExtMinimal appManSpExt;

	public EvalAdmController(ApplicationManager appMan, EvalAdmApp evaluationOCApp) {
		super(appMan, evaluationOCApp, null, new PageConfig(false, false));
		this.appManSpExt = evaluationOCApp.appManSpExt;
	}
    
	@Override
    public void registerExistingMultiPages( ) {
		for(KPIPageConfig item: appConfigData.kpiPageConfigs().getAllElements()) {
			if(!(item.pageId().getValue().equals("luthmonquality") ||
					item.pageId().getValue().equals("sensorquality") ||
					item.pageId().getValue().equals("basichumidity") ||
					item.pageId().getValue().equals("detailhumidity")))
				continue;
			if(item.sourceProviderId().isActive()) {
				GaRoSingleEvalProvider sourceEval = getEvalProvider(item.sourceProviderId().getValue());
				if(sourceEval != null) 
					if(addOrUpdateAndCreatePage(item, sourceEval))
						return;
			}
			if(item.pageId().isActive()) addMultiPage(item);
		}
	}
	
	@Override
	public Set<String> getGatewayIds() {
		Set<String> all = super.getGatewayIds();
		List<String> toRemove = new ArrayList<>();
		for(String a: all) {
			if(!a.startsWith("_18")) toRemove.add(a);
		}
		all.removeAll(toRemove);
		return all;
	}

}
