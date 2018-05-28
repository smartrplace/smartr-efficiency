package org.smartrplace.extensionservice.resourcecreate;

import java.util.List;

import org.ogema.core.model.Resource;
import org.ogema.model.jsonresult.JSONResultFileData;
import org.ogema.util.directresourcegui.kpi.KPIStatisticsManagement;
import org.smartrplace.extensionservice.driver.DriverProvider;

import de.iwes.timeseries.eval.garo.api.base.GaRoSuperEvalResult;
import de.iwes.timeseries.eval.garo.multibase.GaRoSingleEvalProvider;

public interface ExtensionPageSystemAccessForEvaluation {
	
	/**
	 * 
	 * @param eval
	 * @param entryResource
	 * @param userData
	 * @param userDataNonEdit
	 * @param drivers may be null, then all available drivers will be tested if they can provide
	 * 		data for the entryResource. Note that currently users do not have access to
	 * 		DataProvider control so usually null has to be set here and all active
	 * 		DataProviders are used.
	 * @return
	 */
	public long[] calculateKPIs(GaRoSingleEvalProvider eval, Resource entryResource,
			Resource configurationResource,
			List<DriverProvider> drivers, boolean saveJsonResult,
			int defaultIntervalsToCalculate);
	
	public long[] calculateKPIs(GaRoSingleEvalProvider eval, Resource entryResource,
			Resource configurationResource, List<DriverProvider> drivers, boolean saveJsonResult,
			long startTime, long endTime);

	
	/** Get all file descriptor resources that are indicated to be generated by certain provider in a certain interval
	 * @param startTime start of interval
	 * @param endTime end of interval
	 * @param includeOverlap if true all files that contribute to the interval at least partially are returned, 
	 * 		otherwise only files that are completely inside the interval*/
	public List<JSONResultFileData> getDataOfProvider(String providerId, long startTime, long endTime,
			boolean includeOverlap);

	/** Get a super result containing all evaluation intervals of a certain interval even from different files.
	 * See {@link #getDataOfProvider(String, long, long, boolean)} for details. If the result intervals overlap
	 * always the newst result shall be used.
	 */
	public GaRoSuperEvalResult<?> getAggregatedResult(String providerId, long startTime, long endTime,
			boolean includeOverlap);	
	
	/**Get instances KPIStatisticsManagement that can be used to access the KPI data
	 * */
	public List<KPIStatisticsManagement> getKPIManagement(Resource entryResource, String providerId);
	/** Get all KPIStatisticsManagement instances relevant for an entry resouce
	 */
	public List<KPIStatisticsManagement> getKPIManagement(Resource entryResource);
	
	/** Get all results for entryResource*/
	public List<JSONResultFileData> getDataOfResource(Resource entryResource);
}
