package org.smartrplace.extensionservice;

import org.ogema.core.model.ResourceList;
import org.ogema.core.model.array.FloatArrayResource;

public interface SmartEff2DMap {
	/** 2D characteristics map
	* Each element contains values for a single primary key
	* so the size of this list must be equal to
	* {@link #primaryKeys()}. Each FloatArrayResource
	must be the size of {@link #secondaryKeys()}.
	*/
	ResourceList<FloatArrayResource> characteristics();
	FloatArrayResource primaryKeys();
	FloatArrayResource secondaryKeys();
}
