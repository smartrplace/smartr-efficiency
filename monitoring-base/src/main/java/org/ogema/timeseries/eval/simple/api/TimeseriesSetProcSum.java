package org.ogema.timeseries.eval.simple.api;

import org.ogema.devicefinder.api.DatapointInfo.AggregationMode;

public class TimeseriesSetProcSum extends TimeseriesSetProcMultiToSingle {
	
	public TimeseriesSetProcSum(String label) {
		super(label);
	}

	@Override
	protected float aggregateValues(float[] values, long timestamp, AggregationMode mode) {
		float result = 0;
		for(float val: values) {
			if(!Float.isNaN(val))
				result += val;
		}
		return result;
	}

}
