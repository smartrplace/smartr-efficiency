package org.smartrplace.smarteff.util.button;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.ogema.core.model.Resource;
import org.ogema.core.model.ValueResource;
import org.smartrplace.extenservice.resourcecreate.ExtensionResourceAccessInitData;
import org.smartrplace.extensionservice.gui.ExtensionNavigationPageI;
import org.smartrplace.extensionservice.gui.NavigationGUIProvider.PageType;

import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.localisation.OgemaLocale;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import extensionmodel.smarteff.api.base.SmartEffUserDataNonEdit;

public class AddEditButton extends NaviOpenButton { //implements CreateButtonI {
	private static final long serialVersionUID = 1L;

	public static final Map<OgemaLocale, String> BUTTON_TEXTS = new HashMap<>();

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
		super(page, id, pid, "", exPage, PageType.EDIT_PAGE, false, controlProvider);
		//this.type = type;
		setDefaultOpenInNewTab(false);
	}

	@Override
	public void onGET(OgemaHttpRequest req) {
		super.onGET(req);
		ExtensionResourceAccessInitData appData = exPage.getAccessData(req);
		/*List<Resource> resultAll = getResource(appData, req).getSubResources(false);
		List<Resource> result = new ArrayList<>();
		for(Resource r: resultAll) {
			if((r instanceof ValueResource)) result.add(r);
		}*/
		int size = getSize(getResource(appData, req), appData);
		String text = BUTTON_TEXTS.get(req.getLocale());
		if(text == null) text = BUTTON_TEXTS.get(OgemaLocale.ENGLISH);
		setText(text+"("+size+")", req);
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
