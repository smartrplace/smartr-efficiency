package org.smartrplace.app.evaladm.gui;

import java.util.ArrayList;
import java.util.List;

import org.ogema.core.application.ApplicationManager;
import org.ogema.externalviewer.extensions.IntervalConfiguration;
import org.ogema.externalviewer.extensions.ScheduleViewerOpenButtonEval;
import org.smartrplace.smarteff.access.api.EvalButtonConfig;

import com.iee.app.evaluationofflinecontrol.util.ScheduleViewerConfigProvEvalOff;

import de.iwes.timeseries.eval.api.TimeSeriesData;
import de.iwes.timeseries.eval.api.extended.util.TimeSeriesDataExtendedImpl;
import de.iwes.timeseries.eval.base.provider.utils.TimeSeriesDataImpl;
import de.iwes.util.timer.AbsoluteTimeHelper;
import de.iwes.util.timer.AbsoluteTiming;
import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.reswidget.scheduleviewer.utils.ScheduleViewerUtil;

public class EvalButtonConfigured extends ScheduleViewerOpenButtonEval {
	private static final long serialVersionUID = 1L;

	protected final EvalButtonConfigServiceProvider service;
	protected final int evalConfigIdx;
	protected final ApplicationManager appMan;
	
	public EvalButtonConfigured(WidgetPage<?> page, String widgetId,
			EvalButtonConfigServiceProvider service, int evalConfigIdx, ApplicationManager appMan) {
		super(page, widgetId, "--", ScheduleViewerConfigProvEvalOff.PROVIDER_ID,
				ScheduleViewerConfigProvEvalOff.getInstance());
		this.service = service;
		this.evalConfigIdx = evalConfigIdx;
		this.appMan = appMan;
	}
	
	@Override
	public void onGET(OgemaHttpRequest req) {
		if(service.getService() == null) {
			setWidgetVisibility(false, req);
			return;
		}
		List<EvalButtonConfig> bcList = service.getService().configurations();
		if(bcList.size() <= evalConfigIdx) {
			setWidgetVisibility(false, req);
		} else  {
			setText(bcList.get(evalConfigIdx).buttonText(), req);
			setWidgetVisibility(true, req);			
		}
	}

	@Override
	protected List<TimeSeriesData> getTimeseries(OgemaHttpRequest req) {
		EvalButtonConfig bc = service.getService().configurations().get(evalConfigIdx);
		List<TimeSeriesData> tsdList = new ArrayList<>();
		//int index = 0;
		for(TimeSeriesData ts: bc.timeSeriesToOpen()) {
			if(ts.label(req.getLocale()) == null) {
				if(ts instanceof TimeSeriesDataImpl) {
					String label = ScheduleViewerUtil.getScheduleShortName(((TimeSeriesDataImpl) ts).getTimeSeries(), appMan.getResourceAccess());
					ts = new TimeSeriesDataExtendedImpl((TimeSeriesDataImpl) ts, label, label);
				}
			}
			tsdList.add(ts);
			//TimeSeriesDataImpl tsd = new TimeSeriesDataImpl(ts, label, label, null);
			//TimeSeriesDataExtendedImpl tsExt = new TimeSeriesDataExtendedImpl(tsd, null, null);
			//tsExt.addProperty("deviceName", di.getDeviceName());
			//tsExt.addProperty("deviceResourceLocation", di.getDeviceResourceLocation());
			//tsdList.add(tsExt);
			//index++;
		}
		return tsdList;
	}

	@Override
	protected String getEvaluationProviderId(OgemaHttpRequest req) {
		EvalButtonConfig bc = service.getService().configurations().get(evalConfigIdx);
		return bc.buttonText();
	}

	@Override
	protected IntervalConfiguration getITVConfiguration(OgemaHttpRequest req) {
		EvalButtonConfig bc = service.getService().configurations().get(evalConfigIdx);
		IntervalConfiguration itv = bc.getDefaultInterval();
		if(itv != null) return itv;
		itv = new IntervalConfiguration();
		itv.end = appMan.getFrameworkTime();
		itv.start = AbsoluteTimeHelper.getIntervalStart(itv.end, AbsoluteTiming.DAY);
		itv.start = AbsoluteTimeHelper.addIntervalsFromAlignedTime(itv.start, -1, AbsoluteTiming.DAY);
		return itv;
	}

}
