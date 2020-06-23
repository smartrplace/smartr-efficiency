package org.smartrplace.external.accessadmin.config;

import org.ogema.core.model.ResourceList;
import org.ogema.model.locations.BuildingPropertyUnit;
import org.ogema.model.prototypes.Configuration;

/** 
 * The global configuration resource type for this app.
 */
public interface AccessAdminConfig extends Configuration {

	/** Permissions for users and user groups*/
	ResourceList<AccessConfigUser> userPermissions();
	
	/** The list contains not only real property units, but all kinds of room groups.
	 * These groups can be used also for other purposes besides permission management
	 */
	ResourceList<BuildingPropertyUnit> roomGroups();
	
	AccessConfigBase userStatusPermission();
}
