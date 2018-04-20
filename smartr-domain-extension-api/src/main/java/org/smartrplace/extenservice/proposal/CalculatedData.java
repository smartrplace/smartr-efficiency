package org.smartrplace.extenservice.proposal;

import org.ogema.core.model.Resource;
import org.ogema.core.model.ResourceList;
import org.ogema.model.prototypes.Data;

public interface CalculatedData extends Data {
	/** Typically references to input data are given. If the values shall be preserved
	 * also copies of the input resources at the time of calculation can be provided
	 */
	ResourceList<Resource> inputData();
}
