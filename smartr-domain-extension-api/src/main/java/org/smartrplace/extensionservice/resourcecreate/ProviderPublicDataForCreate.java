package org.smartrplace.extensionservice.resourcecreate;

import java.util.Collections;
import java.util.List;

import org.ogema.core.model.Resource;
import org.smartrplace.extensionservice.ExtensionCapabilityPublicData;

public interface ProviderPublicDataForCreate extends ExtensionCapabilityPublicData {
	/** Types that can be created by the GUI provider. Note that further classes may be edited. Usually this
	 * should either be an empty list or a list containing a single element. If more than one element is give
	 * the creator may choose one of the options to create based on the entry data of the actual request or
	 * may create all of the types declared with one editing process. This is organized by one of the 
	 * variants of {@link ExtensionPageSystemAccessForCreate#getNewResource(Resource)*/
	default List<Class<? extends Resource>> createTypes() {
		return Collections.emptyList();
	};
	
	public enum PagePriority {
		STANDARD,
		SECONDARY,
		/** Hidden pages can only be accessed via URL directly*/
		HIDDEN
	}
	default PagePriority getPriority() {
		return PagePriority.STANDARD;
	}
}
