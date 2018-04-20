package org.smartrplace.extenservice.resourcecreate;

import org.ogema.core.model.Resource;
import org.smartrplace.extensionservice.ExtensionResourceTypeDeclaration;
import org.smartrplace.extensionservice.gui.NavigationPublicPageData;

public interface ExtensionPageSystemAccessForCreate extends ExtensionPageSystemAccessForPageOpening{
	public String accessCreatePage(NavigationPublicPageData pageData, int entryIdx,
			Resource parent);
	
	public enum ResourceAccessResult {
		OK,
		NOT_ALLOWED,
		RESOURCE_LOCKED_BY_YOURSELF,
		/** This is also returned if not sufficient user information is available*/
		RESOURCE_LOCKED_BY_OTHERUSER,
		/** If getNewResource is called on an existing resource this is returned. Applications should only
		 * call the method when they really expect a new resource.
		 */
		RESOURCE_ALREADY_EXISTS,
		/** This is a hint that two applications use the same resource path/name.
		 * TODO: We might need a specification to avoid this.
		 */
		RESOURCE_ALREADY_EXISTS_DIFFENT_TYPE,
		/** For cardinality single already a sub resource of this type exists*/
		SINGLE_RESOURCETYPE_ALREADY_EXISTS,
		/** Can be returned when an exception in the admin application occurs*/
		SYSTEM_ERROR
	}
	public class LockResult {
		public ResourceAccessResult result;
		/** If the requested resource is locked the system may provide information here which
		 * module locked the resource
		 */
		public String lockingApplicationName;
	}
	/** Notify system that you want to write into the resource. A GUI page should test and acquire the lock
	 * before it opens the respective editing page to the user and show a message that the resource is locked
	 * as a message to the user
	 * @param resource
	 * @return null if access is not allowed
	 */
	LockResult lockResource(Resource resource);
	/** Unlock resource
	 * 
	 * @param resource
	 * @param activate if true the method behaves like {@link #activateResource(Resource)}
	 */
	void unlockResource(Resource resource, boolean activate);
	
	boolean isLocked(Resource resource);

	public class NewResourceResult<T extends Resource> {
		public ResourceAccessResult result;
		public T newResource;
	}
	/** With getNewResource the newly created resource is automatically locked*/
	<T extends Resource> NewResourceResult<T> getNewResource(Resource parent, String name, ExtensionResourceTypeDeclaration<T> type);
	<T extends Resource> NewResourceResult<T> getNewResource(T virtualResource);
	
	/** Activates a resource and all its childres and unlocks the resource. This should usually be called to unlock
	 * after extensive editing as new subresources may have been created*/
	void activateResource(Resource resource);
}
