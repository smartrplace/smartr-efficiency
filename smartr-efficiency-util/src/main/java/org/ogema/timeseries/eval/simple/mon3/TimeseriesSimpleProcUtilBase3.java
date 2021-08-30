package org.ogema.timeseries.eval.simple.mon3;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.ogema.core.application.ApplicationManager;
import org.ogema.core.channelmanager.measurements.SampledValue;
import org.ogema.core.timeseries.ReadOnlyTimeSeries;
import org.ogema.devicefinder.api.Datapoint;
import org.ogema.devicefinder.api.DatapointService;
import org.ogema.devicefinder.api.DatapointInfo.AggregationMode;
import org.ogema.devicefinder.util.AggregationModeProvider;
import org.ogema.devicefinder.util.DPUtil;
import org.ogema.externalviewer.extensions.ScheduleViewerOpenButtonEval.TimeSeriesNameProvider;
import org.ogema.timeseries.eval.simple.api.ProcessedReadOnlyTimeSeries2;
import org.ogema.timeseries.eval.simple.api.ProcessedReadOnlyTimeSeries3;
import org.ogema.timeseries.eval.simple.mon.TimeseriesSetProcMultiToSingle;
import org.ogema.timeseries.eval.simple.mon.TimeseriesSimpleProcUtilBase;
import org.smartrplace.tsproc.persist.TsProcPersistUtil;

import de.iwes.timeseries.eval.api.TimeSeriesData;

public abstract class TimeseriesSimpleProcUtilBase3 implements TimeseriesSimpleProcUtilBase {
	protected final Map<String, TimeseriesSetProcessor3> knownProcessors3 = new HashMap<>();
	
	protected final Set<Datapoint> generatedTss = new HashSet<>();
	
	protected final ApplicationManager appMan;
	public final DatapointService dpService;

	/**Overwrite this to load data into the timeseries initially by a different mechanism than standard file reading
	 */
	public void loadInitData3(Datapoint dp) {
		TsProcPersistUtil.importTsFromFile(dp, appMan);		
	}
	public void saveData(Datapoint dp) {
		TsProcPersistUtil.saveTsToFile(dp, appMan);		
	}
	
	public TimeseriesSetProcessor3 getProcessor3(String procID) {
		return knownProcessors3.get(procID);
	}
	
	public TimeseriesSimpleProcUtilBase3(ApplicationManager appMan, DatapointService dpService) {
		//super(appMan, dpService);
		this.appMan = appMan;
		this.dpService = dpService;
		TimeseriesSetProcMultiToSingle3.aggregateLog = TimeseriesSetProcMultiToSingle.aggregateLog;
		TimeseriesSetProcMultiToSingle3.tsSingleLog = TimeseriesSetProcMultiToSingle.tsSingleLog;
	}
	
	@Override
	public List<Datapoint> process(String tsProcessRequest, List<Datapoint> input) {
		TimeseriesSetProcessor3 proc = knownProcessors3.get(tsProcessRequest);
		if(proc == null)
			throw new IllegalArgumentException("Unknown timeseries processor: "+tsProcessRequest);
		List<Datapoint> result = proc.getResultSeries(input, true, dpService);
		generatedTss.addAll(result);
		return result;
	}
	
	@Override
	public <T> Datapoint processSingle(String tsProcessRequest, Datapoint dp, T params) {
		TimeseriesSetProcessor3 procRaw = getProcessor3(tsProcessRequest);
		if(procRaw == null || (!(procRaw instanceof TimeseriesSetProcessorArg3)))
			throw new IllegalArgumentException("Unknown or unfitting timeseries processor: "+tsProcessRequest);
		@SuppressWarnings("unchecked")
		TimeseriesSetProcessorArg3<T> proc = (TimeseriesSetProcessorArg3<T>) procRaw;
		List<Datapoint> resultTs = proc.getResultSeries(Arrays.asList(new Datapoint[] {dp}), dpService, true, params);
		if(resultTs != null && !resultTs.isEmpty()) {
			generatedTss.addAll(resultTs);
			return resultTs.get(0);
		}
		return null;
	}

	/** Regarding calculation notes see {@link TimeseriesSetProcMultiToSingle3}
	 * 
	 * @param tsProcessRequest
	 * @param input
	 * @return
	 */
	@Override
	public Datapoint processMultiToSingle(String tsProcessRequest, List<Datapoint> input) {
		TimeseriesSetProcessor3 proc = knownProcessors3.get(tsProcessRequest);
		if(proc == null)
			throw new IllegalArgumentException("Unknown timeseries processor: "+tsProcessRequest);
		List<Datapoint> resultTs = proc.getResultSeries(input, true, dpService);
		if(resultTs != null && !resultTs.isEmpty()) {
			Datapoint result = resultTs.get(0);
			generatedTss.add(result);
			return result;
		}
		return null;
	}
	
	@Override
	public Datapoint processSingle(String tsProcessRequest, Datapoint dp) {
		TimeseriesSetProcessor3 proc = getProcessor3(tsProcessRequest);
		if(proc == null)
			throw new IllegalArgumentException("Unknown timeseries processor: "+tsProcessRequest);
		List<Datapoint> resultTs = proc.getResultSeries(Arrays.asList(new Datapoint[] {dp}), true, dpService);
		if(resultTs != null && !resultTs.isEmpty()) {
			Datapoint result = resultTs.get(0);
			generatedTss.add(result);
			return result;
		}
		return null;
	}
	
	/** Variant of {@link #processSingle(String, Datapoint)} allowing to pass an object containing parameters
	 * that shall be processed
	 * @param tsProcessRequest must reference to a TimeseriesSetProcessorArg (taking argument object)
	 * @param dp
	 * @param params
	 * @return
	 */
	//TODO: Add support
	/*public <T> Datapoint processSingle(String tsProcessRequest, Datapoint dp, T params) {
		TimeseriesSetProcessor3 procRaw = getProcessor3(tsProcessRequest);
		if(procRaw == null || (!(procRaw instanceof TimeseriesSetProcessorArg)))
			throw new IllegalArgumentException("Unknown or unfitting timeseries processor: "+tsProcessRequest);
		@SuppressWarnings("unchecked")
		TimeseriesSetProcessorArg<T> proc = (TimeseriesSetProcessorArg<T>) procRaw;
		List<Datapoint> resultTs = proc.getResultSeries(Arrays.asList(new Datapoint[] {dp}), true, dpService, params);
		if(resultTs != null && !resultTs.isEmpty())
			return resultTs.get(0);
		return null;
	}*/

	/** For processing of two or more aligned time series that are not interchangeable like difference, division,...*/
	@Override
	public Datapoint processArgs(String tsProcessRequest, Datapoint... dp) {
		TimeseriesSetProcessor3 proc = getProcessor3(tsProcessRequest);
		if(proc == null)
			throw new IllegalArgumentException("Unknown timeseries processor: "+tsProcessRequest);
		List<Datapoint> resultTs = proc.getResultSeries(Arrays.asList(dp), true, dpService);
		if(resultTs != null && !resultTs.isEmpty()) {
			Datapoint result = resultTs.get(0);
			generatedTss.add(result);
			return result;
		}
		return null;
	}

	@Override
	public List<Datapoint> processSingleToMulti(String tsProcessRequest, Datapoint dp) {
		List<Datapoint> result = new ArrayList<>();
		TimeseriesSetProcessor3 proc = getProcessor3(tsProcessRequest);
		if(proc == null)
			throw new IllegalArgumentException("Unknown timeseries processor: "+tsProcessRequest);
		List<Datapoint> resultTs = proc.getResultSeries(Arrays.asList(new Datapoint[] {dp}), true, dpService);
		if(resultTs != null && !resultTs.isEmpty())
			result.addAll(resultTs);
		generatedTss.addAll(result);
		return result;
	}

	@Deprecated //Should not be used anymore
	public List<TimeSeriesData> processTSD(String tsProcessRequest, List<TimeSeriesData> input,
			TimeSeriesNameProvider nameProvider, AggregationModeProvider aggProv) {
		return DPUtil.getTSList(process(tsProcessRequest, DPUtil.getDPList(input, nameProvider, aggProv)), null);
	}
	
	public void saveUpdatesForAllData() {
		for(Datapoint dp: generatedTss) {
			saveData(dp);
		}
	}
	/** Get MemoryTimeseries without TimedJob. Note that such a timeseries is not calculated automatically but new
	 * time stamps need to be added directly via {@link ProcessedReadOnlyTimeSeries3#addValuesPublic(List)}. Such
	 * a timeseries will also not store any values persistently.*/
	public static Datapoint getMemoryTimeseriesDatapointWithoutTimedJobSingle(Datapoint inputForNameAndAggregationMode,
			String labelPostfix, DatapointService dpService) {
		String location = ProcessedReadOnlyTimeSeries2.getDpLocation(inputForNameAndAggregationMode, labelPostfix);
		ProcessedReadOnlyTimeSeries3 ts = new ProcessedReadOnlyTimeSeries3(inputForNameAndAggregationMode) {
			
			@Override
			protected long getCurrentTime() {
				return dpService.getFrameworkTime();
			}
			
			@Override
			protected List<SampledValue> getResultValuesMulti(List<ReadOnlyTimeSeries> timeSeries, long start, long end,
					AggregationMode mode) {
				throw new IllegalStateException("Not usable via timedJob!");
			}
			
			@Override
			protected List<SampledValue> getResultValues(ReadOnlyTimeSeries timeSeries, long start, long end,
					AggregationMode mode) {
				throw new IllegalStateException("Not usable via timedJob!");
			}
			
			@Override
			public Long getFirstTimeStampInSource() {
				throw new IllegalStateException("Not usable via timedJob!");
			}
		};
		Datapoint result = dpService.getDataPointStandard(location);
		result.setTimeSeries(ts);
		return result;
	}
}
