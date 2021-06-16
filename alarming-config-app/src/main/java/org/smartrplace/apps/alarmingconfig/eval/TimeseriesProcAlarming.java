package org.smartrplace.apps.alarmingconfig.eval;

import java.util.List;

import org.ogema.core.application.ApplicationManager;
import org.ogema.core.channelmanager.measurements.SampledValue;
import org.ogema.core.model.units.TemperatureResource;
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
import org.smartrplace.apps.alarmingconfig.model.eval.ThermPlusConfig;

public class TimeseriesProcAlarming extends TimeseriesSimpleProcUtilBase {
	public static final String GAP_EVAL = "GAP_EVAL";
	public static final String OUTVALUE_EVAL = "OUTVALUE_EVAL";
	public static final String SETPREACT_EVAL = "SETPREACT_EVAL";
	public static final String VALUECHANGED_EVAL = "VALUECHANGED_EVAL";
	
	public TimeseriesProcAlarming(ApplicationManager appMan, DatapointService dpService) {
		super(appMan, dpService);
		
		TimeseriesSetProcessor meterProc = new TimeseriesSetProcSingleToSingleArg<Float>(TimeProcUtil.ALARM_GAP_SUFFIX) {
			@Override
			protected List<SampledValue> calculateValues(ReadOnlyTimeSeries timeSeries, long start, long end,
					AggregationMode mode, ProcessedReadOnlyTimeSeries2 newTs2, Float maxGapSizeIn) {
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
		knownProcessors.put(GAP_EVAL, meterProc);
		
		TimeseriesSetProcessor outProc = new TimeseriesSetProcSingleToSingleArg<AlarmConfiguration>(TimeProcUtil.ALARM_OUTVALUE_SUFFIX) {
			@Override
			protected List<SampledValue> calculateValues(ReadOnlyTimeSeries timeSeries, long start, long end,
					AggregationMode mode, ProcessedReadOnlyTimeSeries2 newTs2, AlarmConfiguration param) {
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
		knownProcessors.put(OUTVALUE_EVAL, outProc);
		
		TimeseriesSetProcessor setpProc = new TimeseriesSetProcSingleToSingleArg<SetpReactInput>(TimeProcUtil.ALARM_SETPREACT_SUFFIX) {
			@Override
			protected List<SampledValue> calculateValues(ReadOnlyTimeSeries timeSeries, long start, long end,
					AggregationMode mode, ProcessedReadOnlyTimeSeries2 newTs2, SetpReactInput param) {
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
		knownProcessors.put(SETPREACT_EVAL, setpProc);
		
		TimeseriesSetProcessor valueChangedProc = new TimeseriesSetProcSingleToSingleArg<Float>(TimeProcUtil.ALARM_VALCHANGED_SUFFIX) {
			@Override
			protected List<SampledValue> calculateValues(ReadOnlyTimeSeries timeSeries, long start, long end,
					AggregationMode mode, ProcessedReadOnlyTimeSeries2 newTs2, Float minChange) {
				return TimeSeriesServlet.getValueChanges(timeSeries, start, end, minChange, false);					
			}

			@Override
			protected void alignUpdateIntervalFromSource(DpUpdated updateInterval) {}

			@Override
			public Class<Float> getParamClass() {
				return Float.class;
			}
		};
		knownProcessors.put(VALUECHANGED_EVAL, valueChangedProc);

	}

	public static class SetpReactInput {
		public ReadOnlyTimeSeries setpFb;
		public ThermPlusConfig config;
	}
}
