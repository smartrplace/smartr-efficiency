package extensionmodel.smarteff.api.base;

import org.ogema.core.model.array.StringArrayResource;
import org.ogema.core.model.simple.StringResource;
import org.ogema.generictype.GenericDataTypeDeclaration;
import org.smartrplace.efficiency.api.base.SmartEffResource;

/** This is a base resource for all value resources containing time series. Usually the data is
 * provided via a DriverProvider that provides specific DataProvider instances for each user
 * based on a certain DataProvider class.
 * Not all data provided by DataProviders should be represented
 * as such a resource, this is mainly intended for time series that are managed by the user
 * actively such as via file uploads.*/
public interface SmartEffTimeSeries extends SmartEffResource {
	/**DataProvider id that is used to provide the data.*/
	StringResource driverId();

	/**Ids of selection items required to obtain the respective time series from the
	 * DataProvider. Usually this is not stored, but time series are obtained via the
	 * entry resource, which usually is the parent of this resource.
	 */
	StringArrayResource selectionItemIds();
	
	/** The id of the respective {@link GenericDataTypeDeclaration}, e.g. GaRoDataType. See
	 * GenericTSDPTimeSeries for a documentation.
	 */
	StringResource inputTypeId();
}
