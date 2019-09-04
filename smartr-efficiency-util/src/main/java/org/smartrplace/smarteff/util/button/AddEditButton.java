package org.smartrplace.smarteff.util.button;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.ogema.core.model.Resource;
import org.ogema.core.model.ValueResource;
import org.smartrplace.extensionservice.gui.ExtensionNavigationPageI;
import org.smartrplace.extensionservice.gui.NavigationGUIProvider.PageType;
import org.smartrplace.extensionservice.resourcecreate.ExtensionResourceAccessInitData;

import de.iwes.widgets.api.widgets.OgemaWidget;
import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.localisation.OgemaLocale;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import extensionmodel.smarteff.api.base.SmartEffUserDataNonEdit;

public class AddEditButton extends NaviOpenButton { //implements CreateButtonI {
	private static final long serialVersionUID = 1L;

	public static final Map<OgemaLocale, String> BUTTON_TEXTS = new HashMap<>();
	protected Map<OgemaLocale, String> buttonTexts = null;
	public void setButtonTexts(Map<OgemaLocale, String> buttonTexts) {
		this.buttonTexts = buttonTexts;
	}
	protected Map<OgemaLocale, String> getButtonTexts(OgemaHttpRequest req) {
		if(buttonTexts == null) return BUTTON_TEXTS;
		else return buttonTexts;
	}

	static {
		BUTTON_TEXTS.put(OgemaLocale.ENGLISH, "Edit Values");
		BUTTON_TEXTS.put(OgemaLocale.GERMAN, "Werte bearbeiten");
		BUTTON_TEXTS.put(OgemaLocale.FRENCH, "Édite Valeurs");
	}

	//private final Class<? extends Resource> type;

	public AddEditButton(WidgetPage<?> page, String id, String pid,
			//Class<? extends Resource> type,
			ExtensionNavigationPageI<SmartEffUserDataNonEdit, ExtensionResourceAccessInitData> exPage,
			ButtonControlProvider controlProvider) {
		this(page, id, pid, exPage, false, controlProvider);
	}
	public AddEditButton(WidgetPage<?> page, String id, String pid,
			//Class<? extends Resource> type,
			ExtensionNavigationPageI<SmartEffUserDataNonEdit, ExtensionResourceAccessInitData> exPage,
			boolean isBackButton, ButtonControlProvider controlProvider) {
		super(page, id, pid, "", exPage, PageType.EDIT_PAGE, false, isBackButton, controlProvider);
		//this.type = type;
		setDefaultOpenInNewTab(false);
	}
	public AddEditButton(OgemaWidget parent, String id, String pid,
			//Class<? extends Resource> type,
			ExtensionNavigationPageI<SmartEffUserDataNonEdit, ExtensionResourceAccessInitData> exPage,
			ButtonControlProvider controlProvider, OgemaHttpRequest req) {
		super(parent, id, pid, "", exPage, PageType.EDIT_PAGE, false, controlProvider, null, req);
		//this.type = type;
		setDefaultOpenInNewTab(false);
	}

	@Override
	public void onGET(OgemaHttpRequest req) {
		super.onGET(req);
		ExtensionResourceAccessInitData appData = exPage.getAccessData(req);
		String text = getButtonTexts(req).get(req.getLocale());
		if(text == null) text = getButtonTexts(req).get(OgemaLocale.ENGLISH);

		Resource res = getResource(appData, req);
		if(Boolean.getBoolean("org.smartrplace.smarteff.util.button.addSizesOnButtons")
				&& (res != null)) {
			Integer size = getSizeInternal(res, appData);
			if(size != null) {
				setText(text+"("+size+")", req);
				return;
			}
		}
		setText(text, req);
	}
	
	/**Intended to be overwritten. 
	 * 
	 * @param myResource
	 * @param appData
	 * @return if null no size will be shown
	 */
	protected Integer getSizeInternal(Resource myResource, ExtensionResourceAccessInitData appData) {
		return getSize(myResource, appData);
	}

	public static int getSize(Resource myResource, ExtensionResourceAccessInitData appData) {
		List<Resource> resultAll = myResource.getSubResources(false);
		List<Resource> result = new ArrayList<>();
		for(Resource r: resultAll) {
			if((r instanceof ValueResource)) result.add(r);
		}
		return result.size();
	}

	//@Override
	//public Class<? extends Resource> typeToCreate(ExtensionResourceAccessInitData appData, OgemaHttpRequest req) {
	//	return type;
	//}
}
