package org.smartrplace.extensionservice.driver;

import java.util.List;

import org.ogema.core.model.Resource;
import org.smartrplace.extensionservice.ExtensionCapability;

/** see {@link DriverProvider}
 *	This is a driver sychronizing data via Pull/Push or server operations.
 *  TODO: Add additional specification to access information on last/outstanding
 *  synchronization tasks, update rate etc.
 */
public interface SynchronizerProviderPublicData extends ExtensionCapability {
	
	/** Trigger synchronization operation manually
	 * @param resourcesToSynch usually a subset of the resources returned by
	 * {@link SynchronizerProvider#startOperation(org.smartrplace.extensionservice.resourcecreate.ExtensionResourceAccessInitData)}
	 * TODO: Chech how ExtensionCapabilities can access this information.*/
	List<Resource> triggerSynchronization(List<Resource> resourcesToSynch);

}
