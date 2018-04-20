package org.smartrplace.extenservice.resourcecreate;

import java.util.List;

import org.ogema.core.model.Resource;
import org.smartrplace.extenservice.proposal.ProposalPublicData;
import org.smartrplace.extensionservice.gui.NavigationGUIProvider.PageType;
import org.smartrplace.extensionservice.gui.NavigationPublicPageData;

public interface ExtensionPageSystemAccessForPageOpening {
	List<NavigationPublicPageData> getPages(Class<? extends Resource> type);
	List<NavigationPublicPageData> getPages(Class<? extends Resource> type, boolean includeHidden);

	NavigationPublicPageData getMaximumPriorityPage(Class<? extends Resource> type, PageType pageType);
	List<NavigationPublicPageData> getStartPages();
	
	NavigationPublicPageData getPageByProvider(String url);
	List<ProposalPublicData> getProposalProviders(Class<? extends Resource> type);
	
	/**Get configId to put as parameter into page request when opening new page*/
	String accessPage(NavigationPublicPageData pageData, int entryIdx, List<Resource> entryResources);
	String accessPage(NavigationPublicPageData pageData, int entryIdx, List<Resource> entryResources, Object context);
}
