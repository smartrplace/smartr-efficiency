package org.smartrplace.apps.hw.install.gui.eval;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.ogema.accessadmin.api.ApplicationManagerPlus;
import org.ogema.core.channelmanager.measurements.SampledValue;
import org.ogema.devicefinder.api.Datapoint;
import org.ogema.devicefinder.api.TimedJobMemoryData;
import org.ogema.timeseries.eval.simple.api.ProcessedReadOnlyTimeSeries3;
import org.ogema.timeseries.eval.simple.mon3.TimeseriesSetProcSingleToSingle3;
import org.smartrplace.apps.eval.timedjob.TimedJobConfig;
import org.smartrplace.util.directobjectgui.ObjectResourceGUIHelper;

import de.iwes.util.format.StringFormatHelper;
import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.localisation.OgemaLocale;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.complextable.RowTemplate.Row;
import de.iwes.widgets.html.form.label.Label;
import de.iwes.widgets.html.form.label.LabelData;

public class TimedEvalJobsPage extends TimedJobsPage {

	public TimedEvalJobsPage(WidgetPage<?> page, ApplicationManagerPlus appManPlus, TimedJobPageType pageType) {
		super(page, appManPlus, true, pageType);
	}

	@Override
	protected String getHeader(OgemaLocale locale) {
		if(pageType == TimedJobPageType.SPECIAL)
			return "Evaluation Job Details Special Version";
		return "Evaluation Job Details";
	}
	
	@Override
	public Collection<TimedJobMemoryData> getObjectsInTable(OgemaHttpRequest req) {
		Collection<TimedJobMemoryData> all = dpService.timedJobService().getAllProviders();
		//List<TimedJobMemoryData> result = filterDrop.getFiltered(all, req);
		ArrayList<TimedJobMemoryData> result = new ArrayList<>();
		for(TimedJobMemoryData obj: all) {
			if((obj.prov().evalJobType() > 0) && filterDrop.isInSelection(obj, req)) {
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
			if(TimedJobPageType.SPECIAL.equals(pageType)) {
				vh.registerHeaderEntry("#Add-Dps expected");
				vh.registerHeaderEntry("#Add-Dps Processed");
				vh.registerHeaderEntry("ResultSize");
				vh.registerHeaderEntry("ResultEnd");
			}
			vh.registerHeaderEntry("Location");
			return;
		}
		int dpLocationError = 0;
		String location2Print = object.prov().description(req.getLocale());
		if(TimedJobPageType.SPECIAL.equals(pageType)) {
			ProcessedReadOnlyTimeSeries3 pts3 = object.prov().getEvaluationContext();
			if(pts3 != null) {
				String text;
				if(pts3.proc != null && (pts3.proc instanceof TimeseriesSetProcSingleToSingle3)) {
					int num = ((TimeseriesSetProcSingleToSingle3)pts3.proc).getDependentTimeseriesNum(pts3.datapointForChangeNotification);
					text = ""+num;
					vh.stringLabel("#Add-Dps expected", id, text, row);
				}
			
				Collection<Datapoint> depts = pts3.getAllDependentTimeseries();
				if(depts == null)
					text = "n/a";
				else
					text = ""+depts.size();
				vh.stringLabel("#Add-Dps Processed", id, text, row);
				if(pts3.datapointForChangeNotification == null)
					dpLocationError = 1;
				else
					dpLocationError = ((pts3.datapointForChangeNotification.getLocation() != location2Print)?2:0);
				
				List<SampledValue> vals = pts3.getValuesInternal();
				if(vals == null) {
					vh.stringLabel("ResultSize", id, "--", row);
				} else {
					int size = vals.size();
					vh.stringLabel("ResultSize", id, ""+size, row);
					if(size > 0) {
						String endText = StringFormatHelper.getFullTimeDateInLocalTimeZone(vals.get(size-1).getTimestamp());
						vh.stringLabel("ResultEnd", id, endText, row);
					}
				}
			}
		}

		//if(isEval) {
		Label loclLb = vh.stringLabel("Location", id, location2Print, row);
		switch(dpLocationError) {
		case 1:
			loclLb.addDefaultStyle(LabelData.BOOTSTRAP_ORANGE);
			break;
		case 2:
			loclLb.addDefaultStyle(LabelData.BOOTSTRAP_RED);
		}
		//}
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
