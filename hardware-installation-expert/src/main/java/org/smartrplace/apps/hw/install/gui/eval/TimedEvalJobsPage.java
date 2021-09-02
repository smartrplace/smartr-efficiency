package org.smartrplace.apps.hw.install.gui.eval;

import java.util.ArrayList;
import java.util.Collection;

import org.ogema.accessadmin.api.ApplicationManagerPlus;
import org.ogema.devicefinder.api.TimedJobMemoryData;
import org.smartrplace.apps.eval.timedjob.TimedJobConfig;
import org.smartrplace.util.directobjectgui.ObjectResourceGUIHelper;

import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.localisation.OgemaLocale;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.complextable.RowTemplate.Row;

public class TimedEvalJobsPage extends TimedJobsPage {

	public TimedEvalJobsPage(WidgetPage<?> page, ApplicationManagerPlus appManPlus) {
		super(page, appManPlus, true);
	}

	@Override
	protected String getHeader(OgemaLocale locale) {
		return "Evaluation Job Details";
	}
	
	@Override
	public Collection<TimedJobMemoryData> getObjectsInTable(OgemaHttpRequest req) {
		Collection<TimedJobMemoryData> all = dpService.timedJobService().getAllProviders();
		ArrayList<TimedJobMemoryData> result = new ArrayList<>();
		for(TimedJobMemoryData obj: all) {
			if(obj.prov().evalJobType()>0) {
				result.add(obj);
			}
		}
		return result;
	}
	
	@Override
	protected void addWidgetsPlus(final TimedJobMemoryData object, ObjectResourceGUIHelper<TimedJobMemoryData, TimedJobConfig> vh, String id,
			OgemaHttpRequest req, Row row,
			boolean isEval) {
		if(req == null) {
			vh.registerHeaderEntry("Location");
			return;
		}
		if(isEval) {
			vh.stringLabel("Location", id, object.prov().description(req.getLocale()), row);
		}
	}
	
	@Override
	protected String getTypeName(OgemaLocale locale) {
		return "ID";
	}
	
	@Override
	protected String getLabel(TimedJobMemoryData obj, OgemaHttpRequest req) {
		return obj.prov().id();
	}
}
