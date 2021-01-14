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
	public static String OUTVALUE_EVAL = "OUTVALUE_EVAL";
	
	public TimeseriesProcAlarming(ApplicationManager appMan, DatapointService dpService) {
		super(appMan, dpService);
		
		TimeseriesSetProcessor meterProc = new TimeseriesSetProcSingleToSingleArg<AlarmConfiguration>(TimeProcUtil.ALARM_GAP_SUFFIX) {
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
		
		TimeseriesSetProcessor outProc = new TimeseriesSetProcSingleToSingleArg<AlarmConfiguration>(TimeProcUtil.ALARM_OUTVALUE_SUFFIX) {
			@Override
			protected List<SampledValue> calculateValues(ReadOnlyTimeSeries timeSeries, long start, long end,
					AggregationMode mode, ProcessedReadOnlyTimeSeries2 newTs2, AlarmConfiguration param) {
				long maxOutTime = (long) (param.maxViolationTimeWithoutAlarm().getValue()*TimeProcUtil.MINUTE_MILLIS);
				float lowerLimit = param.lowerLimit().getValue();
				float upperLimit = param.upperLimit().getValue();
				return TimeSeriesServlet.getOutValues(timeSeries, start, end, lowerLimit, upperLimit, maxOutTime);						
			}

			@Override
			protected void alignUpdateIntervalFromSource(DpUpdated updateInterval) {}

			@Override
			public Class<AlarmConfiguration> getParamClass() {
				return AlarmConfiguration.class;
			}
		};
		knownProcessors.put(OUTVALUE_EVAL, outProc);
	}

}
