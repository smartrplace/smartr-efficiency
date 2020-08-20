package org.smartrplace.apps.hw.install.gui.expert;

import java.util.Collection;
import java.util.List;

import org.ogema.externalviewer.extensions.IntervalConfiguration;
import org.ogema.externalviewer.extensions.ScheduleViewerOpenButton;
import org.ogema.externalviewer.extensions.ScheduleViewerOpenButtonEval;

import de.iwes.timeseries.eval.api.TimeSeriesData;
import de.iwes.widgets.api.widgets.OgemaWidget;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.reswidget.scheduleviewer.api.ScheduleViewerConfiguration;
import de.iwes.widgets.reswidget.scheduleviewer.api.ScheduleViewerConfigurationBuilder;
import de.iwes.widgets.reswidget.scheduleviewer.api.TimeSeriesFilter;

public class ScheduleViwerOpenTemp {
	private static final String fixedEvalProviderId = "extended-quality_eval_provider";
	public static interface SchedOpenDataProvider {
		List<TimeSeriesData> getData(OgemaHttpRequest req);
		IntervalConfiguration getITVConfiguration();		
	}
	
	//public static ScheduleViewerOpenButton getScheduleViewerOpenButton(WidgetPage<?> page, String widgetId,
	//		final SchedOpenDataProvider provider) {
	//	
	//}
	public static ScheduleViewerOpenButton getScheduleViewerOpenButton(OgemaWidget parent, String widgetId,
			String text, final SchedOpenDataProvider provider, OgemaHttpRequest req) {
		ScheduleViewerOpenButtonEval schedOpenButtonEval = new ScheduleViewerOpenButtonEval(parent, widgetId, text,
				ScheduleViewerConfigProvHWI.PROVIDER_ID,
				ScheduleViewerConfigProvHWI.getInstance(), req) {
			private static final long serialVersionUID = 1L;

			@Override
			protected ScheduleViewerConfiguration getViewerConfiguration(long startTime, long endTime,
					List<Collection<TimeSeriesFilter>> programs) {
				final ScheduleViewerConfiguration viewerConfiguration =
						ScheduleViewerConfigurationBuilder.newBuilder().setPrograms(programs).
						setStartTime(startTime).setEndTime(endTime).setShowManipulator(true).
						setShowIndividualConfigBtn(false).setShowPlotTypeSelector(true).
						setShowManipulator(false).build();
					return viewerConfiguration;
			}

			@Override
			protected List<TimeSeriesData> getTimeseries(OgemaHttpRequest req) {
				return provider.getData(req);
			}

			@Override
			protected String getEvaluationProviderId(OgemaHttpRequest req) {
				return fixedEvalProviderId;
			}

			@Override
			protected IntervalConfiguration getITVConfiguration(OgemaHttpRequest req) {
				return provider.getITVConfiguration();
			}
		};
		schedOpenButtonEval.setNameProvider(null);
		return schedOpenButtonEval;
	}

}
