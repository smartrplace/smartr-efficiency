package org.smartrplace.extensionservice.resourcecreate;

import java.util.List;

import org.ogema.core.model.Resource;
import org.smartrplace.critical.crossuser.ExtensionPageSystemAccessForCrossuserAccess;
import org.smartrplace.extensionservice.ExtensionUserData;
import org.smartrplace.extensionservice.ExtensionUserDataNonEdit;
import org.smartrplace.extensionservice.gui.NavigationPublicPageData;
import org.smartrplace.smarteff.access.api.ConfigInfoExt;

public interface ExtensionResourceAccessInitData {
	public static class ConfigInfo extends ConfigInfoExt {
		public ConfigInfo(int entryIdx, List<Resource> entryResources) {
			super(entryIdx, entryResources);
		}
		public ConfigInfo(ConfigInfoExt cext) {
			super(cext.entryIdx, cext.entryResources);
			this.context = cext.context;
			this.lastPrimaryResource = cext.lastPrimaryResource;
		}
		public NavigationPublicPageData lastPage;
	}

	/** index within {@link #getEntryTypes()} used to open the page*/
	int entryTypeIdx();
	
	/** resources of the entry type specified by entryTypeIdx. If the cardinality of
	 * 		the EntryType does not allow multiple entries the list will only contain a single element. If
	 * 		the cardinality allows zero the list may be empty.
	 */
	List<Resource> entryResources();
	//List<GenericDataTypeDeclaration> entryData();
	
	ConfigInfo getConfigInfo();

	/**Domain-specific reference to user data.
	 */
	ExtensionUserData userData();
	
	/** User data than cannot be edited by the user*/
	ExtensionUserDataNonEdit userDataNonEdit();
	
	/** Access for module for resource creation process. This is an extended version of
	 * {@link #systemAccessForPageOpening()} and can only be used by modules that create resources.*/
	ExtensionPageSystemAccessForCreate systemAccess();

	/** Access for module for simple page opening, supported also for page openings without
	 * valid configId*/
	ExtensionPageSystemAccessForPageOpening systemAccessForPageOpening();
	
	ExtensionPageSystemAccessForEvaluation getEvaluationManagement();
	
	ExtensionPageSystemAccessForTimeseries getTimeseriesManagement();
	
	ExtensionPageSystemAccessForCrossuserAccess getCrossuserAccess();
	
	public interface PublicUserInfo {
		String userName();
		boolean isAnonymousUser();
	}
	PublicUserInfo getUserInfo();
	
	
}
