package org.smartrplace.app.monbase.gui;

import java.util.Collection;
import java.util.List;

import org.ogema.externalviewer.extensions.IntervalConfiguration;

import de.iwes.timeseries.eval.api.TimeSeriesData;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;

/** Chart configurator page configuration
 * Note: we do not make the gatewayIDs configurable yet
 * Also room information is collected directly from the controller
 */
public interface OfflineControlGUIConfig {
	/** Get names of interval options. Note that the respective {@link IntervalConfiguration} object needs to
	 * be provided by the controller*/
	List<String> getIntervalOptions();
	
	/** Get names of plots
	 * TODO: Rename to getPlotNames*/
    Collection<String> getDataTypes();
   
    /** Get name of plot to be selected as default
     * TODO: Rename to getDefaultPlotName*/
    String getDefaultComplexOptionKey();

    /** Get declarations of timeseries types to be added to a certain plot
     * Note: Finding manual data entry schedules based on the declaration is performed via the controller.
     * Also the list of List<ComplexOptionDescription> for the declaration is acquired via the controller.*/
    List<String> baseLabels(String plotName);
    
    List<TimeSeriesData> getTimeseries(final List<String> gwIds, List<String> roomIds,
    		OgemaHttpRequest req);
}
