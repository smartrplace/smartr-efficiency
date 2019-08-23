package org.smartrplace.critical.crossuser;

import java.util.List;

import org.ogema.core.model.Resource;

public interface ExtensionPageSystemAccessForCrossuserAccess {
	/** This methods allows modules to get access to data from a certain user, usually the provider
	 * of the module or provider of parameters that are not managed globally but by the module
	 * provider. A typical case is the provision of internal project proposal calculation parameters
	 * (e.g. cost parameters) by a vendor that are required to provide cost information for a project
	 * proposal planning but that shall not be made public to the users receiving the proposals.<br>
	 * Note that this method only allows to access resources that are marked with the relevant
	 * {@link AccessControl#modules()} entry. The user() entries are not relevant here.
	 * 
	 * @param subUserPath path below the editable user data of the resource that shall be accessed.
	 * 		Note that currently only resources in the editable space can be accessed cross-user with
	 * 		this method.
	 * @param userName userName of user providing the data that shall be accessed
	 * @param type resource type to be accessed
	 * @param module4authentication the module that wants to use the data has to present itself to
	 * 		the implementation so that the framework can check whether the class path of the module
	 * 		is allowed to access the resource.
	 * @return
	 */
	<T extends Resource> T getAccess(String subUserPath, String userName, Class<T> type, Object module4authentication);
	
	/** Get all cross-user resources of a type that can be accessed by the user
	 * 
	 * @param type if null all cross-user resources are returned
	 */
	<T extends Resource> List<T> getAccess(Class<T> type);
	
	/** Copy resource into a suitable ResourceList of the destination user. The respective
	 * ResourceList must be top-level in the user data and must contain a AccessControl.modules()
	 * entry for "receiveResources".<br>
	 * Note that in most cases the receiver should get information on the time and the source user
	 * of the resource. So usually copyResourceIntoOffer should be used, although this may not be
	 * general enough for all cases.
	 * 
	 * @param source
	 * @param destinationUserName
	 * @return
	 */
	public <T extends Resource> T copyResource(T source, String destinationUserName);
	/** See {@link #copyResource(Resource, String)}
	 * 
	 * @param source
	 * @param destinationUserName
	 * @param sourceUserName
	 * @param purpose 1: request offer, 2: review, 3: add your data, 4: request consulting
	 * @return
	 */
	public <T extends Resource> T copyResourceIntoOffer(T source,
			String destinationUserName, String sourceUserName, int purpose);
}