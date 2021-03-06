package org.smartrplace.extensionservice.gui;

import org.smartrplace.extensionservice.gui.NavigationGUIProvider.PageType;
import org.smartrplace.extensionservice.resourcecreate.ProviderPublicDataForCreate;

/** The public visualization page data is not directly provided by the ExtensionCapability of
 * type {@link NavigationGUIProvider}, but it is provided by the framework. The URL is generated
 * by the framework.
 */
public interface NavigationPublicPageData extends ProviderPublicDataForCreate {

	/**Relative URL on which the page can be accessed*/
	String getUrl();
	
	/** see {@link PageType}*/
	PageType getPageType();
	
	PageImplementationContext getPageContextData();
}
