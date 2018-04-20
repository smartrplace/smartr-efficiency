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
import org.smartrplace.extensionservice.gui.NavigationPublicPageData;

import de.iwes.widgets.api.widgets.OgemaWidget;
import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.localisation.OgemaLocale;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import extensionmodel.smarteff.api.base.SmartEffUserDataNonEdit;

public class BackButton extends TableOpenButton {
	private static final long serialVersionUID = 1L;
	
	public static final Map<OgemaLocale, String> BUTTON_TEXTS = new HashMap<>();
	public static final Map<OgemaLocale, String> UPBUTTON_TEXTS = new HashMap<>();
	static {
		BUTTON_TEXTS.put(OgemaLocale.ENGLISH, "Back");
		BUTTON_TEXTS.put(OgemaLocale.GERMAN, "Zurück");
		BUTTON_TEXTS.put(OgemaLocale.FRENCH, "Retournez");
	}

	public BackButton(WidgetPage<?> page, String id, String pid,
			//Class<? extends Resource> type,
			ExtensionNavigationPageI<SmartEffUserDataNonEdit, ExtensionResourceAccessInitData> exPage,
			ButtonControlProvider controlProvider) {
		super(page, id, pid, "", exPage, controlProvider);
		setDefaultOpenInNewTab(false);
	}

	public BackButton(OgemaWidget parent, String id, String pid,
			//Class<? extends Resource> type,
			ExtensionNavigationPageI<SmartEffUserDataNonEdit, ExtensionResourceAccessInitData> exPage,
			ButtonControlProvider controlProvider, OgemaHttpRequest req) {
		super(parent, id, pid, "", exPage, controlProvider, req);
		setDefaultOpenInNewTab(false);
	}


	@Override
	protected NavigationPublicPageData getPageData(ExtensionResourceAccessInitData appData,
			Class<? extends Resource> type, PageType typeRequested, OgemaHttpRequest req) {
		if(appData.getConfigInfo() == null) return null;
		return appData.getConfigInfo().lastPage;
	}
	@Override
	protected Resource getResource(ExtensionResourceAccessInitData appData, OgemaHttpRequest req) {
		if(appData.getConfigInfo() == null) return null;
		return appData.getConfigInfo().lastPrimaryResource;
	}

	
	@Override
	public void onGET(OgemaHttpRequest req) {
		super.onGET(req);
		ExtensionResourceAccessInitData appData = exPage.getAccessData(req);
		Resource parent = getResource(appData, req);
		String text;
		text = BUTTON_TEXTS.get(req.getLocale());
		if(text == null) text = BUTTON_TEXTS.get(OgemaLocale.ENGLISH);						
		if(parent == null) {
			setText(text, req);
		} else {
			int size = getSize(parent, appData);
			setText(text+"("+size+")", req);			
		}
	}	
	public static int getSize(Resource myResource, ExtensionResourceAccessInitData appData) {
		List<Resource> resultAll = myResource.getSubResources(false);
		List<Resource> result = new ArrayList<>();
		for(Resource r: resultAll) {
			if(!(r instanceof ValueResource)) result.add(r);
		}
		return result.size();
	}
}
