package org.smartrplace.smarteff.model.syste;

import org.ogema.core.model.array.StringArrayResource;
import org.ogema.model.prototypes.Data;

/** Relevant for time series that have their content stored in external files that are
 * only loaded on demand, e.g. via the GenericDriverProvider
 * */
public interface GenericTSDPTimeSeries extends Data {
	/** Reference to an explicit time series declaration for user management*/
	//SmartEffTimeSeries userTimeSeries();
	
	/**Paths to files that contain the data of the time series
	 * TODO: Only storage filePath relative to internal base directory as this is visible
	 * to the user*/
	StringArrayResource filePaths();
}
