package org.smartrplace.extensionservice;

import org.ogema.core.model.array.StringArrayResource;
import org.ogema.core.model.array.TimeArrayResource;
import org.ogema.core.model.schedule.Schedule;
import org.ogema.core.model.simple.BooleanResource;
import org.ogema.core.model.simple.FloatResource;
import org.ogema.core.model.simple.StringResource;
import org.ogema.generictype.GenericDataTypeDeclaration;
import org.ogema.model.prototypes.Data;
import org.smartrplace.smarteff.model.syste.GenericTSDPTimeSeries;

import de.iwes.timeseries.eval.garo.api.base.GaRoMultiEvaluationInput;

/** This is a base resource for all value resources containing time series. Usually the data is
 * provided via a DriverProvider that provides specific DataProvider instances for each user
 * based on a certain DataProvider class.
 * Not all data provided by DataProviders should be represented
 * as such a resource, this is mainly intended for time series that are managed by the user
 * actively such as via file uploads.
 * Only one of the 4 options below should be active. If Option 1 is a reference to a schedule below
 * {@link #recordedDataParent()} then {@link #recordedDataParent()} should NOT be active, but the
 * schedule should be active. Currently we support only a single mode for each time series. So
 * if you want to import data via CSV and also manipulate data manually then you have to copy
 * all data into a schedule.
 */
public interface SmartEffTimeSeries extends Data {
	/**DataProvider id that is used to provide the data.*/
	StringResource driverId();

	/** The id of the respective {@link GenericDataTypeDeclaration}. Note that this usually is a
	 * standard GaRoDataType and that we currently use the label(null) of these as they all have
	 * the same id (TODO: Check if this could be changed). Non-standard GaRoDataTypes currently
	 * cannot be processed by {@link GaRoMultiEvaluationInput#itemSelector()} and thus will not
	 * be found as input for evaluations. Such non-standard GaRoTypes usually occur as a result
	 * of another evaluation (which is a pre-evaluation for another evaluation) and here currently
	 * only the JSON file reading pre-evaluation mechanism is supported, no other time series injection
	 * as input.<br>
	 */
	StringResource dataTypeId();
	
	/**If more than one time series for the same entryResource and the same data type shall be reported,
	 * the sourceId has to be set
	 */
	StringResource sourceId();
	
	/** Option 1: Storage in schedule. This can only be used as a reference. If you need to create a schedule
	 * here, use {@link #recordedDataParent()} as parent.*/
	Schedule schedule();
	
	/**Option 2: Storage in RecordedData of the SingleValueResource. In many cases this is a
	 * reference to the resource that actually logs the data. Use this also if
	 * {@link FloatResource#historicalData()} shall be used for mixed historical data or just
	 * written historical data.*/
	FloatResource recordedDataParent();
	
	/**Option 3: Storage in files*/
	GenericTSDPTimeSeries filePaths();
	/** The type may be obtainable from filePath, but the details which module is able to
	 * read the file may be given here. Note that additional parameters may be given in inherited
	 * resource types or decorators. Typical file types are CSV and JSON.
	 */
	StringResource fileType();
	
	/**Option 4: The time series can be stored outside OGEMA and is obtainable via a DataProvider.
	 * In order to make it accessible via a DriverProvider the DataProvider have to be
	 * wrapped by a (usually specific) DriverProvider (see DataDriverGaroJAXB for an example).
	 * Usually it is not necessary to declare such a time series as SmartEffTimeSeries as it
	 * cannot be edited or otherwise managed, e.g. via TSManagementPage. But in some cases it
	 * might still be relevant in the future.*/
	StringResource dataProviderId();
	/** Can also be used as parameter to read from JSON files from GaRo evaluations*/
	StringResource dataProviderGatewayId();
	StringResource dataProviderRoomId();
	/**Ids of selection items required to obtain the respective time series from the
	 * DataProvider as an alternative to gatewayId and roomId. Usually this is not stored, but time series are obtained via the
	 * entry resource, which usually is the parent of this resource.
	 */
	StringArrayResource selectionItemIds();

	/** If true NaN values can be added to the time series manually and via file upload. Default is false
	 * which means that NaN values cannot be entered and shall be omitted/replaced by file importers.
	 * @return
	 */
	BooleanResource allowNanValues();
	
	/** comments and commentTimeStamps must always have same length and same index*/
	StringArrayResource comments();
	TimeArrayResource commmentTimeStamps();
}
