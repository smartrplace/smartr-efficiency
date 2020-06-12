package org.ogema.timeseries.eval.simple.mon;

import org.ogema.devicefinder.api.DatapointInfo.AggregationMode;

public class TimeseriesSetProcSum extends TimeseriesSetProcMultiToSingle {
	
	public TimeseriesSetProcSum(String label) {
		super(label);
	}

	@Override
	protected float aggregateValues(Float[] values, long timestamp, AggregationMode mode) {
		float result = 0;
		for(Float val: values) {
			if((val != null) && (!Float.isNaN(val)))
				result += val;
		}
		return result;
	}

}
