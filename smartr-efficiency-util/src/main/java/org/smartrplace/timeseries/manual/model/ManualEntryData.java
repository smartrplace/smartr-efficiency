package org.smartrplace.timeseries.manual.model;

import org.ogema.core.model.ResourceList;
import org.ogema.core.model.simple.FloatResource;
import org.ogema.core.model.simple.StringResource;
import org.ogema.model.prototypes.Data;

public interface ManualEntryData extends Data {
	/** Note: Usually this ResourceList contains only a single entry. For electricity and heat meters
	 * this is a resoure of type EnergyResource, named energySensor
	 */
	ResourceList<FloatResource> manualEntryDataHolder();
	
	//TODO: We need backend page to edit these values
	StringResource manualEntryUnit();
	//StringArrayResource manualEntryTypeNames();
	
	FloatResource lowerLimit();
	FloatResource upperLimit();
}
