package org.ogema.timeseries.eval.simple.mon;

import java.util.ArrayList;
import java.util.List;

import org.ogema.core.channelmanager.measurements.SampledValue;
import org.ogema.core.timeseries.ReadOnlyTimeSeries;
import org.ogema.devicefinder.api.Datapoint;
import org.ogema.devicefinder.api.DatapointInfo.AggregationMode;
import org.ogema.devicefinder.api.DatapointService;
import org.ogema.devicefinder.util.DatapointImpl;
import org.ogema.timeseries.eval.simple.api.ProcessedReadOnlyTimeSeries2;

public abstract class TimeseriesSetProcSingleToSingle implements TimeseriesSetProcessor {
	protected abstract List<SampledValue> calculateValues(ReadOnlyTimeSeries timeSeries, long start,
			long end, AggregationMode mode);
	//protected TimeSeriesNameProvider nameProvider() {return null;}
	//protected abstract AggregationMode getMode(String tsLabel);
	protected final String labelPostfix;
	
	/** Return true if informatoin relevant for the labelling has been added*/
	protected boolean addDatapointInfo(Datapoint tsdi) {
		return false;
	}
	
	public TimeseriesSetProcSingleToSingle(String labelPostfix) {
		this.labelPostfix = labelPostfix;
	}
	@Override
	public List<Datapoint> getResultSeries(List<Datapoint> input, DatapointService dpService) {
		List<Datapoint> result = new ArrayList<>();
		for(Datapoint tsdi: input) {
			//if(nameProvider() != null)
			//	tsdi.setLabel(nameProvider().getShortNameForTypeI(tsdi.getGaroDataType(), tsdi.getTimeSeriesDataImpl()));
			ProcessedReadOnlyTimeSeries2 newTs2 = new ProcessedReadOnlyTimeSeries2(tsdi) {
				@Override
				protected List<SampledValue> getResultValues(ReadOnlyTimeSeries timeSeries, long start,
						long end, AggregationMode mode) {
					return calculateValues(timeSeries, start, end, mode);						
				}
				@Override
				protected String getLabelPostfix() {
					return labelPostfix;
				}
				
				@Override
				protected long getCurrentTime() {
					return dpService.getFrameworkTime();
				}
			}; 
			DatapointImpl newtsdi = newTs2.getResultSeriesDP(dpService);
			result.add(newtsdi);
		}
		return result;
	}

}
