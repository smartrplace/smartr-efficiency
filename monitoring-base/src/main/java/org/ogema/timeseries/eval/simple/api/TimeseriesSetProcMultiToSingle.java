package org.ogema.timeseries.eval.simple.api;

import java.util.ArrayList;
import java.util.List;

import org.ogema.core.channelmanager.measurements.FloatValue;
import org.ogema.core.channelmanager.measurements.Quality;
import org.ogema.core.channelmanager.measurements.SampledValue;
import org.ogema.core.timeseries.InterpolationMode;
import org.ogema.core.timeseries.ReadOnlyTimeSeries;
import org.ogema.devicefinder.api.ConsumptionInfo.AggregationMode;
import org.ogema.devicefinder.api.DatapointService;
import org.ogema.externalviewer.extensions.ScheduleViewerOpenButtonEval.TimeSeriesNameProvider;
import org.smartrplace.app.monbase.gui.ProcessedReadOnlyTimeSeries2;

import de.iwes.timeseries.eval.api.TimeSeriesData;
import de.iwes.timeseries.eval.api.extended.util.TimeSeriesDataExtendedImpl;
import de.iwes.timeseries.eval.base.provider.utils.TimeSeriesDataImpl;

/** The input time series for this provider must be aligned having common time stamps*/
public abstract class TimeseriesSetProcMultiToSingle implements TimeseriesSetProcessor {
	protected abstract float aggregateValues(float[] values, long timestamp, AggregationMode mode);
	protected TimeSeriesNameProvider nameProvider() {return null;}
	//protected abstract AggregationMode getMode(String tsLabel);
	protected String resultLabel() {return label;}
	protected String resultDescription() {
		return resultLabel();
	}
	protected final String label;
	
	public TimeseriesSetProcMultiToSingle(String label) {
		this.label = label;
	}
	@Override
	public List<TimeSeriesData> getResultSeries(List<TimeSeriesData> input, DatapointService dpService) {
		List<TimeSeriesData> result = new ArrayList<>();
		final TimeSeriesDataImpl tsdi = (TimeSeriesDataImpl) input.get(0);
		ProcessedReadOnlyTimeSeries2 newTs2 = new ProcessedReadOnlyTimeSeries2(tsdi, nameProvider(),
					(AggregationMode)null) {
			@Override
			protected List<SampledValue> getResultValues(ReadOnlyTimeSeries timeSeries, long start,
					long end, AggregationMode mode) {
				float[] values = new float[input.size()];
				List<SampledValue> resultLoc = new ArrayList<>();
				for(SampledValue svalTs: tsdi.getTimeSeries().getValues(start, end)) {
					long timestamp = svalTs.getTimestamp();
					int idx = 0;
					for(TimeSeriesData tsd: input) {
						TimeSeriesDataImpl tsdiLoc = (TimeSeriesDataImpl) tsd;
						SampledValue svLoc = tsdiLoc.getTimeSeries().getValue(timestamp);
						if(svLoc == null)
							values[idx] = Float.NaN;
						else
							values[idx] = svLoc.getValue().getFloatValue();
						idx++;
					}
					float val = aggregateValues(values, timestamp, mode);
					resultLoc.add(new SampledValue(new FloatValue(val), timestamp, Quality.GOOD));
				}
				return resultLoc;					
			}
			
			@Override
			public TimeSeriesDataExtendedImpl getResultSeries() {
				return new TimeSeriesDataExtendedImpl(this,
						resultLabel(), resultDescription(), InterpolationMode.STEPS);
			}
		}; 
		TimeSeriesDataExtendedImpl newtsdi = newTs2.getResultSeries();
		result.add(newtsdi);
		return result;
	}

}
