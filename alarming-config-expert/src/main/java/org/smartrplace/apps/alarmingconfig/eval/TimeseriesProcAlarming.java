package org.smartrplace.apps.alarmingconfig.eval;

import java.util.List;

import org.ogema.core.application.ApplicationManager;
import org.ogema.core.channelmanager.measurements.SampledValue;
import org.ogema.core.timeseries.ReadOnlyTimeSeries;
import org.ogema.devicefinder.api.DatapointInfo.AggregationMode;
import org.ogema.devicefinder.api.DatapointService;
import org.ogema.devicefinder.api.DpUpdateAPI.DpUpdated;
import org.ogema.model.extended.alarming.AlarmConfiguration;
import org.ogema.timeseries.eval.simple.api.ProcessedReadOnlyTimeSeries2;
import org.ogema.timeseries.eval.simple.api.TimeProcUtil;
import org.ogema.timeseries.eval.simple.mon.TimeSeriesServlet;
import org.ogema.timeseries.eval.simple.mon.TimeseriesSetProcSingleToSingleArg;
import org.ogema.timeseries.eval.simple.mon.TimeseriesSetProcessor;
import org.ogema.timeseries.eval.simple.mon.TimeseriesSimpleProcUtilBase;

public class TimeseriesProcAlarming extends TimeseriesSimpleProcUtilBase {
	public static String GAP_EVAL = "GAP_EVAL";
	
	public TimeseriesProcAlarming(ApplicationManager appMan, DatapointService dpService, int updateMode) {
		super(appMan, dpService, updateMode);
		
		TimeseriesSetProcessor meterProc = new TimeseriesSetProcSingleToSingleArg<AlarmConfiguration>("_gaps") {
			
			@Override
			protected List<SampledValue> calculateValues(ReadOnlyTimeSeries timeSeries, long start, long end,
					AggregationMode mode, ProcessedReadOnlyTimeSeries2 newTs2, AlarmConfiguration param) {
				long maxGapSize = (long) (param.maxIntervalBetweenNewValues().getValue()*TimeProcUtil.MINUTE_MILLIS);
				return TimeSeriesServlet.getGaps(timeSeries, start, end, maxGapSize);						
			}

			@Override
			protected void alignUpdateIntervalFromSource(DpUpdated updateInterval) {}

			@Override
			public Class<AlarmConfiguration> getParamClass() {
				return AlarmConfiguration.class;
			}
		};
		knownProcessors.put(GAP_EVAL, meterProc);
	}

}
