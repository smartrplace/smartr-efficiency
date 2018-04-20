package org.smartrplace.smarteff.util.button;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.ogema.core.model.Resource;
import org.smartrplace.extenservice.proposal.ProposalPublicData;
import org.smartrplace.extenservice.resourcecreate.ExtensionResourceAccessInitData;
import org.smartrplace.extensionservice.gui.ExtensionNavigationPageI;
import org.smartrplace.extensionservice.gui.NavigationPublicPageData;
import org.smartrplace.extensionservice.gui.NavigationGUIProvider.PageType;
import org.smartrplace.smarteff.defaultservice.BaseDataService;
import org.smartrplace.smarteff.util.SPPageUtil;

import de.iwes.widgets.api.widgets.OgemaWidget;
import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.localisation.OgemaLocale;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import extensionmodel.smarteff.api.base.SmartEffUserDataNonEdit;

public class ProposalProvTableOpenButton extends TableOpenButton {
	private static final long serialVersionUID = 1L;
	
	public static final Map<OgemaLocale, String> BUTTON_TEXTS = new HashMap<>();
	static {
		BUTTON_TEXTS.put(OgemaLocale.ENGLISH, "Calculators");
		BUTTON_TEXTS.put(OgemaLocale.GERMAN, "Rechner");
		BUTTON_TEXTS.put(OgemaLocale.FRENCH, "Calculateurs");
	}

	public ProposalProvTableOpenButton(WidgetPage<?> page, String id, String pid,
			//Class<? extends Resource> type,
			ExtensionNavigationPageI<SmartEffUserDataNonEdit, ExtensionResourceAccessInitData> exPage,
			ButtonControlProvider controlProvider) {
		super(page, id, pid, "", exPage, controlProvider);
		setDefaultOpenInNewTab(false);
	}

	public ProposalProvTableOpenButton(OgemaWidget parent, String id, String pid,
			//Class<? extends Resource> type,
			ExtensionNavigationPageI<SmartEffUserDataNonEdit, ExtensionResourceAccessInitData> exPage,
			ButtonControlProvider controlProvider, OgemaHttpRequest req) {
		super(parent, id, pid, "", exPage, controlProvider, req);
		setDefaultOpenInNewTab(false);
	}

	@Override
	protected NavigationPublicPageData getPageData(ExtensionResourceAccessInitData appData,
			Class<? extends Resource> type, PageType typeRequested, OgemaHttpRequest req) {
		return appData.systemAccessForPageOpening().getPageByProvider(SPPageUtil.getProviderURL(BaseDataService.PROPOSALTABLE_PROVIDER));//super.getPageData(appData, type, typeRequested);
	}
	
	@Override
	public void onGET(OgemaHttpRequest req) {
		super.onGET(req);
		ExtensionResourceAccessInitData appData = exPage.getAccessData(req);
		/*Class<? extends Resource> type = getResource(appData, req).getResourceType();
		List<ProposalPublicData> provs = appData.systemAccessForPageOpening().getProposalProviders(type);*/
		Resource myRes = getResource(appData, req);
		String text = BUTTON_TEXTS.get(req.getLocale());
		if(text == null) text = BUTTON_TEXTS.get(OgemaLocale.ENGLISH);
		if(myRes == null) {
			setText(text, req);
			return;
		}
		int size = getSize(myRes, appData);
		setText(text+"("+size+")", req);
	}
	
	public static int getSize(Resource myResource, ExtensionResourceAccessInitData appData) {
		Class<? extends Resource> type = myResource.getResourceType();
		List<ProposalPublicData> provs = appData.systemAccessForPageOpening().getProposalProviders(type);
		return provs.size();
	}
}
