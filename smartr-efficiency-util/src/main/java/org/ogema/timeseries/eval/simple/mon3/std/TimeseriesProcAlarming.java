package org.ogema.timeseries.eval.simple.mon3.std;

import java.util.List;

import org.ogema.core.application.ApplicationManager;
import org.ogema.core.channelmanager.measurements.SampledValue;
import org.ogema.core.model.units.TemperatureResource;
import org.ogema.core.timeseries.ReadOnlyTimeSeries;
import org.ogema.devicefinder.api.DatapointInfo.AggregationMode;
import org.ogema.devicefinder.api.DatapointService;
import org.ogema.devicefinder.api.DpUpdateAPI.DpUpdated;
import org.ogema.model.extended.alarming.AlarmConfiguration;
import org.ogema.timeseries.eval.simple.api.ProcessedReadOnlyTimeSeries3;
import org.ogema.timeseries.eval.simple.api.TimeProcUtil;
import org.ogema.timeseries.eval.simple.mon.TimeSeriesServlet;
import org.ogema.timeseries.eval.simple.mon3.TimeseriesSetProcSingleToSingleArg3;
import org.ogema.timeseries.eval.simple.mon3.TimeseriesSetProcessor3;
import org.ogema.timeseries.eval.simple.mon3.TimeseriesSimpleProcUtil3;
import org.smartrplace.apps.alarmingconfig.model.eval.ThermPlusConfig;

import de.iwes.util.timer.AbsoluteTiming;

public class TimeseriesProcAlarming extends TimeseriesSimpleProcUtil3 {
	public static final long KPI_UPDATE_RATE_DEFAULT = 60*TimeProcUtil.MINUTE_MILLIS;
	
	public static final String GAP_EVAL = "GAP_EVAL";
	public static final String OUTVALUE_EVAL = "OUTVALUE_EVAL";
	public static final String SETPREACT_EVAL = "SETPREACT_EVAL";
	public static final String VALUECHANGED_EVAL = "VALUECHANGED_EVAL";
	
	public TimeseriesProcAlarming(ApplicationManager appMan, DatapointService dpService) {
		this(appMan, dpService, KPI_UPDATE_RATE_DEFAULT);
	}
	
	public TimeseriesProcAlarming(ApplicationManager appMan, DatapointService dpService, long minIntervalForReCalc) {
		super(appMan, dpService, 2, minIntervalForReCalc);
		
		TimeseriesSetProcessor3 meterProc = new TimeseriesSetProcSingleToSingleArg3<Float>(TimeProcUtil.ALARM_GAP_SUFFIX,
				AbsoluteTiming.WEEK, minIntervalForReCalc, TimeseriesProcAlarming.this) {
			@Override
			protected List<SampledValue> calculateValues(ReadOnlyTimeSeries timeSeries, long start, long end,
					AggregationMode mode, ProcessedReadOnlyTimeSeries3 newTs2, Float maxGapSizeIn) {
				//long maxGapSize = (long) (param.maxIntervalBetweenNewValues().getValue()*TimeProcUtil.MINUTE_MILLIS);
				long maxGapSize = (long) (maxGapSizeIn*TimeProcUtil.MINUTE_MILLIS);
				return TimeSeriesServlet.getGaps(timeSeries, start, end, maxGapSize);					
			}

			@Override
			protected void alignUpdateIntervalFromSource(DpUpdated updateInterval) {}

			@Override
			public Class<Float> getParamClass() {
				return Float.class;
			}
		};
		knownProcessors3.put(GAP_EVAL, meterProc);
		
		TimeseriesSetProcessor3 outProc = new TimeseriesSetProcSingleToSingleArg3<AlarmConfiguration>(
				TimeProcUtil.ALARM_OUTVALUE_SUFFIX, AbsoluteTiming.WEEK, minIntervalForReCalc, TimeseriesProcAlarming.this) {
			@Override
			protected List<SampledValue> calculateValues(ReadOnlyTimeSeries timeSeries, long start, long end,
					AggregationMode mode, ProcessedReadOnlyTimeSeries3 newTs2, AlarmConfiguration param) {
				long maxOutTime = (long) (param.maxViolationTimeWithoutAlarm().getValue()*TimeProcUtil.MINUTE_MILLIS);
				float lowerLimit = param.lowerLimit().getValue();
				float upperLimit = param.upperLimit().getValue();
				if(param.sensorVal() instanceof TemperatureResource) {
					lowerLimit += 273.15;
					upperLimit += 273.15;
				}
				return TimeSeriesServlet.getOutValues(timeSeries, start, end, lowerLimit, upperLimit, maxOutTime);						
			}

			@Override
			protected void alignUpdateIntervalFromSource(DpUpdated updateInterval) {}

			@Override
			public Class<AlarmConfiguration> getParamClass() {
				return AlarmConfiguration.class;
			}
		};
		knownProcessors3.put(OUTVALUE_EVAL, outProc);
		
		TimeseriesSetProcessor3 setpProc = new TimeseriesSetProcSingleToSingleArg3<SetpReactInput>(
				TimeProcUtil.ALARM_SETPREACT_SUFFIX, AbsoluteTiming.WEEK, minIntervalForReCalc, TimeseriesProcAlarming.this) {
			@Override
			protected List<SampledValue> calculateValues(ReadOnlyTimeSeries timeSeries, long start, long end,
					AggregationMode mode, ProcessedReadOnlyTimeSeries3 newTs2, SetpReactInput param) {
				long maxReactTime = (long) (param.config.maxSetpointReactionTimeSeconds().getValue()*1000);
				return TimeSeriesServlet.getSensReact(timeSeries, param.setpFb, start, end, maxReactTime);						
			}

			@Override
			protected void alignUpdateIntervalFromSource(DpUpdated updateInterval) {}

			@Override
			public Class<SetpReactInput> getParamClass() {
				return SetpReactInput.class;
			}
		};
		knownProcessors3.put(SETPREACT_EVAL, setpProc);
		
		TimeseriesSetProcessor3 valueChangedProc = new TimeseriesSetProcSingleToSingleArg3<Float>(
				TimeProcUtil.ALARM_VALCHANGED_SUFFIX, AbsoluteTiming.WEEK, minIntervalForReCalc, TimeseriesProcAlarming.this) {
			@Override
			protected List<SampledValue> calculateValues(ReadOnlyTimeSeries timeSeries, long start, long end,
					AggregationMode mode, ProcessedReadOnlyTimeSeries3 newTs2, Float minChange) {
				return TimeSeriesServlet.getValueChanges(timeSeries, start, end, minChange, false);					
			}

			@Override
			protected void alignUpdateIntervalFromSource(DpUpdated updateInterval) {}

			@Override
			public Class<Float> getParamClass() {
				return Float.class;
			}
		};
		knownProcessors3.put(VALUECHANGED_EVAL, valueChangedProc);

	}

	public static class SetpReactInput {
		public ReadOnlyTimeSeries setpFb;
		public ThermPlusConfig config;
	}
}
