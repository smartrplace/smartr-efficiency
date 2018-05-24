package org.smartrplace.smarteff.util.button;

import java.util.HashMap;
import java.util.Map;

import org.smartrplace.extensionservice.gui.ExtensionNavigationPageI;
import org.smartrplace.extensionservice.gui.NavigationPublicPageData;
import org.smartrplace.extensionservice.resourcecreate.ExtensionResourceAccessInitData;
import org.smartrplace.smarteff.util.CapabilityHelper;

import de.iwes.widgets.api.widgets.OgemaWidget;
import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.localisation.OgemaLocale;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import extensionmodel.smarteff.api.base.SmartEffUserDataNonEdit;

public class RegisterAsUserButton extends TableOpenButton {
	private static final long serialVersionUID = 1L;
	
	public static final Map<OgemaLocale, String> BUTTON_TEXTS = new HashMap<>();
	public static final Map<OgemaLocale, String> UPBUTTON_TEXTS = new HashMap<>();
	static {
		BUTTON_TEXTS.put(OgemaLocale.ENGLISH, "Register");
		BUTTON_TEXTS.put(OgemaLocale.GERMAN, "Registrieren");
		BUTTON_TEXTS.put(OgemaLocale.FRENCH, "Enregistrez");
	}

	public RegisterAsUserButton(WidgetPage<?> page, String id, String pid,
			//Class<? extends Resource> type,
			ExtensionNavigationPageI<SmartEffUserDataNonEdit, ExtensionResourceAccessInitData> exPage,
			ButtonControlProvider controlProvider) {
		super(page, id, pid, "", exPage, controlProvider);
		setDefaultOpenInNewTab(false);
	}

	public RegisterAsUserButton(OgemaWidget parent, String id, String pid,
			//Class<? extends Resource> type,
			ExtensionNavigationPageI<SmartEffUserDataNonEdit, ExtensionResourceAccessInitData> exPage,
			ButtonControlProvider controlProvider, OgemaHttpRequest req) {
		super(parent, id, pid, "", exPage, controlProvider, req);
		setDefaultOpenInNewTab(false);
	}


	/*@Override
	protected NavigationPublicPageData getPageData(ExtensionResourceAccessInitData appData,
			Class<? extends Resource> type, PageType typeRequested, OgemaHttpRequest req) {
		if(appData.getConfigInfo() == null) return null;
		return appData.getConfigInfo().lastPage;
	}
	@Override
	protected Resource getResource(ExtensionResourceAccessInitData appData, OgemaHttpRequest req) {
		return null;
	}*/

	
	@Override
	public void onGET(OgemaHttpRequest req) {
		ExtensionResourceAccessInitData appData = exPage.getAccessData(req);
		boolean isAnonymous = appData.getUserInfo().isAnonymousUser();
		if(isAnonymous) {
			String text;
			text = BUTTON_TEXTS.get(req.getLocale());
			if(text == null) text = BUTTON_TEXTS.get(OgemaLocale.ENGLISH);						
			setText(text, req);
			setWidgetVisibility(true, req);
		} else setWidgetVisibility(false, req);
	}

	@Override
	public void onPrePOST(String data, OgemaHttpRequest req) {
		if(controlProvider != null) {
			setOpenInNewTab(controlProvider.openInNewTab(req), req);
		}
		
		ExtensionResourceAccessInitData appData = exPage.getAccessData(req);
		NavigationPublicPageData pageData = appData.systemAccess().getPageByProvider("org_smartrplace_smarteff_multiadmin_gui_RegisterAsUserPage.html");
		final String configId = getConfigId(pageType, null, appData.systemAccessForPageOpening(),
				pageData, doCreate, null , null); //type(appData, req)
		if(configId.startsWith(CapabilityHelper.ERROR_START)) setUrl("error/"+configId, req);
		else setUrl(pageData.getUrl()+"?configId="+configId, req);
		
	}
}
