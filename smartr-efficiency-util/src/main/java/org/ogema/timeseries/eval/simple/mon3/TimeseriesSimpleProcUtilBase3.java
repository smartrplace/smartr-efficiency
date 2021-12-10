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
import org.ogema.core.model.simple.StringResource;
import org.ogema.core.timeseries.ReadOnlyTimeSeries;
import org.ogema.devicefinder.api.Datapoint;
import org.ogema.devicefinder.api.DatapointInfo.AggregationMode;
import org.ogema.devicefinder.api.DatapointService;
import org.ogema.devicefinder.util.AggregationModeProvider;
import org.ogema.devicefinder.util.DPUtil;
import org.ogema.externalviewer.extensions.ScheduleViewerOpenButtonEval.TimeSeriesNameProvider;
import org.ogema.model.gateway.LocalGatewayInformation;
import org.ogema.model.jsonresult.JsonOGEMAFileData;
import org.ogema.timeseries.eval.simple.api.ProcessedReadOnlyTimeSeries2;
import org.ogema.timeseries.eval.simple.api.ProcessedReadOnlyTimeSeries3;
import org.ogema.timeseries.eval.simple.api.TimeseriesSetProcessor3;
import org.ogema.timeseries.eval.simple.mon.TimeseriesSetProcMultiToSingle;
import org.ogema.timeseries.eval.simple.mon.TimeseriesSimpleProcUtilBase;
import org.smartrplace.autoconfig.api.InitialConfig;
import org.smartrplace.tsproc.persist.TsProcPersistUtil;

import de.iwes.timeseries.eval.api.TimeSeriesData;
import de.iwes.util.resource.ResourceHelper;

public abstract class TimeseriesSimpleProcUtilBase3 implements TimeseriesSimpleProcUtilBase {
	protected final Map<String, TimeseriesSetProcessor3> knownProcessors3 = new HashMap<>();
	
	protected final Set<Datapoint> generatedTss = new HashSet<>();
	public int getTssNum() {
		return generatedTss.size();
	}
	
	protected final ApplicationManager appMan;
	public final DatapointService dpService;

	/**Overwrite this to load data into the timeseries initially by a different mechanism than standard file reading
	 */
	public void loadInitData3(Datapoint dp) {
		TsProcPersistUtil.importTsFromFile(dp, appMan);		
	}
	public JsonOGEMAFileData saveData(Datapoint dp) {
		return TsProcPersistUtil.saveTsToFile(dp, appMan);		
	}
	
	public TimeseriesSetProcessor3 getProcessor3(String procID) {
		return knownProcessors3.get(procID);
	}
	
	/** Overwrite this to make sure calculations are done from a certain time even if some values have
	 * been processed and the result has been saved already.<br> 
	 * Make sure the ID is unique when you overwrite this: This is the time stamp from which on
	 * all input data will be re-used. Real start time is the aligned start of the interval of the
	 * time stamp according to the alignment (if alignment is set).<br>
	 * IMPORTANT: Do not reuse exactly the same time stamp when you want to trigger again. As long
	 * as the known IDs in the gateway resource is not cleaned each time stamp is only processed once. But
	 * you can just add 1 msec to get another time stamp.
	 * @param ts */
	protected Long recalcFromTime(ProcessedReadOnlyTimeSeries3 ts) {
		String tsFilter = System.getProperty("org.ogema.timeseries.eval.simple.api.tsfilter");
		if(tsFilter != null &&
				((ts.datapointForChangeNotification == null) ||
						(!ts.datapointForChangeNotification.getLocation().contains(tsFilter)))) {
			return null;
		}
		return Long.getLong("org.ogema.timeseries.eval.simple.api.recalcfrom");
	};
	
	public String id() {
		return getClass().getName();
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
	
	public int saveUpdatesForAllData() {
		int count = 0;
		List<Datapoint> generatedTssLoc = new ArrayList<>(generatedTss);
		for(Datapoint dp: generatedTssLoc) {
			if(saveData(dp) != null)
				count++;
		}
		return count;
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
	
	protected Long checkForTimeReset(ProcessedReadOnlyTimeSeries3 ts) {
		final Long recalcFrom = recalcFromTime(ts);
		if(recalcFrom != null) {
			final String shortRecalId;
			final StringResource initDoneStatus;
			if(ts.timedJob != null) {
				shortRecalId = "RC_"+recalcFrom;
				initDoneStatus = ts.timedJob.res().initDoneStatus();
			} else {
				shortRecalId = ts.datapointForChangeNotification.getLocation()+"_"+recalcFrom;
				LocalGatewayInformation gw = ResourceHelper.getLocalGwInfo(appMan);
				initDoneStatus = gw.initDoneStatus();
			}
			if((!InitialConfig.checkInitAndMarkAsDone(shortRecalId, initDoneStatus, ts.datapointForChangeNotification.getLocation()))) {	
				return recalcFrom;
			}
		}
		return null;
	}
}
