package org.ogema.timeseries.eval.simple.mon3;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

import org.json.JSONArray;
import org.json.JSONObject;
import org.ogema.core.channelmanager.measurements.FloatValue;
import org.ogema.core.channelmanager.measurements.Quality;
import org.ogema.core.channelmanager.measurements.SampledValue;
import org.ogema.core.timeseries.ReadOnlyTimeSeries;
import org.ogema.devicefinder.api.Datapoint;
import org.ogema.devicefinder.api.DatapointInfo.AggregationMode;
import org.ogema.devicefinder.api.DatapointService;
import org.ogema.devicefinder.api.DpUpdateAPI.DpUpdated;
import org.ogema.devicefinder.api.TimedJobProvider;
import org.ogema.devicefinder.util.TimedJobMemoryData;
import org.ogema.externalviewer.extensions.ScheduleViewerOpenButtonEval.TimeSeriesNameProvider;
import org.ogema.timeseries.eval.simple.api.ProcessedReadOnlyTimeSeries2;
import org.ogema.timeseries.eval.simple.api.ProcessedReadOnlyTimeSeries3;
import org.ogema.timeseries.eval.simple.api.TimeProcUtil;
import org.ogema.timeseries.eval.simple.mon.TimeseriesSetProcSingleToSingle;
import org.smartrplace.apps.eval.timedjob.TimedJobConfig;
import org.smartrplace.tissue.util.logconfig.PerformanceLog;
import org.smartrplace.util.frontend.servlet.UserServletUtil;

import de.iwes.util.resource.ValueResourceHelper;
import de.iwes.util.timer.AbsoluteTimeHelper;
import de.iwes.widgets.api.widgets.localisation.OgemaLocale;

/** The input time series for this provider must be aligned having common time stamps
 * and the time series starting first and ending last shall have no gap in between that occurs not in
 * all input series.<br>
 * Note that this processor evaluates the input time series on calling {@link #getResultSeries(List, DatapointService)} as
 * it has to find out which time series starts first and which ends last. Based on this it builds a
 * temporary input time series inputSingle.
 * Note also that you have to set the label of the resulting datapoint explicitly as the label cannot
 * be directly obtained from a single input datapoint with postfix as for {@link TimeseriesSetProcSingleToSingle}.
 * If you are using a Multi2Single predefined evaluation you still have to set the label afterwards. You should
 * set at least label(null) or label(ENGLISH).*/
public abstract class TimeseriesSetProcMultiToSingle3 implements TimeseriesSetProcessor3 {

	public static PerformanceLog tsSingleLog;
	public static PerformanceLog aggregateLog;
	
	protected abstract float aggregateValues(Float[] values, long timestamp, AggregationMode mode);
	
	/** change startTime and endTime of parameter if necessary*/
	protected abstract void alignUpdateIntervalFromSource(DpUpdated updateInterval);
	
	/**Overwrite this to load data into the timeseries initially, e.g. by reading
	 * from a file
	 */
	protected void loadInitData(Datapoint dp) {}
	
	/** Update mode regarding interval propagation and regarding singleInput.<br>
	 * Note that from mode 2 on any change in the input data triggers a recalculation of the output data<br>
	 * !! Note also that this is set for the entire processor, so usually for your instance of {@link TimeseriesSimpleProcUtil3} !!
	 * 
	 * 0: Do not generate a result time series if input is empty, no updates
	 *  1: Update only at the end
	 *  2: Update exactly for any input change interval
	 *  3: Update for any input change onwards completely
	 *  4: Update completely if any input has a change 
	 */
	public final int updateMode;
	
	protected final Integer absoluteTiming;
	
	protected final long minIntervalForReCalc;
	
	protected void debugCalculationResult(List<Datapoint> input, List<SampledValue> resultLoc) {};
	//protected abstract AggregationMode getMode(String tsLabel);
	protected String resultLabel() {return label;}
	public String resultLoction(List<Datapoint> input) {
		return resultLoction(input, this.getClass().getName());
	}

	public static String resultLoction(List<Datapoint> input, String className) {
		JSONObject json = new JSONObject();
		JSONArray args = new JSONArray();
		for(Datapoint inp:input) {
			args.put(inp.getLocation());
		}
		json.put(className, args);
		String location = json.toString();
		return location;
	}
	
	protected final String label;
	//protected final int intervalType2;
	private final long TEST_SHIFT;
	
	/**
	 * 
	 * @param resultlabel
	 * @param intervalType relevant for test shift only
	 * @param absoluteTiming if set then knownEnd will be reset to beginning of interval always
	 */
	public TimeseriesSetProcMultiToSingle3(String resultlabel, int intervalType, Integer absoluteTiming,
			long minIntervalForReCalc, int updateMode) {
		this.label = resultlabel;
		//this.intervalType = intervalType;
		this.absoluteTiming = absoluteTiming;
		this.updateMode = updateMode;
		this.minIntervalForReCalc = minIntervalForReCalc;
		TEST_SHIFT = (long) (0.9*AbsoluteTimeHelper.getStandardInterval(intervalType)); //TimeProcUtil.DAY_MILLIS-2*TimeProcUtil.HOUR_MILLIS;
	}

	protected ProcessedReadOnlyTimeSeries2 resultSeriesStore = null;
	@Override
	public List<Datapoint> getResultSeries(List<Datapoint> input, boolean registersTimedJob, DatapointService dpService) {
		List<Datapoint> result = new ArrayList<>();

		ProcessedReadOnlyTimeSeries3 resultTs = new ProcessedReadOnlyTimeSeries3((TimeSeriesNameProvider)null, null) {
			@Override
			protected List<SampledValue> getResultValues(ReadOnlyTimeSeries timeSeries, long start,
					long end, AggregationMode mode) {
				return null;
			}
			
			@Override
			protected List<Datapoint> getInputDps() {
				return input;
			}
			
			@Override
			protected String getLabelPostfix() {
				return ""; //labelPostfix;
			}
			
			@Override
			public String resultLabel() {
				return TimeseriesSetProcMultiToSingle3.this.dpLabel();
			}
			
			@Override
			protected long getCurrentTime() {
				return dpService.getFrameworkTime();
			}
			
			@Override
			protected void alignUpdateIntervalFromSource(DpUpdated updateInterval) {
				TimeseriesSetProcMultiToSingle3.this.alignUpdateIntervalFromSource(updateInterval);
			}
			
			@Override
			public void loadInitData() {
				TimeseriesSetProcMultiToSingle3.this.loadInitData(datapointForChangeNotification);
			}
			
			@Override
			protected List<SampledValue> getResultValuesMulti(List<ReadOnlyTimeSeries> timeSeries,
					long start, long end, AggregationMode mode) {
				long startOfAgg = dpService.getFrameworkTime();
if(Boolean.getBoolean("evaldebug")) System.out.println("Starting aggregation for "+getShortId());
				long startOfCalc = dpService.getFrameworkTime();
						
				Float[] values = new Float[input.size()];
				List<SampledValue> resultLoc = new ArrayList<>();
				//List<SampledValue> vals; // = getTSDI().getTimeSeries().getValues(start, end);
				
				//get all datapoints from input
				TreeSet<Long> vals = new TreeSet<>(); //)<>();
				//for(Datapoint dpLoc: input) {
				for(ReadOnlyTimeSeries ts: timeSeries) {
					List<SampledValue> svv = ts.getValues(start, end); //dpLoc.getTimeSeries().getValues(start, end);
					for(SampledValue sv: svv) {
						vals.add(sv.getTimestamp());
					}
				}
				
long endOfAgg1 =  dpService.getFrameworkTime();
if(aggregateLog != null) aggregateLog.logEvent(endOfAgg1-startOfAgg, "Calculation of AG1 "+getShortId()+" took");
				for(long timestamp: vals) {
					//long timestamp = svalTs.getTimestamp();
					int idx = 0;
					//for(Datapoint dpLoc: input) {
					for(ReadOnlyTimeSeries ts: timeSeries) {
						SampledValue svLoc = ts.getValue(timestamp); //dpLoc.getTimeSeries().getValue(timestamp);
						if(svLoc == null) {
							values[idx] = null;
							List<SampledValue> svTest = ts.getValues(timestamp-TEST_SHIFT, timestamp+TEST_SHIFT);
							if(svTest != null && (!svTest.isEmpty())) {
								System.out.println("  !!! Warning: Input time steps not aligned for:"+idx+" in "+label);
								}
							} else
								values[idx] = svLoc.getValue().getFloatValue();
							idx++;
						}
long startOfAgg2 = dpService.getFrameworkTime();
						float val = aggregateValues(values, timestamp, mode);
long endOfAgg =  dpService.getFrameworkTime();
if(aggregateLog != null) aggregateLog.logEvent(endOfAgg-startOfAgg2, "Calculation of AG2 "+getShortId()+" took");
if(aggregateLog != null) aggregateLog.logEvent(endOfAgg-startOfAgg, "Calculation of AGG "+getShortId()+" took");
					resultLoc.add(new SampledValue(new FloatValue(val), timestamp, Quality.GOOD));
				}
				debugCalculationResult(input, resultLoc);
				
				long endOfCalc =  dpService.getFrameworkTime();
//TODO: These values could be logged to check evaluation performance
if(tsSingleLog != null) tsSingleLog.logEvent((endOfCalc-startOfCalc), "Calculation of TSI "+getShortId()+" took");

				return resultLoc;
			}
			
			@Override
			public Long getFirstTimeStampInSource() {
				return TimeseriesSetProcSingleToSingle3.getFirstTsInSource(input);
			}
		};

		String location = resultLoction(input); //getinputSingle.dpIn.getLocation()+"";
		Datapoint newtsdi = TimeseriesSetProcSingleToSingle3.getOrUpdateTsDp(location, resultTs, dpService);
		String label = resultLabel();
		if(label != null)
			newtsdi.setLabelDefault(label);

		if(registersTimedJob) {
			registerTimedJob(resultTs, input, resultLabel(), newtsdi.getLocation(), "M2S", minIntervalForReCalc, dpService);
		}

		result.add(newtsdi);
		return result;
	}

	public static String getResultDpLocation(List<Datapoint> input, String className) {
		return resultLoction(input, className)+"_SgInp";		
	}
	
	public String getShortId() {
		return resultLabel();
		//return dpLabel();
		//return ProcessedReadOnlyTimeSeries2.getShortId(tsdi, null, null);
	}

	protected String dpLabel() {
		return "Multi_"+((resultSeriesStore!=null)?resultSeriesStore.dpLabel():"?");
	}

	protected static void registerTimedJob(ProcessedReadOnlyTimeSeries3 ts, List<Datapoint> input,
			String label, String resultLocation, String idBase,
			long repetitionTime,
			DatapointService dpService) {
		dpService.timedJobService().registerTimedJobProvider(new TimedJobProvider() {

			@Override
			public String label(OgemaLocale locale) {
				return label;
			}
			
			@Override
			public String id() {
				return UserServletUtil.getHashWithPrefix(idBase+"_"+label+"_", resultLocation);
			}
			
			@Override
			public String description(OgemaLocale locale) {
				return resultLocation;
			}
			
			@Override
			public boolean initConfigResource(TimedJobConfig config) {
				ValueResourceHelper.setCreate(config.alignedInterval(), -1);
				ValueResourceHelper.setCreate(config.interval(), (float) (((double)repetitionTime)/TimeProcUtil.MINUTE_MILLIS));
				ValueResourceHelper.setCreate(config.disable(), false);
				ValueResourceHelper.setCreate(config.performOperationOnStartUpWithDelay(), 0);
				return true;
			}
			
			@Override
			public String getInitVersion() {
				return "A";
			}
			
			@Override
			public void execute(long now, TimedJobMemoryData data) {
				for(Datapoint indp: input) {
					ReadOnlyTimeSeries ints = indp.getTimeSeries();
					if(ints instanceof ProcessedReadOnlyTimeSeries3) {
						ProcessedReadOnlyTimeSeries3 ints3 = (ProcessedReadOnlyTimeSeries3)ints;
						updateTimeseries(ints3, now);
					}
				}
				updateTimeseries(ts, now);
			}
			
			@Override
			public int evalJobType() {
				return 1;
			}
		});
	}

	protected static void updateTimeseries(ProcessedReadOnlyTimeSeries3 ts, long now) {
		long start;
		if(ts.getLastEndTime() <= 0) {
			ts.loadInitData();
			if(ts.getLastEndTime() <= 0) {
				Long startRaw = ts.getFirstTimeStampInSource();
				if(startRaw == null)
					start = now;
				else start = startRaw;
			} else
				start = ts.getLastEndTime();
		} else
			start = ts.getLastEndTime();
		ts.updateValuesStoredAligned(start, now, false);		
	}
}
