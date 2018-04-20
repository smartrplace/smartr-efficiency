package org.smartrplace.extenservice.resourcecreate;

import java.util.List;

import org.ogema.core.model.Resource;
import org.smartrplace.extensionservice.ExtensionUserData;
import org.smartrplace.extensionservice.ExtensionUserDataNonEdit;
import org.smartrplace.extensionservice.gui.NavigationPublicPageData;

public interface ExtensionResourceAccessInitData {
	public static class ConfigInfo {
		public ConfigInfo(int entryIdx, List<Resource> entryResources) {
			this.entryIdx = entryIdx;
			this.entryResources = entryResources;
		}
		public int entryIdx;
		public List<Resource> entryResources;
		
		public Resource lastPrimaryResource;
		public NavigationPublicPageData lastPage;
		
		public Object context; 
	}

	/** index within {@link #getEntryTypes()} used to open the page*/
	int entryTypeIdx();
	
	/** resources of the entry type specified by entryTypeIdx. If the cardinality of
	 * 		the EntryType does not allow multiple entries the list will only contain a single element. If
	 * 		the cardinality allows zero the list may be empty.
	 */
	List<Resource> entryResources();
	
	ConfigInfo getConfigInfo();

	/**Domain-specific reference to user data.
	 */
	ExtensionUserData userData();
	
	/** User data than cannot be edited by the user*/
	ExtensionUserDataNonEdit userDataNonEdit();
	
	/** Access for module for resource creation process*/
	ExtensionPageSystemAccessForCreate systemAccess();

	/** Access for module for simple page opening, supported also for page openings without
	 * valid configId*/
	ExtensionPageSystemAccessForPageOpening systemAccessForPageOpening();
}
