package org.ogema.timeseries.eval.simple.mon3;

import java.util.Collections;
import java.util.List;

import org.ogema.core.channelmanager.measurements.SampledValue;
import org.ogema.core.timeseries.ReadOnlyTimeSeries;
import org.ogema.devicefinder.api.DatapointInfo.AggregationMode;
import org.ogema.devicefinder.api.DpUpdateAPI.DpUpdated;
import org.ogema.timeseries.eval.simple.api.ProcessedReadOnlyTimeSeries3;

public class TimeseriesSetProcSingleToSingle3Dependent extends TimeseriesSetProcSingleToSingle3 {

	public TimeseriesSetProcSingleToSingle3Dependent(String resultlabel) {
		super(resultlabel, null, -99);
	}

	@Override
	protected List<SampledValue> calculateValues(ReadOnlyTimeSeries timeSeries, long start, long end,
			AggregationMode mode, ProcessedReadOnlyTimeSeries3 newTs2) {
		return Collections.emptyList();
	}

	@Override
	protected void alignUpdateIntervalFromSource(DpUpdated updateInterval) {}
}
