package org.smartrplace.smarteff.util.button;

import org.ogema.core.model.Resource;
import org.smartrplace.extenservice.resourcecreate.ExtensionResourceAccessInitData;
import org.smartrplace.extensionservice.gui.ExtensionNavigationPageI;
import org.smartrplace.extensionservice.gui.NavigationGUIProvider.PageType;

import de.iwes.widgets.api.widgets.OgemaWidget;
import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import extensionmodel.smarteff.api.base.SmartEffUserDataNonEdit;

public class AddEntryButton extends NaviOpenButton implements CreateButtonI {
	private static final long serialVersionUID = 1L;

	private final Class<? extends Resource> type;
	
	public AddEntryButton(WidgetPage<?> page, String id, String pid, String text,
			Class<? extends Resource> type,
			ExtensionNavigationPageI<SmartEffUserDataNonEdit, ExtensionResourceAccessInitData> exPage,
			ButtonControlProvider tabButton) {
		super(page, id, pid, text, exPage, PageType.EDIT_PAGE, true, tabButton);
		this.type = type;
	}
	
	public AddEntryButton(OgemaWidget parent, String id, String pid, String text,
			Class<? extends Resource> type,
			ExtensionNavigationPageI<SmartEffUserDataNonEdit, ExtensionResourceAccessInitData> exPage,
			ButtonControlProvider tabButton, OgemaHttpRequest req) {
		super(parent, id, pid, text, exPage, PageType.EDIT_PAGE, true, tabButton, req);
		this.type = type;
	}

	@Override
	public Class<? extends Resource> typeToCreate(ExtensionResourceAccessInitData appData, OgemaHttpRequest req) {
		return type;
	}
}
