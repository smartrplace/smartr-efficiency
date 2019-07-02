package org.smartrplace.extensionservice;

import org.ogema.core.model.array.FloatArrayResource;
import org.ogema.core.model.simple.FloatResource;
import org.ogema.core.model.simple.IntegerResource;
import org.ogema.model.prototypes.Data;

/**
 * Packs the FloatArrayResource of values together with an index, in order to keep the SmartEff2DMap in order.
 * This is necessary because we can't depend on a ResourceList<FloatArrayResources> to always be sorted correctly,
 * as the order changes if one of the FloatArrayResources is modified.
 * @author jruckel
 *
 */
public interface SmartEff2DMapPrimaryValue extends Data {
	
	/** Index of the primary value. Zero corresponds to the lowest key */
	IntegerResource index();
	
	/** FloatArrayResource of secondary values */
	FloatArrayResource val();
	
}
