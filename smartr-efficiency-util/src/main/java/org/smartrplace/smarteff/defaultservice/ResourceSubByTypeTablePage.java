package org.smartrplace.smarteff.defaultservice;

import java.util.ArrayList;
import java.util.List;

import org.ogema.core.model.Resource;
import org.ogema.tools.resource.util.ResourceUtils;
import org.smartrplace.extensionservice.ExtensionCapabilityPublicData.EntryType;
import org.smartrplace.extensionservice.resourcecreate.ExtensionResourceAccessInitData;
import org.smartrplace.smarteff.util.CapabilityHelper;
import org.smartrplace.smarteff.util.button.AddEntryButton;
import org.smartrplace.smarteff.util.button.RegisterAsUserButton;
import org.smartrplace.smarteff.util.button.TabButton;

import de.iwes.widgets.api.widgets.html.StaticTable;
import de.iwes.widgets.api.widgets.localisation.OgemaLocale;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.form.button.Button;
import de.iwes.widgets.html.form.button.RedirectButton;

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
		return "Resources of type "+typeSelected(req).getSimpleName()+" below "+ResourceUtils.getHumanReadableName(getReqData(req));
	}
	
	@Override
	protected List<EntryType> getEntryTypes() {
		return CapabilityHelper.getStandardEntryTypeList(primaryEntryTypeClass());
	}
	
	//From Resource TablePage
	protected void addWidgetsAboveTable(Class<? extends Resource> resourceType) {
		tabButton = new TabButton(page, "tabButton", pid());
		
		RedirectButton addEntry = new AddEntryButton(page, "addEntry", pid(), "Add Element",
				null, exPage, tabButton.control) {
			private static final long serialVersionUID = 1L;
			@Override
			public Class<? extends Resource> typeToCreate(ExtensionResourceAccessInitData appData,
					OgemaHttpRequest req) {
				return typeSelected(req);
			}
			@Override
			protected Resource getResource(ExtensionResourceAccessInitData appData, OgemaHttpRequest req) {
				return appData.userData();
			}
		};

		Button registerUser = new RegisterAsUserButton(page, "registerUser", pid(), exPage, tabButton.control);

		StaticTable topTable = new StaticTable(1, 3, new int[]{8,2, 2});
		topTable.setContent(0, 0, addEntry).setContent(0, 1, registerUser).setContent(0, 2, tabButton);
		page.append(topTable);

		/*RedirectButton editResource = new AddEditButton(page, "editEntry", pid(), exPage,
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
		addTopButtonsExceptFirstTwo(editResource, allResourceButton2, tabButton);*/
	}

	
}	
