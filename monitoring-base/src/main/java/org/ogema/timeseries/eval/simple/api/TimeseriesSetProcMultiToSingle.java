package org.ogema.timeseries.eval.simple.api;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;
import org.ogema.core.channelmanager.measurements.FloatValue;
import org.ogema.core.channelmanager.measurements.Quality;
import org.ogema.core.channelmanager.measurements.SampledValue;
import org.ogema.core.timeseries.InterpolationMode;
import org.ogema.core.timeseries.ReadOnlyTimeSeries;
import org.ogema.devicefinder.api.Datapoint;
import org.ogema.devicefinder.api.DatapointInfo.AggregationMode;
import org.ogema.devicefinder.api.DatapointService;
import org.ogema.devicefinder.util.DatapointImpl;
import org.smartrplace.app.monbase.gui.ProcessedReadOnlyTimeSeries;
import org.smartrplace.app.monbase.gui.ProcessedReadOnlyTimeSeries2;
import org.smartrplace.monbase.alarming.AlarmingManagement;

/** The input time series for this provider must be aligned having common time stamps
 * and the time series starting first and ending last shall have no gap in between that occurs not in
 * all input series.*/
public abstract class TimeseriesSetProcMultiToSingle implements TimeseriesSetProcessor {
	private final static long TEST_SHIFT = AlarmingManagement.DAY_MILLIS-2*AlarmingManagement.HOUR_MILLIS;

	protected abstract float aggregateValues(Float[] values, long timestamp, AggregationMode mode);
	//protected TimeSeriesNameProvider nameProvider() {return null;}
	protected void debugCalculationResult(List<Datapoint> input, List<SampledValue> resultLoc) {};
	//protected abstract AggregationMode getMode(String tsLabel);
	protected String resultLabel() {return label;}
	protected String resultLoction(List<Datapoint> input) {
		JSONObject json = new JSONObject();
		JSONArray args = new JSONArray();
		for(Datapoint inp:input) {
			args.put(inp.getLocation());
		}
		json.put(this.getClass().getName(), args);
		String location = json.toString();
		return location;
	}
	
	protected final String label;
	
	public TimeseriesSetProcMultiToSingle(String resultlabel) {
		this.label = resultlabel;
	}
	@Override
	public List<Datapoint> getResultSeries(List<Datapoint> input, DatapointService dpService) {
		List<Datapoint> result = new ArrayList<>();
		long firstStart = Long.MAX_VALUE;
		ReadOnlyTimeSeries firstStartTs = null;
		long lastEnd = 0;
		ReadOnlyTimeSeries lastEndTs = null;
		for(Datapoint tsd: input) {
			ReadOnlyTimeSeries ts = tsd.getTimeSeries();
			SampledValue svStart = ts.getNextValue(0);
			SampledValue svEnd = ts.getPreviousValue(Long.MAX_VALUE);
			if((svStart != null) && (svStart.getTimestamp() < firstStart)) {
				firstStart = svStart.getTimestamp();
				firstStartTs = ts;
			}
			if((svEnd != null) && (svEnd.getTimestamp() > lastEnd)) {
				lastEnd = svEnd.getTimestamp();
				lastEndTs = ts;
			}
		}
		final ReadOnlyTimeSeries firstStartTsFinal = firstStartTs;
		final ReadOnlyTimeSeries lastEndTsFinal =lastEndTs;

		//TODO: We assume that startTS and endTS overlap. If not we might have to use more timeseries intermediately
		long firstStartTSEnd = firstStartTs.getPreviousValue(Long.MAX_VALUE).getTimestamp();
		ProcessedReadOnlyTimeSeries inputSingle = new ProcessedReadOnlyTimeSeries(InterpolationMode.NONE) {
			
			@Override
			protected List<SampledValue> updateValues(long start, long end) {
				if(end <= firstStartTSEnd)
					return firstStartTsFinal.getValues(start, end);
				if(start >= firstStartTSEnd)
					return lastEndTsFinal.getValues(start, end);
				List<SampledValue> resultLoc = new ArrayList<>(firstStartTsFinal.getValues(start,firstStartTSEnd));
				resultLoc.addAll(lastEndTsFinal.getValues(firstStartTSEnd, end));
				return resultLoc;
			}
			
			@Override
			protected long getCurrentTime() {
				return dpService.getFrameworkTime();
			}
		};
		
		DatapointImpl dpIn = new DatapointImpl(inputSingle, resultLoction(input));
		ProcessedReadOnlyTimeSeries2 newTs2 = new ProcessedReadOnlyTimeSeries2(dpIn) {
			@Override
			protected List<SampledValue> getResultValues(ReadOnlyTimeSeries timeSeries, long start,
					long end, AggregationMode mode) {
				Float[] values = new Float[input.size()];
				List<SampledValue> resultLoc = new ArrayList<>();
				for(SampledValue svalTs: tsdi.getTimeSeries().getValues(start, end)) {
					long timestamp = svalTs.getTimestamp();
					int idx = 0;
					for(Datapoint dpLoc: input) {
						SampledValue svLoc = dpLoc.getTimeSeries().getValue(timestamp);
						if(svLoc == null) {
							values[idx] = null;
							List<SampledValue> svTest = dpLoc.getTimeSeries().getValues(timestamp-TEST_SHIFT, timestamp+TEST_SHIFT);
							if(svTest != null && (!svTest.isEmpty())) {
								System.out.println("  !!! Warning: Input time steps not aligned for:"+idx+" in "+label);
							}
						} else
							values[idx] = svLoc.getValue().getFloatValue();
						idx++;
					}
					float val = aggregateValues(values, timestamp, mode);
					resultLoc.add(new SampledValue(new FloatValue(val), timestamp, Quality.GOOD));
				}
				debugCalculationResult(input, resultLoc);
				return resultLoc;
			}
			
			@Override
			protected long getCurrentTime() {
				return dpService.getFrameworkTime();
			}

			//@Override
			//public DatapointImpl getResultSeriesDP() {
			//	return new DatapointImpl(this, location,
			//			resultLabel());
			//}
		}; 
		DatapointImpl newtsdi = newTs2.getResultSeriesDP(dpService);
		newtsdi.setLabel(label);
		result.add(newtsdi);
		return result;
	}

}
