package org.ogema.timeseries.eval.simple.api;

import java.util.List;

import org.ogema.devicefinder.api.DatapointService;
import org.smartrplace.util.frontend.servlet.UserServlet;

import de.iwes.timeseries.eval.api.TimeSeriesData;
import de.iwes.timeseries.eval.api.extended.util.TimeSeriesDataExtendedImpl;

public interface TimeseriesSetProcessor {
	/** Provide resulting time series
	 * 
	 * @param input
	 * @param dpService: Via the DatapointService the processor can add data point information for result series.
	 * If the result time series shall be accessible via TimeseriesID, then it can also be added
	 * to {@link UserServlet#knownTS} in this method.<br>
	 * Note that these steps can be done by the processor or by the surrounding process. This method
	 * should only be relevant if the information is added by the process, but in many cases this can be done
	 * much more efficiently by The TimeseriesSetProcessy<br>
	 * Note that the label and description shall be provided directly via the methods in {@link TimeSeriesData}.
	 * TODO: There should be an extension of {@link TimeSeriesDataExtendedImpl} providing a reference to the
	 * Datapoint.
	 * @return resulting time series.
	 */
	List<TimeSeriesData> getResultSeries(List<TimeSeriesData> input, DatapointService dpService);
}
