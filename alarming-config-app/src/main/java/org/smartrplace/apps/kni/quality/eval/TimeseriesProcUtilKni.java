package org.smartrplace.apps.kni.quality.eval;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.ogema.core.application.ApplicationManager;
import org.ogema.core.channelmanager.measurements.FloatValue;
import org.ogema.core.channelmanager.measurements.Quality;
import org.ogema.core.channelmanager.measurements.SampledValue;
import org.ogema.core.timeseries.ReadOnlyTimeSeries;
import org.ogema.devicefinder.api.Datapoint;
import org.ogema.devicefinder.api.DatapointInfo.AggregationMode;
import org.ogema.devicefinder.api.DatapointService;
import org.ogema.devicefinder.api.DpUpdateAPI.DpUpdated;
import org.ogema.devicefinder.util.AlarmingConfigUtil;
import org.ogema.timeseries.eval.simple.api.ProcessedReadOnlyTimeSeries3;
import org.ogema.timeseries.eval.simple.api.TimeProcUtil;
import org.ogema.timeseries.eval.simple.mon3.TimeseriesSetProcSingleToSingle3;
import org.ogema.timeseries.eval.simple.mon3.TimeseriesSetProcSingleToSingle3Dependent;
import org.ogema.timeseries.eval.simple.mon3.TimeseriesSetProcessor3;
import org.ogema.timeseries.eval.simple.mon3.TimeseriesSimpleProcUtil3;
import org.ogema.timeseries.eval.simple.mon3.std.StandardEvalAccess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.iwes.util.timer.AbsoluteTimeHelper;
import de.iwes.util.timer.AbsoluteTiming;

public class TimeseriesProcUtilKni extends TimeseriesSimpleProcUtil3 {
	public static final long KPI_UPDATE_RATE = 3*TimeProcUtil.MINUTE_MILLIS;

	public static final String QUALITY_DAILY = "QualityDaily";

	public static final long DAILY_EVAL_INTERVAL = (Boolean.getBoolean("qualitydebug")?6:40)*TimeProcUtil.DAY_MILLIS;

	//protected final FloatResource maxGapSizeRes;
	protected final Logger logger = LoggerFactory.getLogger(getClass());
	
	public TimeseriesProcUtilKni(ApplicationManager appMan, DatapointService dpService) {
		this(appMan, dpService, 2, KPI_UPDATE_RATE);
	}
	public TimeseriesProcUtilKni(ApplicationManager appMan, DatapointService dpService,
			int updateMode, long minIntervalForReCalc) {
		super(appMan, dpService, updateMode, minIntervalForReCalc);
		//this.maxGapSizeRes = maxGapSizeRes;
		
		TimeseriesSetProcessor3 dayProc = new TimeseriesSetProcSingleToSingle3("_quality", AbsoluteTiming.DAY, minIntervalForReCalc) {
			
			@Override
			protected List<SampledValue> calculateValues(ReadOnlyTimeSeries timeSeries, long start, long end,
					AggregationMode mode, ProcessedReadOnlyTimeSeries3 newTs2) {

				long nextDayStart = AbsoluteTimeHelper.getIntervalStart(start, AbsoluteTiming.DAY);
				
				List<SampledValue> result = new ArrayList<>();
				List<SampledValue> resultGold = new ArrayList<>();
				while(nextDayStart <= end) {
					long startCurrentDay = nextDayStart;
					nextDayStart = AbsoluteTimeHelper.addIntervalsFromAlignedTime(nextDayStart, 1, AbsoluteTiming.DAY);
					int[] res1 = StandardEvalAccess.getQualityValues(appMan, dpService, startCurrentDay, nextDayStart,
							AlarmingConfigUtil.QUALITY_DAY_MAX_MINUTES);
					result.add(new SampledValue(new FloatValue(res1[0]), startCurrentDay, Quality.GOOD));
					resultGold.add(new SampledValue(new FloatValue(res1[1]), startCurrentDay, Quality.GOOD));
				}
				Datapoint dpGold = newTs2.getDependentTimeseries("goldTs");
				((ProcessedReadOnlyTimeSeries3)dpGold.getTimeSeries()).updateValuesStoredForcedForDependentTimeseries(end, resultGold);
				return result;
			}

			@Override
			protected Map<String, Datapoint> addDependetTimeseries(Datapoint input) {
				Datapoint goldTs = new TimeseriesSetProcSingleToSingle3Dependent("_qualityGold").
						getResultSeriesSingle(input, false, dpService);
				Map<String, Datapoint> result = new HashMap<>();
				result.put("goldTs", goldTs);
				return result ;
			}
			
			@Override
			protected Long getFirstTimestampInSource() {
				long now = dpService.getFrameworkTime();
				return now - DAILY_EVAL_INTERVAL;
			}
			
			@Override
			protected void alignUpdateIntervalFromSource(DpUpdated updateInterval) {
				updateInterval.start = AbsoluteTimeHelper.getIntervalStart(updateInterval.start, AbsoluteTiming.DAY);
				updateInterval.end = AbsoluteTimeHelper.getNextStepTime(updateInterval.end, AbsoluteTiming.DAY)-1;				
			}
		};
		knownProcessors3.put(QUALITY_DAILY, dayProc);
	}

}
