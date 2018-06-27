package org.smartrplace.extensionservice.resourcecreate;

import java.util.List;

import org.ogema.core.model.Resource;
import org.ogema.model.jsonresult.JSONResultFileData;
import org.ogema.model.jsonresult.MultiKPIEvalConfiguration;
import org.ogema.util.directresourcegui.kpi.KPIStatisticsManagement;
import org.smartrplace.extensionservice.driver.DriverProvider;

import de.iwes.timeseries.eval.garo.api.base.GaRoSuperEvalResult;
import de.iwes.timeseries.eval.garo.multibase.GaRoSingleEvalProvider;

public interface ExtensionPageSystemAccessForEvaluation {
	
	/** The method creates or uses an instance of {@link MultiKPIEvalConfiguration} to calculate
	 * KPIs. The subConfigId is the location of the input resource. The input resource will also
	 * be given as {@link MultiKPIEvalConfiguration#configurationResource()} to the evaluation.
	 * This instance of MultiKPIEvalConfiguration is put as reference into a resource list used or created as
	 * sub resource of the input resource named "multiKPIEvalConfiguration".<br>
	 * Apps shall have read access to this resource, but not write access.<br>
	 * By default only the base interval is calculated. This can be adapted in the
	 * MultiKPIEvalConfiguration, but for the first evaluation when the configuration is created the
	 * standard is used. Higher intervals can still be calculated via
	 * {@link KPIStatisticsManagement#updateUpperTimeSteps(int, int[])}.
	 * 
	 * @param eval
	 * @param entryResource
	 * @param userData
	 * @param userDataNonEdit
	 * @param drivers may be null, then all available drivers will be tested if they can provide
	 * 		data for the entryResource. Note that currently users do not have access to
	 * 		DataProvider control so usually null has to be set here and all active
	 * 		DataProviders are used.
	 * @param stepInterval base interval for the configuration. If null the standard base
	 * 		interval is used. Note that setting this stepInterval is forced so it is highly
	 * 		recommended to use only a single value for a certain evaluation provider and not change it.
	 * @return
	 */
	public long[] calculateKPIs(GaRoSingleEvalProvider eval, Resource entryResource,
			Resource configurationResource,
			List<DriverProvider> drivers, boolean saveJsonResult,
			int defaultIntervalsToCalculate, Integer stepInterval);
	
	public long[] calculateKPIs(GaRoSingleEvalProvider eval, Resource entryResource,
			Resource configurationResource, List<DriverProvider> drivers, boolean saveJsonResult,
			long startTime, long endTime, Integer stepInterval);

	
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
