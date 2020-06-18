package org.smartrplace.external.accessadmin.config;

import org.ogema.core.model.ResourceList;
import org.ogema.core.model.ValueResource;
import org.ogema.core.model.simple.BooleanResource;
import org.ogema.core.model.simple.StringResource;
import org.ogema.model.locations.BuildingPropertyUnit;
import org.ogema.model.prototypes.Data;
import org.ogema.model.user.NaturalPerson;

/** 
 * Access configuration resource for users and groups of them.
 */
public interface AccessConfigUser extends Data {

	/** If false the configuration is usually linked to a natural user, but may also be linked to a REST user*/
	BooleanResource isGroup();

	@Override
	/** Name of user or group. For natural users this is the user name.*/
	StringResource name();

	/** This value may exist if the access configuration is linked to a single user and does not
	 * represent a group. As such resources may not exist on all OGEMA systems the value may not be set,
	 * though.
	 */
	NaturalPerson user();
	
	/** Direct super groups. The list shall only contain references to these settings.
	 * All settings are inherited that are not overwritten by an own setting in
	 * this group. In case of conflicts for a certain settings provided by two direct superGroups the
	 * first entry is used.*/
	ResourceList<AccessConfigUser> superGroups();
	
	/** Permissons for rooms, room groups represented by {@link BuildingPropertyUnit}s*/
	AccessConfigBase roompermissionData();

	/** Permissons for devices and all other resources if relevant. Permissions on {@link ValueResource}s
	 * usually should be represented by {@link #datapointPermissionData()}*/
	AccessConfigBase otherResourcepermissionData();
	
	/** Permissons for Datapoints
	 *  TODO: As datapoints are not stored persistently also no permissions should be defined for them. Permissions
	 *  shall be based on the resources used to provide Datapoints.
	 */
	//ResourceList<AccessConfigBase> datapointPermissionData();

	/** Permissons for apps in the appstore. If a user has no permissions for upload or download
	 * in the appstore the list should not exist or be empty*/
	AccessConfigBase appstorePermissionData();
}
