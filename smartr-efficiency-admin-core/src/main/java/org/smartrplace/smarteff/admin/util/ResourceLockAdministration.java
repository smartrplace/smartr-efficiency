package org.smartrplace.smarteff.admin.util;

import java.util.HashMap;
import java.util.Map;

import org.ogema.core.model.Resource;
import org.smartrplace.extenservice.resourcecreate.ExtensionPageSystemAccessForCreate.LockResult;
import org.smartrplace.extenservice.resourcecreate.ExtensionPageSystemAccessForCreate.ResourceAccessResult;

/** TODO: This is not thread-safe yet
 * TODO: Locking mechanisms are not really implemented yet*/
public class ResourceLockAdministration {
	private class LockInfo {
		public LockInfo(String userName, String application) {
			this.userName = userName;
			this.application = application;
		}
		String userName;
		String application;
	}
	Map<Resource, LockInfo> resourcesLocked = new HashMap<>();
	
	public LockResult lockResource(Resource resource, String userName, String application) {
		LockResult result = new LockResult();
		boolean alreadyExists = isLocked(resource);
		if(!alreadyExists) {
			resourcesLocked.put(resource, new LockInfo(userName, application));
			result.result = ResourceAccessResult.OK;
			return result;
		}
		LockInfo li = resourcesLocked.get(resource);
		if(userName != null && userName.equals(li.userName))
			result.result = ResourceAccessResult.RESOURCE_LOCKED_BY_YOURSELF;
		else
			result.result = ResourceAccessResult.RESOURCE_LOCKED_BY_OTHERUSER;
		result.lockingApplicationName = li.application;
		return result;
	}
	
	public void unlockResource(Resource resource) {
		resourcesLocked.remove(resource);
	}

	public boolean isLocked(Resource resource) {
		return resourcesLocked.containsKey(resource);
	}
}
