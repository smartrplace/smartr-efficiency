package org.ogema.timeseries.eval.simple.mon3;

import java.util.List;

import org.ogema.devicefinder.api.Datapoint;
import org.ogema.devicefinder.api.DatapointService;
import org.ogema.devicefinder.util.AggregationModeProvider;
import org.ogema.devicefinder.util.DPUtil;
import org.ogema.externalviewer.extensions.ScheduleViewerOpenButtonEval.TimeSeriesNameProvider;
import org.smartrplace.util.frontend.servlet.UserServlet;

import de.iwes.timeseries.eval.api.TimeSeriesData;

public interface TimeseriesSetProcessor3 {
	/** Provide resulting time series
	 * 
	 * @param input
	 * @param dpService: Via the DatapointService the processor can add data point information for result series.
	 * If the result time series shall be accessible via TimeseriesID, then it can also be added
	 * to {@link UserServlet#knownTS} in this method.<br>
	 * Note that these steps can be done by the processor or by the surrounding process. This method
	 * should only be relevant if the information is added by the process, but in many cases this can be done
	 * much more efficiently by The TimeseriesSetProcessy<br>
	 * Note that the label and description shall be provided directly via the methods in {@link Datapoint}.
	 * TODO: There should be an extension of {@link DatapointExtendedImpl} providing a reference to the
	 * Datapoint.
	 * @return resulting time series.
	 */
	List<Datapoint> getResultSeries(List<Datapoint> input, boolean registersTimedJob, DatapointService dpService);
	
	default List<TimeSeriesData> getResultSeriesTSD(List<TimeSeriesData> input, boolean registersTimedJob, DatapointService dpService,
			TimeSeriesNameProvider nameProvider, AggregationModeProvider aggProv) {
		return DPUtil.getTSList(getResultSeries(DPUtil.getDPList(input, nameProvider, aggProv),
				registersTimedJob, dpService), null);
	};
}
