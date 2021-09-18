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
import org.ogema.timeseries.eval.simple.api.TimeseriesSetProcessor3;

public abstract class TimeseriesSetProcSingleToSingle3 implements TimeseriesSetProcessor3 {
	protected final Integer absoluteTiming;
	protected final long minIntervalForReCalc;
	public final TimeseriesSimpleProcUtil3 util3;
	
	@Override
	public Integer getAbsoluteTiming() {
		return absoluteTiming;
	}

	@Override
	public TimeseriesSimpleProcUtil3 getUtilProc() {
		return util3;
	}
	
	@Override
	public long getMinIntervalForReCalc() {
		return minIntervalForReCalc;
	}
	
	/** Perform calculation on a certain input series.
	 * 
	 * @param timeSeries input time series
	 * @param start
	 * @param end
	 * @param mode
	 * @param newTs2 This series will contain the result time series, but also has the reference to
	 * 		the input datapoint that can be accessed with {@link ProcessedReadOnlyTimeSeries2#getInputDp()}
	 * @return
	 */
	protected abstract List<SampledValue> calculateValues(ReadOnlyTimeSeries timeSeries, long start,
			long end, AggregationMode mode, ProcessedReadOnlyTimeSeries3 newTs2);
	
	/** change startTime and endTime of parameter if necessary*/
	protected abstract void alignUpdateIntervalFromSource(DpUpdated updateInterval);
	
	/** Overwrite to add dependent time series
	 * @return map ID -> datapoint containing dependent time series. The ID can be used to obtain
	 * the dependent timeseries via {@link ProcessedReadOnlyTimeSeries3#getDependentTimeseries(String)}
	 * for usage of the timeseries and to provide to the calculation algorithm.*/
	protected Map<String, Datapoint> addDependetTimeseries(Datapoint input) {return null;}
	/** Overwrite this if no input timeseries is provided*/
	protected Long getFirstTimestampInSource() {return null;}
		
	/**Overwrite this to load data into the timeseries initially, e.g. by reading
	 * from a file
	 */
	protected void loadInitData2(Datapoint dp) {
		util3.loadInitData3(dp);
	}
	
	//protected TimeSeriesNameProvider nameProvider() {return null;}
	//protected abstract AggregationMode getMode(String tsLabel);
	protected final String labelPostfix;
	
	//public TimeseriesSetProcSingleToSingle3(String labelPostfix, long minIntervalForReCalc) {
	//	this(labelPostfix, null, minIntervalForReCalc);
	//}
	public TimeseriesSetProcSingleToSingle3(String labelPostfix, Integer absoluteTiming, long minIntervalForReCalc,
			TimeseriesSimpleProcUtil3 util3) {
		this.labelPostfix = labelPostfix;
		this.absoluteTiming = absoluteTiming;
		this.minIntervalForReCalc = minIntervalForReCalc;
		this.util3 = util3;
	}
	@Override
	public List<Datapoint> getResultSeries(List<Datapoint> input, boolean registersTimedJob, DatapointService dpService) {
		List<Datapoint> result = new ArrayList<>();
		for(Datapoint tsdi: input) {
			Datapoint newtsdi = getResultSeriesSingle(tsdi, registersTimedJob, dpService);
			result.add(newtsdi);
		}
		return result;
	}

	public Datapoint getResultSeriesSingle(Datapoint tsdi, boolean registersTimedJob, DatapointService dpService) {
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
				return calculateValues(timeSeries, start, end, mode, this);						
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
				TimeseriesSetProcSingleToSingle3.this.alignUpdateIntervalFromSource(updateInterval);
			}

			@Override
			public void loadInitData() {
				if(datapointForChangeNotification == null) {
					getResultSeriesDP(dpService, location);
				}
				TimeseriesSetProcSingleToSingle3.this.loadInitData2(datapointForChangeNotification);
			}
			
			@Override
			protected List<SampledValue> getResultValuesMulti(List<ReadOnlyTimeSeries> timeSeries,
					long start, long end, AggregationMode mode) {
				return null;
			}
			
			@Override
			public Long getFirstTimeStampInSource() {
				Long directVal = TimeseriesSetProcSingleToSingle3.this.getFirstTimestampInSource();
				if(directVal != null)
					return directVal;
				return getFirstTsInSource(input);
			}
		};
		if(deps != null)
			util3.generatedTss.addAll(deps.values());
		resultTs.proc = this;
		newtsdi = getOrUpdateTsDp(location, resultTs , dpService);
		if(registersTimedJob) {
			//throw new UnsupportedOperationException("Own TimedJob for Single2Single not implemented yet!");
			resultTs.timedJob = TimeseriesSetProcMultiToSingle3.registerTimedJob(resultTs, input, resultTs.resultLabel(),
					newtsdi.getLocation(), "S2S", minIntervalForReCalc, dpService);
		}
		return newtsdi;
	}

	/**
	 * 
	 * @param resultLocation result location, usually generated based on tsdi location and postfix
	 * @param tsdi data point used as input
	 * @param resultTs
	 * @param dpService
	 * @return
	 */
	static Datapoint getOrUpdateTsDp(String resultLocation, ProcessedReadOnlyTimeSeries3 resultTs, DatapointService dpService) {
		ProcessedReadOnlyTimeSeries3 newTs2 = null;
		Datapoint newtsdi = null;
		if(dpService != null) {
			//String location = ProcessedReadOnlyTimeSeries2.getDpLocation(tsdi, labelPostfix);
			newtsdi = dpService.getDataPointStandard(resultLocation);
			ReadOnlyTimeSeries dpts = newtsdi.getTimeSeries();
			if((dpts != null) && (dpts instanceof ProcessedReadOnlyTimeSeries3))
				newTs2 = (ProcessedReadOnlyTimeSeries3) dpts; 
		} else
			throw new IllegalStateException("Operation without DatapointService not supported anymore!");
		if(newTs2 == null) {
			newTs2 = resultTs;
			newtsdi = newTs2.getResultSeriesDP(dpService, resultLocation);
		}
		return newtsdi;
	}
	
	public static Long getFirstTsInSource(List<Datapoint> input) {
		long first = Long.MAX_VALUE;
		Long earliestStart = Long.getLong("org.ogema.timeseries.eval.simple.mon3.evalafter");
		for(Datapoint indp: input) {
			ReadOnlyTimeSeries ints = indp.getTimeSeries();
			if(ints == null)
				continue;
			if(ints instanceof ProcessedReadOnlyTimeSeries3) {
				ProcessedReadOnlyTimeSeries3 ints3 = (ProcessedReadOnlyTimeSeries3)ints;
				Long start = ints3.getFirstTimeStampInSource();
				if(start != null && start < first)
					first = start;
			} else {
				SampledValue sv = ints.getNextValue(0);
				if(sv != null && sv.getTimestamp() < first)
					first = sv.getTimestamp();
			}
		}
		if(first == Long.MAX_VALUE)
			return null;
		if(earliestStart != null && first < earliestStart)
			return earliestStart;
		return first;		
	}
}
