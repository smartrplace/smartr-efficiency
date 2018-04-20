package org.smartrplace.smarteff.admin.object;

import org.smartrplace.efficiency.api.base.SmartEffExtensionService;
import org.smartrplace.extenservice.resourcecreate.ExtensionResourceAccessInitData;
import org.smartrplace.extensionservice.gui.ExtensionNavigationPageI;
import org.smartrplace.extensionservice.gui.NavigationGUIProvider;

import extensionmodel.smarteff.api.base.SmartEffUserDataNonEdit;

public class NavigationPageData {
	public final NavigationGUIProvider provider;
	public final SmartEffExtensionService parent;
	public final String url;
	public final ExtensionNavigationPageI<SmartEffUserDataNonEdit, ExtensionResourceAccessInitData> dataExPage;
	
	public NavigationPageData(NavigationGUIProvider provider, SmartEffExtensionService parent, String url,
			final ExtensionNavigationPageI<SmartEffUserDataNonEdit, ExtensionResourceAccessInitData> dataExPage) {
		this.provider = provider;
		this.parent = parent;
		this.url = url;
		this.dataExPage = dataExPage;
	}
	
}
