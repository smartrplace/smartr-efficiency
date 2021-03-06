package org.smartrplace.smarteff.util.button;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.ogema.core.model.Resource;
import org.ogema.generictype.GenericDataTypeDeclaration;
import org.smartrplace.extensionservice.gui.ExtensionNavigationPageI;
import org.smartrplace.extensionservice.gui.NavigationGUIProvider.PageType;
import org.smartrplace.extensionservice.gui.NavigationPublicPageData;
import org.smartrplace.extensionservice.resourcecreate.ExtensionResourceAccessInitData;
import org.smartrplace.smarteff.defaultservice.BaseDataService;
import org.smartrplace.smarteff.defaultservice.ResourceByTypeTablePage;
import org.smartrplace.smarteff.util.SPPageUtil;
import org.smartrplace.smarteff.util.editgeneric.GenericResourceByTypeTablePageBase.ResourceOfTypeContext;

import de.iwes.widgets.api.widgets.OgemaWidget;
import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.localisation.OgemaLocale;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import extensionmodel.smarteff.api.base.SmartEffUserDataNonEdit;

public abstract class ResourceOfTypeTableOpenButton extends ResourceTableOpenButton {
	private static final long serialVersionUID = 1L;
	public static final Map<OgemaLocale, String> BUTTON_TEXTS = new HashMap<>();
	static {
		BUTTON_TEXTS.put(OgemaLocale.ENGLISH, "Open");
		BUTTON_TEXTS.put(OgemaLocale.GERMAN, "Öffnen");
		BUTTON_TEXTS.put(OgemaLocale.FRENCH, "Ouvrier");
	}
	protected Map<OgemaLocale, String> getTextMap(OgemaHttpRequest req) {
		return BUTTON_TEXTS;
	}
	protected String getEditPageURL() {return null;}
	
	protected boolean openResSub = false;
	public void openResSub(boolean openResSub) {
		this.openResSub = openResSub;
	}
	
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
			Class<? extends Resource> type, PageType typeRequestedNotUsed, OgemaHttpRequest req) {
		String url = null;
		Resource object = getResource(appData, req);
		Class<? extends Resource> entryType = object.getResourceType(); //primaryEntryTypeClass()
		Class<? extends Resource> openType = typeToOpen(appData, req);
		NavigationPublicPageData prov = appData.systemAccessForPageOpening().getMaximumPriorityTablePage(entryType, openType);
		if(prov != null) url = prov.getUrl();
		else {
			//TODO: It is not clear whether the logic in this else statement can ever find a provider/url if the
			//previous method failed, but we keep it for now for sure
			List<NavigationPublicPageData> provs = appData.systemAccessForPageOpening().
					getPages(entryType, true);
			for(NavigationPublicPageData p: provs) {
				if((p.typesListedInTable() != null)) {
					for(GenericDataTypeDeclaration typeListed: p.typesListedInTable()) if(typeListed.representingResourceType().equals(openType)) {
						url = p.getUrl();
						break;					
					}
				}
			}
		}
		if(url == null) {
			if(openResSub)
				url = SPPageUtil.getProviderURL(BaseDataService.RESBYTYPE_ENTRYPOINT_PROVIDER);
				//url = SPPageUtil.getProviderURL(BaseDataService.RESSUBBYTYPE_PROVIDER);
			else
				url = SPPageUtil.getProviderURL(BaseDataService.RESBYTYPE_PROVIDER);
		}
		return appData.systemAccessForPageOpening().getPageByProvider(url);//super.getPageData(appData, type, typeRequested);
	}
	
	@Override
	protected Object getContext(ExtensionResourceAccessInitData appData, Resource object, OgemaHttpRequest req) {
		ResourceOfTypeContext ct = new ResourceOfTypeContext();
		ct.dataTypeName = typeToOpen(appData, req).getName();
		ct.editPageURL = getEditPageURL();
		return ct;
	}
	
	@Override
	public void onGET(OgemaHttpRequest req) {
		//super.onGET(req);
		ExtensionResourceAccessInitData appData = exPage.getAccessData(req);
		Resource destination;
		if(openResSub)
			destination = getResource(appData, req);
		else
			destination = appData.userData();
		if(destination == null) {
			disable(req);
			setText("--", req);
			return;
		}

		int size = getSize(destination, appData, typeToOpen(appData, req));
		String text;
		text = getTextMap(req).get(req.getLocale());
		if(text == null) text = BUTTON_TEXTS.get(OgemaLocale.ENGLISH);						
		setText(text+"("+size+")", req);
		enable(req);
	}

	/** TODO: Searching for all subresources on every GET of the button may be too costly
	 * @param myResource if {@link ResourceByTypeTablePage} is opened this should be the user resource,
	 * otherwise this should be the parent resource
	 */
	public static int getSize(Resource myResource, ExtensionResourceAccessInitData appData, Class<? extends Resource> typeSelected) {
		List<? extends Resource> resultAll = myResource.getSubResources(typeSelected, true);
		return resultAll.size();
	}
	
}
