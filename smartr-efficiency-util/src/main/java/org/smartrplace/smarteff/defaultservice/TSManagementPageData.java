package org.smartrplace.smarteff.defaultservice;

import org.ogema.core.model.simple.IntegerResource;
import org.ogema.model.prototypes.Data;

@Deprecated //only relevant if own schedule manipulator is implemented, which is not planned currently
public interface TSManagementPageData extends Data {
	IntegerResource numberOfDatapointsToShowInManipulator();
}
