package org.smartrplace.smarteff.admin.protect;

import org.smartrplace.extensionservice.gui.NavigationGUIProvider.PageType;
import org.smartrplace.extensionservice.gui.NavigationPublicPageData;
import org.smartrplace.smarteff.admin.object.NavigationPageData;

public class NavigationPublicPageDataImpl extends ProviderPublicDataForCreateImpl implements NavigationPublicPageData {
	private final NavigationPageData internalData;
	
	public NavigationPublicPageDataImpl(NavigationPageData internalData) {
		super(internalData.provider);
		this.internalData = internalData;
	}

	@Override
	public String getUrl() {
		return internalData.url;
	}

	@Override
	public PageType getPageType() {
		return internalData.provider.getPageType();
	}

	@Override
	public PagePriority getPriority() {
		return internalData.provider.getPriority();
	}

}
