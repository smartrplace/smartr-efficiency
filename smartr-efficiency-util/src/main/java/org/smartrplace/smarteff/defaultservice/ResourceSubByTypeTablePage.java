package org.smartrplace.smarteff.defaultservice;

import java.util.ArrayList;
import java.util.List;

import org.ogema.core.model.Resource;
import org.ogema.tools.resource.util.ResourceUtils;
import org.smartrplace.extensionservice.ExtensionCapabilityPublicData.EntryType;
import org.smartrplace.smarteff.util.CapabilityHelper;

import de.iwes.widgets.api.widgets.localisation.OgemaLocale;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;

public class ResourceSubByTypeTablePage extends ResourceByTypeTablePage {
	public ResourceSubByTypeTablePage() {
		super();
	}
	
	@Override
	protected List<Resource> provideResourcesInTable(OgemaHttpRequest req) {
		//ExtensionResourceAccessInitData appData = exPage.getAccessData(req);
		Resource parent = getReqData(req);
		Class<? extends Resource> type = typeSelected(req);
		List<? extends Resource> resultAll = parent.getSubResources(type, true);
		List<Resource> result = new ArrayList<>(resultAll);
		/*for(Resource r: resultAll) {
			result.add(r);
		}*/
		return result;
	}
	
	@Override
	protected Class<Resource> primaryEntryTypeClass() {
		return Resource.class;
	}
	
	@Override //optional
	public String pid() {
		return ResourceSubByTypeTablePage.class.getSimpleName();
	}

	@Override
	protected String label(OgemaLocale locale) {
		return "Resource-by-type-by-location Overview Table";
	}

	@Override
	protected String getHeader(OgemaHttpRequest req) {
		return "Resources of type "+typeSelected(req)+" below "+ResourceUtils.getHumanReadableName(getReqData(req));
	}
	
	@Override
	protected List<EntryType> getEntryTypes() {
		return CapabilityHelper.getStandardEntryTypeList(primaryEntryTypeClass());
	}
	
	//From Resource TablePage
	/*protected void addWidgetsAboveTable(Class<? extends Resource> resourceType) {
		tabButton = new TabButton(page, "tabButton", pid());
		
		RedirectButton editResource = new AddEditButton(page, "editEntry", pid(), exPage,
				tabButton.control);
		TableOpenButton allResourceButton2;
		if(isInherited()) {
			allResourceButton2 = new ResourceTableOpenButton(page, "allResourceButton", pid(), exPage, tabButton.control);
		} else {
			allResourceButton2 = new TableOpenButton(page, "allResourceButton", pid(), "All Resources", exPage, tabButton.control) {
				private static final long serialVersionUID = 1L;
				@Override
				protected NavigationPublicPageData getPageData(ExtensionResourceAccessInitData appData,
						Class<? extends Resource> type, PageType typeRequested, OgemaHttpRequest req) {
					return appData.systemAccessForPageOpening().getPageByProvider(SPPageUtil.getProviderURL(BaseDataService.RESOURCEALL_NAVI_PROVIDER));//super.getPageData(appData, type, typeRequested);
				}
				@Override
				public void onGET(OgemaHttpRequest req) {
					super.onGET(req);
					ExtensionResourceAccessInitData appData = exPage.getAccessData(req);
					List<Resource> resultAll = getResource(appData, req).getSubResources(false);
					List<Resource> result = new ArrayList<>();
					for(Resource r: resultAll) {
						if(!(r instanceof ValueResource)) result.add(r);
					}
					String text = RESALL_BUTTON_TEXTS.get(req.getLocale());
					if(text == null) text = RESALL_BUTTON_TEXTS.get(OgemaLocale.ENGLISH);
					setText(text+"("+result.size()+")", req);
				}
			};
			allResourceButton2.setDefaultOpenInNewTab(false);
		}
		addTopButtonsExceptFirstTwo(editResource, allResourceButton2, tabButton);
	}*/

	
}	
