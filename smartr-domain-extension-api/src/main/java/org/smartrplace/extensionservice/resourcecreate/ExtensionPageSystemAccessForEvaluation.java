package org.smartrplace.extensionservice.resourcecreate;

import java.util.List;

import org.ogema.core.model.Resource;
import org.smartrplace.extensionservice.driver.DriverProvider;

import de.iwes.timeseries.eval.garo.multibase.GaRoSingleEvalProvider;

public interface ExtensionPageSystemAccessForEvaluation {
	
	/**
	 * 
	 * @param eval
	 * @param entryResource
	 * @param userData
	 * @param userDataNonEdit
	 * @param drivers may be null, then all available drivers will be tested if they can provide
	 * 		data for the entryResource
	 * @return
	 */
	public long[] calculateKPIs(GaRoSingleEvalProvider eval, Resource entryResource,
			Resource configurationResource,
			List<DriverProvider> drivers, boolean saveJsonResult,
			int defaultIntervalsToCalculate);
	
	
}
