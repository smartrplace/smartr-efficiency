package org.smartrplace.smarteff.admin.timeseries;

import org.ogema.core.model.ResourceList;
import org.smartrplace.efficiency.api.base.SmartEffResource;
import org.smartrplace.smarteff.model.syste.GenericTSDPTimeSeries;

/** The generic time series DriverProvider includes various input that provides ReadOnlyTimeSeries
 * and includes them into a GaRo data provider<br>
 * In the future this DataProvider may support to include conversion modules for different
 * file types via an OSGi service. This is not implemented yet, so such modules are just clases
 * in  this DataProvider now.*/
@Deprecated
public interface GenericTSDPConfigBak extends SmartEffResource {
	/** Time series to be offered by the DataProvider*/
	ResourceList<GenericTSDPTimeSeries> timeseries();
}
