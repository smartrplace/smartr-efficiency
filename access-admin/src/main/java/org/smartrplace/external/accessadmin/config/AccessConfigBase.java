package org.smartrplace.external.accessadmin.config;

import org.ogema.core.model.array.IntegerArrayResource;
import org.ogema.core.model.array.StringArrayResource;
import org.ogema.model.prototypes.Data;

import de.iwes.widgets.template.LabelledItem;

/** 
 * Access configuration resource for rooms, users and groups of them.
 */
public interface AccessConfigBase extends Data {
	/** For resources on the system this is usually the id. For {@link LabelledItem}s like Datapoints this
	 * is the id. For resources this is the location. Note that all ArrayResources in a certain resource of
	 * type AccessConfigBase shall have the same length. For each combination of resourceId and permissionType
	 * a separate entry shall be made.
	 */
	StringArrayResource resourceIds();
	
	StringArrayResource permissionTypes();
	/** The following values shall be supported:
	 * 0 : Permission not granted
	 * >1 : Permission granted. A value greater than 1 may indicate additional permission info. This shall
	 *    always imply the basic permission, so a general check if the value is greater than zero thall be
	 *    possible.
	 */
	IntegerArrayResource permissionValues();
}
