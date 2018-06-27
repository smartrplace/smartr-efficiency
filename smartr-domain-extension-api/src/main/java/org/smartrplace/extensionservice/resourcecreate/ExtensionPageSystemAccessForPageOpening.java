package org.smartrplace.extensionservice.resourcecreate;

import java.util.List;

import org.ogema.core.model.Resource;
import org.smartrplace.extensionservice.gui.NavigationGUIProvider.PageType;
import org.smartrplace.extensionservice.gui.NavigationPublicPageData;
import org.smartrplace.extensionservice.proposal.ProposalPublicData;

public interface ExtensionPageSystemAccessForPageOpening {
	List<NavigationPublicPageData> getPages(Class<? extends Resource> type);
	List<NavigationPublicPageData> getPages(Class<? extends Resource> type, boolean includeHidden);

	NavigationPublicPageData getMaximumPriorityPage(Class<? extends Resource> type, PageType pageType);
	List<NavigationPublicPageData> getStartPages();
	
	NavigationPublicPageData getPageByProvider(String url);
	List<ProposalPublicData> getLogicProviders(Class<? extends Resource> type);
	
	/**Get configId to put as parameter into page request when opening new page*/
	String accessPage(NavigationPublicPageData pageData, int entryIdx, List<Resource> entryResources);
	String accessPage(NavigationPublicPageData pageData, int entryIdx, List<Resource> entryResources, Object context);
}
