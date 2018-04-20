package org.smartrplace.extensionservice.gui;

import org.smartrplace.extensionservice.gui.NavigationGUIProvider.PageType;
import org.smartrplace.extensionservice.resourcecreate.ProviderPublicDataForCreate;

public interface NavigationPublicPageData extends ProviderPublicDataForCreate {

	/**Relative URL on which the page can be accessed*/
	String getUrl();
	
	/** see {@link PageType}*/
	PageType getPageType();
}
