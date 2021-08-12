package org.ogema.timeseries.eval.simple.mon3;

import org.ogema.devicefinder.api.DatapointInfo.AggregationMode;
import org.ogema.devicefinder.api.DpUpdateAPI.DpUpdated;

public class TimeseriesSetProcSum3 extends TimeseriesSetProcMultiToSingle3 {
	
	/*public TimeseriesSetProcSum(String label) {
		super(label);
	}
	public TimeseriesSetProcSum(String label, int intervalType) {
		super(label, intervalType);
	}
	public TimeseriesSetProcSum(String label, int intervalType, Integer absoluteTiming) {
		super(label, intervalType, absoluteTiming);
	}
	public TimeseriesSetProcSum(String label, int intervalType, Integer absoluteTiming, Long minIntervalForReCalc) {
		super(label, intervalType, absoluteTiming, minIntervalForReCalc);
	}*/
	public TimeseriesSetProcSum3(String label, int intervalType, Integer absoluteTiming, long minIntervalForReCalc, int updateMode) {
		super(label, intervalType, absoluteTiming, minIntervalForReCalc, updateMode);
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
	
	@Override
	protected void alignUpdateIntervalFromSource(DpUpdated updateInterval) {}

}
