package org.smartrplace.extensionservice.driver;

import java.util.List;

import org.ogema.core.model.Resource;
import org.smartrplace.extensionservice.ApplicationManagerSPExt;
import org.smartrplace.extensionservice.resourcecreate.ExtensionResourceAccessInitData;

/** see {@link DriverProvider}
 *
 */
public interface SynchronizerProvider extends SynchronizerProviderPublicData {
	void init(ApplicationManagerSPExt appManExt);
	
	/** Start synchronizer. If push/pull operations are performed a separate thread, timer or
	 * listener(s) should be started. If the synchronizer works as server the actual operations
	 * are done in the request threads of the server. This operation shall only be used by the
	 * framework.
	 * 
	 * @param entryTypeIdx index within {@link #getEntryTypes()} used to open the page
	 * @param entryResources resources of the entry type specified by entryTypeIdx. If the cardinality of
	 * 		the EntryType does not allow multiple entries the list will only contain a single element. If
	 * 		the cardinality allows zero the list may be empty.
	 * @param userData domain-specific reference to user data. May also be obtainable just as parent of the resource.
	 * @param listener when the user presses a "Save" button or finishes editing otherwise, finishing of editing
	 *		 shall be notified to the main domain app so that it can activate resources etc.
	 * @return resources created and modified. The first element should contain the most important result and the
	 * 		further order of the list should reflect the relevance of the changes (if possible) 
	 */
	List<Resource> startOperation(ExtensionResourceAccessInitData data);
}
