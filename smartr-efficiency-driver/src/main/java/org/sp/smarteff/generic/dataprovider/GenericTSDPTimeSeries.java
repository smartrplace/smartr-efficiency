package org.sp.smarteff.generic.dataprovider;

import org.ogema.core.model.Resource;
import org.ogema.core.model.array.StringArrayResource;
import org.ogema.core.model.schedule.Schedule;
import org.ogema.core.model.simple.SingleValueResource;
import org.ogema.core.model.simple.StringResource;
import org.ogema.generictype.GenericDataTypeDeclaration;
import org.smartrplace.efficiency.api.base.SmartEffResource;
import org.smartrplace.extensionservice.resourcecreate.ExtensionPageSystemAccessForEvaluation;

import extensionmodel.smarteff.api.base.SmartEffTimeSeries;

/** This is a base resource for all value resources containing time series. Usually the entry
 * corresponds to an entry of type {@link SmartEffTimeSeries}. If the data for such a time
 * series is provided by a different DataProvider no such entry is required. As not all time series
 * relevant for evaluations have a {@link SmartEffTimeSeries} entry there may be {@link GenericTSDPTimeSeries}
 * configurations without a SmartEffTimeSeries.<br>
 * The configuration only supports sources from a single type per time series.*/
public interface GenericTSDPTimeSeries extends SmartEffResource {
	/** Reference to an explicit time series declaration for user management*/
	SmartEffTimeSeries userTimeSeries();
	/** As an alternative an entry resource can be provided. If no entry resource is given the
	 * parent of useTimeSeries is the entry resource. Note that the entryResource is the
	 * fundamental mechanism of matching the DataProvider internal structure to the resources
	 * of SmartrEff in the method {@link ExtensionPageSystemAccessForEvaluation#calculateKPIs(de.iwes.timeseries.eval.garo.multibase.GaRoSingleEvalProvider, Resource, Resource, java.util.List, boolean, int)}
	 * and getKPIManagement. Matching to specific Input types like GaRoTypes is done directly
	 * by applying the evaluation and the providers to the GaRoTeststarter, but to include an
	 * arbitrary ReadOnlyTimeseries into a GaRo-DataProvider also the inputTypeId (see below) is
	 * required.
	 */
	Resource entryResource();
	
	/** The id of the respective {@link GenericDataTypeDeclaration}, e.g. GaRoDataType.
	 */
	StringResource inputTypeId();
	
	/** Option 1: Storage in schedule*/
	Schedule schedule();
	
	/**Option 2: Storage in RecordedData of the SingleValueResource*/
	SingleValueResource recordedDataParent();
	
	/**Option 3: Storage in files*/
	StringArrayResource filePaths();
	/** The type may be obtainable from filePath, but the details which module is able to
	 * read the file may be given here. Note that additional parameters may be given in inherited
	 * resource types or decorators. Typical file types are CSV and JSON.
	 */
	StringResource fileType();
	
	/**Option 4: The DataProvider can wrap other data provider (TODO: not implemented yet).*/
	StringResource dataProviderId();
	/** Can also be used as parameter to read from JSON files from GaRo evaluations*/
	StringResource dataProviderGatewayId();
	StringResource dataProviderRoomId();

}
