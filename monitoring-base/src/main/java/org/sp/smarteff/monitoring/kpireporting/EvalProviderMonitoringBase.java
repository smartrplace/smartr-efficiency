package org.sp.smarteff.monitoring.kpireporting;

import java.util.ArrayList;
import java.util.List;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.ogema.util.kpieval.provider.EvalProviderMessagingBase;

import de.iwes.timeseries.eval.api.EvaluationProvider;

//@Service(EvaluationProvider.class)
//@Component
public class EvalProviderMonitoringBase extends EvalProviderMessagingBase {
	protected static List<KPIPageDefinitionWithEmail> pagesWithEmails = new ArrayList<>();
	
	/** Adapt these values to your provider*/
    public final static String ID = "default_monitoringbase_provider";
    public final static String LABEL = "Monitoring base KPI page and message registration";
    public final static String DESCRIPTION = "Monitoring base: Registering KPI pages with messages";

    public EvalProviderMonitoringBase() {
		super(ID, LABEL, DESCRIPTION);
	}

 	@Override
	protected List<KPIPageDefinitionWithEmail> getPages() {
		return pagesWithEmails;
	}

 	public static void addPageWithEmailDefinition(KPIPageDefinitionWithEmail page) {
 		pagesWithEmails.add(page);
 	}
}
