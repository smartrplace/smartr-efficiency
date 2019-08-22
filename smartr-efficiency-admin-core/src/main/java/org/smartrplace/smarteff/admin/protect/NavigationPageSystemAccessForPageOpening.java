package org.smartrplace.smarteff.admin.protect;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.ogema.core.model.Resource;
import org.ogema.generictype.GenericDataTypeDeclaration;
import org.smartrplace.extensionservice.gui.NavigationGUIProvider.PageType;
import org.smartrplace.extensionservice.gui.NavigationPublicPageData;
import org.smartrplace.extensionservice.proposal.LogicProviderPublicData;
import org.smartrplace.extensionservice.resourcecreate.ExtensionPageSystemAccessForPageOpening;
import org.smartrplace.extensionservice.resourcecreate.ProviderPublicDataForCreate.PagePriority;
import org.smartrplace.smarteff.admin.util.ConfigIdAdministration;
import org.smartrplace.smarteff.admin.util.ResourceTypeToGUIInfo;
import org.smartrplace.smarteff.admin.util.SmartrEffUtil;

public class NavigationPageSystemAccessForPageOpening implements ExtensionPageSystemAccessForPageOpening {
	protected final Map<Class<? extends Resource>, ResourceTypeToGUIInfo> pageInfo;
	private final List<NavigationPublicPageData> startPagesData;
	protected final Map<Class<? extends Resource>, List<LogicProviderPublicData>> proposalInfo;
	protected final ConfigIdAdministration configIdAdmin;
	private final Resource myPrimaryResource;
	private final NavigationPublicPageData myNaviData;
	private final Object myContext;
	private final String myConfigId;
	
	public NavigationPageSystemAccessForPageOpening(
			Map<Class<? extends Resource>, ResourceTypeToGUIInfo> navigationPublicData,
			List<NavigationPublicPageData> startPagesData,
			ConfigIdAdministration configIdAdmin,
			Map<Class<? extends Resource>, List<LogicProviderPublicData>> proposalInfo,
			Resource myPrimaryResource, Object myContext, String myUrl,
			String myConfigId) {
		this.pageInfo = navigationPublicData;
		this.startPagesData = startPagesData;
		this.configIdAdmin = configIdAdmin;
		this.proposalInfo = proposalInfo;
		this.myNaviData = getPageByProvider(myUrl);
		this.myPrimaryResource = myPrimaryResource;
		this.myContext = myContext;
		this.myConfigId = myConfigId;
	}

	/*public NavigationPublicPageData getStdPage(Class<? extends Resource> type, PageType pageType) {
		ResourceTypeToGUIInfo l = navigationPublicData.get(type);
	}*/
	public static NavigationPublicPageData getStdPage(Class<? extends Resource> type, PageType pageType,
			ResourceTypeToGUIInfo l) {
		if(l == null) return null;
		switch(pageType) {
		case EDIT_PAGE:
			return l.stdEditPage;
		case TABLE_PAGE:
			return l.stdTablePage;
		}
		throw new IllegalStateException("Unknown page type requested: "+pageType+" for "+type);
	}
	
	@Override
	public List<NavigationPublicPageData> getPages(Class<? extends Resource> type) {
		return getPagesStatic(type, pageInfo);
	}
	public static List<NavigationPublicPageData> getPagesStatic(Class<? extends Resource> type,
			Map<Class<? extends Resource>, ResourceTypeToGUIInfo> pageInfoLoc) {
		return getPagesStatic(type, false, pageInfoLoc);
	}

	@Override
	public List<NavigationPublicPageData> getPages(Class<? extends Resource> type, boolean includeHidden) {
		return getPagesStatic(type, includeHidden, pageInfo);
	}
	public static List<NavigationPublicPageData> getPagesStatic(Class<? extends Resource> type, boolean includeHidden,
			Map<Class<? extends Resource>, ResourceTypeToGUIInfo> pageInfoLoc) {
		List<NavigationPublicPageData> result = new ArrayList<>();
		if(includeHidden) {
			ResourceTypeToGUIInfo l = pageInfoLoc.get(type);
			if(l != null) result.addAll(l.data);
			l = pageInfoLoc.get(Resource.class);
			if(l != null) result.addAll(l.data);
		} else {
			processResultType(result, pageInfoLoc.get(type));
			processResultType(result, pageInfoLoc.get(Resource.class));
		}
		return result;
	}
	private static void processResultType(List<NavigationPublicPageData> result, ResourceTypeToGUIInfo resourceTypeToGUIInfo) {
		if(resourceTypeToGUIInfo == null) return;
		for(NavigationPublicPageData r: resourceTypeToGUIInfo.data) {
			if(r.getPriority() != PagePriority.HIDDEN) result.add(r);
		}
	}

	@Override
	public NavigationPublicPageData getMaximumPriorityPage(Class<? extends Resource> type, PageType pageType) {
		return getMaximumPriorityPageStatic(type, pageType, pageInfo);
	}
	public static NavigationPublicPageData getMaximumPriorityPageStatic(Class<? extends Resource> type, PageType pageType,
			Map<Class<? extends Resource>, ResourceTypeToGUIInfo> pageInfoLoc) {
		NavigationPublicPageData stdPage = getStdPage(type, pageType, pageInfoLoc.get(type));
		if(stdPage != null) return stdPage;
		List<NavigationPublicPageData> resultAll = getPagesStatic(type, pageInfoLoc);
		if(resultAll == null || resultAll.isEmpty()) return null;
		NavigationPublicPageData result = null;
		for(NavigationPublicPageData r: resultAll) {
			if(r.getPageType() != pageType) continue;
			if(result == null) result = r;
			else if(SmartrEffUtil.comparePagePriorities(r.getPriority(), result.getPriority()) > 0) {
				result = r;
			}
		}
		return result;
	}
	
	@Override
	public NavigationPublicPageData getMaximumPriorityTablePage(Class<? extends Resource> primaryType,
			Class<? extends Resource> elementType) {
		List<NavigationPublicPageData> resultAll = getPages(primaryType);
		if(resultAll == null || resultAll.isEmpty()) return null;
		NavigationPublicPageData result = null;
		for(NavigationPublicPageData r: resultAll) {
			if(r.getPageType() != PageType.TABLE_PAGE) continue;
			if(r.typesListedInTable() == null) continue;
			for(GenericDataTypeDeclaration typeListed: r.typesListedInTable()) {
				if(typeListed.representingResourceType().equals(elementType)) {
					if(result == null) result = r;
					else if(SmartrEffUtil.comparePagePriorities(r.getPriority(), result.getPriority()) > 0) {
						result = r;
					}
				}
			}
		}
		return result;
	}
	
	@Override
	public	List<NavigationPublicPageData> getStartPages() {
		return new ArrayList<>(startPagesData);
	}
	
	@Override
	public NavigationPublicPageData getPageByProvider(String url) {
		for(ResourceTypeToGUIInfo list: pageInfo.values()) {
			for(NavigationPublicPageData navi: list.data) {
				if(navi.getUrl().equals(url)) return navi;
			}
		}
		for(NavigationPublicPageData navi: startPagesData) {
			if(navi.getUrl().equals(url)) return navi;
		}
		return null;
	}

	@Override
	public String accessPage(NavigationPublicPageData pageData, int entryIdx,
			List<Resource> entryResources) {
		return configIdAdmin.getConfigId(entryIdx, entryResources, myNaviData, myPrimaryResource, null,
				myContext, myConfigId);
	}
	@Override
	public String accessPage(NavigationPublicPageData pageData, int entryIdx, List<Resource> entryResources, Object context) {
		return accessPage(pageData, entryIdx, entryResources, context, false);
	}
	/** Note
	 * @param isBackLink if true the data for "lastPage" is not updated so that another step back can go back to the real
	 * 		previous page.
	 * @return
	 */
	@Override
	public String accessPage(NavigationPublicPageData pageData, int entryIdx, List<Resource> entryResources, Object context,
			boolean isBackLink) {
		return configIdAdmin.getConfigId(entryIdx, entryResources, isBackLink?null:myNaviData, myPrimaryResource, context, myContext, myConfigId);		
	}

	@Override
	public List<LogicProviderPublicData> getLogicProviders(Class<? extends Resource> type) {
		Collection<LogicProviderPublicData> resultAll;
		if(type == null) {
			resultAll = new HashSet<>();
			for(List<LogicProviderPublicData> logicList: proposalInfo.values()) {
				logicList.addAll(logicList);
			}
		} else {
			resultAll = proposalInfo.get(type);
			List<LogicProviderPublicData> resultRes = proposalInfo.get(Resource.class);
			if(resultRes != null) resultAll.addAll(resultRes);
		}
		if(resultAll == null) return Collections.emptyList();
		List<LogicProviderPublicData> result = new ArrayList<>();
		for(LogicProviderPublicData r: resultAll) {
			if(r.getPriority() != PagePriority.HIDDEN) result.add(r);
		}
		return result;
	}
	
	//TODO: Check if these methods make sense, otherwise remove them
	/*public static NavigationPublicPageData getMaximumPriorityPageStatic(Class<? extends Resource> type, PageType pageType,
			Map<Class<? extends Resource>, ResourceTypeToGUIInfo> navigationPublicData) {
		List<NavigationPublicPageData> resultAll = getPagesStatic(type, false, navigationPublicData);
		if(resultAll == null || resultAll.isEmpty()) return null;
		NavigationPublicPageData result = null;
		for(NavigationPublicPageData r: resultAll) {
			if(r.getPageType() != pageType) continue;
			if(result == null) result = r;
			else if(SmartrEffUtil.comparePagePriorities(r.getPriority(), result.getPriority()) > 0) {
				result = r;
			}
		}
		return result;
	}
	public static List<NavigationPublicPageData> getPagesStatic(Class<? extends Resource> type, boolean includeHidden,
			Map<Class<? extends Resource>, ResourceTypeToGUIInfo> navigationPublicData) {
		List<NavigationPublicPageData> result = new ArrayList<>();
		if(includeHidden) {
			ResourceTypeToGUIInfo l = navigationPublicData.get(type);
			if(l != null) result.addAll(l.data);
			l = navigationPublicData.get(Resource.class);
			if(l != null) result.addAll(l.data);
		} else {
			processResultTypeStatic(result, navigationPublicData.get(type));
			processResultTypeStatic(result, navigationPublicData.get(Resource.class));
		}
		return result;
	}

	private static void processResultTypeStatic(List<NavigationPublicPageData> result, ResourceTypeToGUIInfo resourceTypeToGUIInfo) {
		if(resourceTypeToGUIInfo == null) return;
		for(NavigationPublicPageData r: resourceTypeToGUIInfo.data) {
			if(r.getPriority() != PagePriority.HIDDEN) result.add(r);
		}
	}*/

}
