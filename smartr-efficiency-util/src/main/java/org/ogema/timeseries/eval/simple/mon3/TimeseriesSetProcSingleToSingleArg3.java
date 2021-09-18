package org.ogema.timeseries.eval.simple.mon3;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.ogema.core.channelmanager.measurements.SampledValue;
import org.ogema.core.timeseries.ReadOnlyTimeSeries;
import org.ogema.devicefinder.api.Datapoint;
import org.ogema.devicefinder.api.DatapointInfo.AggregationMode;
import org.ogema.devicefinder.api.DatapointService;
import org.ogema.devicefinder.api.DpUpdateAPI.DpUpdated;
import org.ogema.timeseries.eval.simple.api.ProcessedReadOnlyTimeSeries2;
import org.ogema.timeseries.eval.simple.api.ProcessedReadOnlyTimeSeries3;
import org.ogema.timeseries.eval.simple.api.TimeProcPrint;

public abstract class TimeseriesSetProcSingleToSingleArg3<T> extends TimeseriesSetProcSingleToSingle3 implements TimeseriesSetProcessorArg3<T> {
	protected abstract List<SampledValue> calculateValues(ReadOnlyTimeSeries timeSeries, long start,
			long end, AggregationMode mode, ProcessedReadOnlyTimeSeries3 newTs2, T param);

	@Override
	protected List<SampledValue> calculateValues(ReadOnlyTimeSeries timeSeries, long start, long end,
			AggregationMode mode, ProcessedReadOnlyTimeSeries3 newTs2) {
		throw new UnsupportedOperationException("Only variant with parameter object allowed!");

	}

	public TimeseriesSetProcSingleToSingleArg3(String labelPostfix, Integer absoluteTiming, long minIntervalForReCalc,
			TimeseriesSimpleProcUtil3 util3) {
		super(labelPostfix, absoluteTiming, minIntervalForReCalc, util3);
	}

	@Override
	public List<Datapoint> getResultSeries(List<Datapoint> input, DatapointService dpService,
			boolean registersTimedJob, T params) {
		List<Datapoint> result = new ArrayList<>();
		for(Datapoint tsdi: input) {
			Datapoint newtsdi = getResultSeriesSingle(tsdi, registersTimedJob, dpService, params);
			result.add(newtsdi);
		}
		return result;
	}
	
	@Override
	public Datapoint getResultSeriesSingle(Datapoint tsdi, boolean registersTimedJob, DatapointService dpService) {
		throw new IllegalStateException("In Arg3 use only method version with T params!");
	}
	public Datapoint getResultSeriesSingle(Datapoint tsdi, boolean registersTimedJob, DatapointService dpService,
			T params) {
		String location = ProcessedReadOnlyTimeSeries2.getDpLocation(tsdi, labelPostfix);
		Map<String, Datapoint> deps = addDependetTimeseries(tsdi);
		List<Datapoint> input = Arrays.asList(new Datapoint[] {tsdi});
		final Datapoint newtsdi;
		ProcessedReadOnlyTimeSeries3 resultTs = new ProcessedReadOnlyTimeSeries3(tsdi, deps, absoluteTiming) {
			@Override
			protected List<SampledValue> getResultValues(ReadOnlyTimeSeries timeSeries, long start,
					long end, AggregationMode mode) {
SampledValue sv = timeSeries!=null?timeSeries.getPreviousValue(Long.MAX_VALUE):null;
if(Boolean.getBoolean("evaldebug")) System.out.println("Calculate in "+dpLabel()+" lastInput:"+((sv!=null)?TimeProcPrint.getFullTime(sv.getTimestamp()):"no sv"));
				return calculateValues(timeSeries, start, end, mode, this, params);						
			}
			@Override
			protected String getLabelPostfix() {
				return labelPostfix;
			}
			
			@Override
			protected long getCurrentTime() {
				return dpService.getFrameworkTime();
			}
			
			@Override
			protected void alignUpdateIntervalFromSource(DpUpdated updateInterval) {
				TimeseriesSetProcSingleToSingleArg3.this.alignUpdateIntervalFromSource(updateInterval);
			}

			@Override
			public void loadInitData() {
				if(datapointForChangeNotification == null) {
					getResultSeriesDP(dpService, location);
				}
				TimeseriesSetProcSingleToSingleArg3.this.loadInitData2(datapointForChangeNotification);
			}
			
			@Override
			protected List<SampledValue> getResultValuesMulti(List<ReadOnlyTimeSeries> timeSeries,
					long start, long end, AggregationMode mode) {
				return null;
			}
			
			@Override
			public Long getFirstTimeStampInSource() {
				Long directVal = TimeseriesSetProcSingleToSingleArg3.this.getFirstTimestampInSource();
				if(directVal != null)
					return directVal;
				return getFirstTsInSource(input);
			}
		};
		resultTs.proc = this;
		if(deps != null)
			util3.generatedTss.addAll(deps.values());
		newtsdi = getOrUpdateTsDp(location, resultTs , dpService);
		if(registersTimedJob) {
			//throw new UnsupportedOperationException("Own TimedJob for Single2Single not implemented yet!");
			resultTs.timedJob = TimeseriesSetProcMultiToSingle3.registerTimedJob(resultTs, input, resultTs.resultLabel(),
					newtsdi.getLocation(), "S2S", minIntervalForReCalc, dpService);
		}
		return newtsdi;
	}
}
