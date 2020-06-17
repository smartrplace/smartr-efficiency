package org.smartrplace.app.monbase.gui;

import java.util.Collection;
import java.util.List;

import org.ogema.externalviewer.extensions.IntervalConfiguration;
import org.ogema.externalviewer.extensions.ScheduleViewerOpenButtonEval.TimeSeriesNameProvider;
import org.smartrplace.app.monbase.MonitoringController;

import de.iwes.timeseries.eval.api.TimeSeriesData;
import de.iwes.widgets.api.widgets.localisation.OgemaLocale;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;

/** Chart configurator page configuration
 * Note: we do not make the gatewayIDs configurable yet
 * Also room information is collected directly from the controller
 */
public interface OfflineControlGUIConfig {
	/** Get names of interval options. Note that the respective {@link IntervalConfiguration} object needs to
	 * be provided by the controller*/
	List<String> getIntervalOptions();
	
	/** Get names of plots.
	 * TODO: Currrently the OfflineControlGUI just supports fixed plot names per page without localisation.
	 * 		 To support multiple languages mulitple pages have to be registered.
	 */
    Collection<String> getPlotNames();
   
    /** Get name of plot to be selected as default
    */
    String getDefaultPlotName();

    /** Get declarations of timeseries types to be added to a certain plot
     * Note: Finding manual data entry schedules based on the declaration is performed via the controller.
     * Also the list of List<ComplexOptionDescription> for the declaration is acquired via the controller.*/
    List<String> baseLabels(String plotName, OgemaLocale locale);
    
    List<TimeSeriesData> getTimeseries(final List<String> gwIds, List<String> roomIds,
    		List<String> baselabels, OgemaHttpRequest req);
    
    /** The list returned may contain more labels than relevant for manual time series types.
     * Only labels starting with a hash ('#') are relevant in the result. These are passed to the contoller
     * to get the actual time series for a combination of type label and room via
     * {@link MonitoringController#getManualDataEntrySchedule(String, String, String)}
     * @param baseLabel a time series label returned by {@link #baseLabels(String, OgemaLocale)}
     * @return
     */
    List<String> getManualTimeseriesTypeLabels(String baseLabel);
    
    TimeSeriesNameProvider nameProvider();
}
