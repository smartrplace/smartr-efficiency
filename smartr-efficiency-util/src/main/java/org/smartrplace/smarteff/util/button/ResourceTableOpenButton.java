package org.smartrplace.smarteff.util.button;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.ogema.core.model.Resource;
import org.ogema.core.model.ResourceList;
import org.ogema.core.model.ValueResource;
import org.smartrplace.extenservice.resourcecreate.ExtensionResourceAccessInitData;
import org.smartrplace.extensionservice.gui.ExtensionNavigationPageI;

import de.iwes.widgets.api.widgets.OgemaWidget;
import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.localisation.OgemaLocale;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import extensionmodel.smarteff.api.base.SmartEffUserData;
import extensionmodel.smarteff.api.base.SmartEffUserDataNonEdit;

public class ResourceTableOpenButton extends TableOpenButton {
	private static final long serialVersionUID = 1L;
	
	public static final Map<OgemaLocale, String> BUTTON_TEXTS = new HashMap<>();
	public static final Map<OgemaLocale, String> UPBUTTON_TEXTS = new HashMap<>();
	static {
		BUTTON_TEXTS.put(OgemaLocale.ENGLISH, "Data Explorer");
		BUTTON_TEXTS.put(OgemaLocale.GERMAN, "Data Explorer");
		BUTTON_TEXTS.put(OgemaLocale.FRENCH, "Data Explorer");
		UPBUTTON_TEXTS.put(OgemaLocale.ENGLISH, "Up");
		UPBUTTON_TEXTS.put(OgemaLocale.GERMAN, "Ebene hoch");
		UPBUTTON_TEXTS.put(OgemaLocale.FRENCH, "Step Up");
	}
	private final boolean isUp;

	public ResourceTableOpenButton(WidgetPage<?> page, String id, String pid,
			//Class<? extends Resource> type,
			ExtensionNavigationPageI<SmartEffUserDataNonEdit, ExtensionResourceAccessInitData> exPage,
			ButtonControlProvider controlProvider) {
		this(page, id, pid, exPage, controlProvider, false);
	}
	public ResourceTableOpenButton(WidgetPage<?> page, String id, String pid,
			//Class<? extends Resource> type,
			ExtensionNavigationPageI<SmartEffUserDataNonEdit, ExtensionResourceAccessInitData> exPage,
			ButtonControlProvider controlProvider, boolean isUp) {
		super(page, id, pid, "", exPage, controlProvider);
		this.isUp = isUp;
		setDefaultOpenInNewTab(false);
	}

	public ResourceTableOpenButton(OgemaWidget parent, String id, String pid,
			//Class<? extends Resource> type,
			ExtensionNavigationPageI<SmartEffUserDataNonEdit, ExtensionResourceAccessInitData> exPage,
			ButtonControlProvider controlProvider, OgemaHttpRequest req) {
		this(parent, id, pid, exPage, req, controlProvider, false);
	}
	public ResourceTableOpenButton(OgemaWidget parent, String id, String pid,
			//Class<? extends Resource> type,
			ExtensionNavigationPageI<SmartEffUserDataNonEdit, ExtensionResourceAccessInitData> exPage,
			OgemaHttpRequest req, ButtonControlProvider controlProvider, boolean isUp) {
		super(parent, id, pid, "", exPage, controlProvider, req);
		this.isUp = isUp;
		setDefaultOpenInNewTab(false);
	}

	@Override
	protected Resource getResource(ExtensionResourceAccessInitData appData, OgemaHttpRequest req) {
		if(isUp) {
			Resource parent = super.getResource(appData, req).getParent();
			if(parent == null)  {
				return null;
			}
			if(parent instanceof ResourceList) {
				parent = parent.getParent();
			}
			if(parent.getResourceType().equals(SmartEffUserData.class)) {
				return null;
			}
			return parent;
		}
		return super.getResource(appData, req);
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

		int size = getSize(destination, appData);
		String text;
		if(isUp) {
			text = UPBUTTON_TEXTS.get(req.getLocale());
			if(text == null) text = UPBUTTON_TEXTS.get(OgemaLocale.ENGLISH);			
		} else {
			text = BUTTON_TEXTS.get(req.getLocale());
			if(text == null) text = BUTTON_TEXTS.get(OgemaLocale.ENGLISH);						
		}
		setText(text+"("+size+")", req);
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
