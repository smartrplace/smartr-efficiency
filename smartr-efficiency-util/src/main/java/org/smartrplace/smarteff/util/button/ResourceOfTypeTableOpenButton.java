package org.smartrplace.smarteff.util.button;

import java.util.List;

import org.ogema.core.model.Resource;
import org.smartrplace.extenservice.resourcecreate.ExtensionResourceAccessInitData;
import org.smartrplace.extensionservice.gui.ExtensionNavigationPageI;
import org.smartrplace.extensionservice.gui.NavigationGUIProvider.PageType;
import org.smartrplace.extensionservice.gui.NavigationPublicPageData;
import org.smartrplace.smarteff.defaultservice.BaseDataService;
import org.smartrplace.smarteff.util.SPPageUtil;

import de.iwes.widgets.api.widgets.OgemaWidget;
import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import extensionmodel.smarteff.api.base.SmartEffUserDataNonEdit;

public abstract class ResourceOfTypeTableOpenButton extends ResourceTableOpenButton {
	private static final long serialVersionUID = 1L;

	protected abstract Class<? extends Resource> typeToOpen(ExtensionResourceAccessInitData appData, OgemaHttpRequest req);
	
	public ResourceOfTypeTableOpenButton(WidgetPage<?> page, String id, String pid,
			ExtensionNavigationPageI<SmartEffUserDataNonEdit, ExtensionResourceAccessInitData> exPage,
			ButtonControlProvider controlProvider) {
		super(page, id, pid, exPage, controlProvider, false);
	}

	public ResourceOfTypeTableOpenButton(OgemaWidget parent, String id, String pid,
			//Class<? extends Resource> type,
			ExtensionNavigationPageI<SmartEffUserDataNonEdit, ExtensionResourceAccessInitData> exPage,
			ButtonControlProvider controlProvider, OgemaHttpRequest req) {
		super(parent, id, pid, exPage, req, controlProvider, false);
	}

	@Override
	protected NavigationPublicPageData getPageData(ExtensionResourceAccessInitData appData,
			Class<? extends Resource> type, PageType typeRequested, OgemaHttpRequest req) {
		Resource object = getResource(appData, req);
		Class<? extends Resource> entryType = object.getResourceType(); //primaryEntryTypeClass()
		List<NavigationPublicPageData> provs = appData.systemAccessForPageOpening().
				getPages(entryType, true);
		String url = null;
		Class<? extends Resource> openType = typeToOpen(appData, req);
		for(NavigationPublicPageData p: provs) {
			if((p.typesListedInTable() != null) &&
					p.typesListedInTable().contains(openType)) {
				url = p.getUrl();
				break;
			}
		}
		if(url == null) url = SPPageUtil.getProviderURL(BaseDataService.RESBYTYPE_PROVIDER);
		return appData.systemAccessForPageOpening().getPageByProvider(url);//super.getPageData(appData, type, typeRequested);
	}
	
	@Override
	protected Object getContext(ExtensionResourceAccessInitData appData, Resource object) {
		return object.getResourceType().getName();
	}
	
	@Override
	public void onGET(OgemaHttpRequest req) {
		super.onGET(req);
		ExtensionResourceAccessInitData appData = exPage.getAccessData(req);
		Resource destination = getResource(appData, req);
		if(destination == null) {
			disable(req);
			setText("--", req);
			return;
		}

		int size = getSize(destination, appData, typeToOpen(appData, req));
		String text = "Open";
		setText(text+"("+size+")", req);
	}

	//TODO: Searching for all subresources on every GET of the button may be too costly
	public static int getSize(Resource myResource, ExtensionResourceAccessInitData appData, Class<? extends Resource> typeSelected) {
		List<? extends Resource> resultAll = myResource.getSubResources(typeSelected, true);
		return resultAll.size();
	}
}
