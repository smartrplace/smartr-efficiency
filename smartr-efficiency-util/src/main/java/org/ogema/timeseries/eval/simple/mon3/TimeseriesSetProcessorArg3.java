package org.ogema.timeseries.eval.simple.mon3;

import java.util.List;

import org.ogema.devicefinder.api.Datapoint;
import org.ogema.devicefinder.api.DatapointService;

public interface TimeseriesSetProcessorArg3<T> extends TimeseriesSetProcessor3 {
	@Override
	default List<Datapoint> getResultSeries(List<Datapoint> input, boolean registersTimedJob,
			DatapointService dpService) {
		throw new UnsupportedOperationException("Only supports getResultSeries with Argument Object");
	}
	List<Datapoint> getResultSeries(List<Datapoint> input, DatapointService dpService,
			boolean registersTimedJob, T params);
	
	Class<T> getParamClass();
}
