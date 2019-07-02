package org.smartrplace.extensionservice;

import org.ogema.core.model.ResourceList;
import org.ogema.core.model.array.FloatArrayResource;
import org.ogema.model.prototypes.Data;

public interface SmartEff2DMap extends Data {
	/** 2D characteristics map
	* Each element contains values for a single primary key
	* so the size of this list must be equal to
	* {@link #primaryKeys()}. Each FloatArrayResource
	must be the size of {@link #secondaryKeys()}.
	*/
	ResourceList<SmartEff2DMapPrimaryValue> characteristics();
	
	/** Primary keys of the map. Must from lowest to highest. */
	FloatArrayResource primaryKeys();
	/** Secondary keys of the map. Must from lowest to highest. */
	FloatArrayResource secondaryKeys();
	
}
