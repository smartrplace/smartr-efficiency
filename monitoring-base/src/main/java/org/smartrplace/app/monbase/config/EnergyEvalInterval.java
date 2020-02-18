package org.smartrplace.app.monbase.config;

import org.ogema.core.model.ResourceList;
import org.ogema.core.model.simple.StringResource;
import org.ogema.core.model.simple.TimeResource;
import org.ogema.model.prototypes.Data;

/** The interval concept shall be extended to KPIs in the future. In
 * this way is shall not only be possible to calculate KPIs for days, weeks etc., but it
 * shall also be possible to define custom evaluation intervals for which KPIs are calculated
 * and stored.
 */
public interface EnergyEvalInterval extends Data {
	TimeResource start();
	/** If end is not active the interval is ongoing*/
	TimeResource end();
	
	ResourceList<EnergyEvalIntervalMeterData> meterData();
	
	StringResource description();
}
