package org.smartrplace.smarteff.util.button;

import org.smartrplace.extenservice.resourcecreate.ExtensionResourceAccessInitData;
import org.smartrplace.extensionservice.gui.ExtensionNavigationPageI;
import org.smartrplace.extensionservice.gui.NavigationGUIProvider.PageType;

import de.iwes.widgets.api.widgets.OgemaWidget;
import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import extensionmodel.smarteff.api.base.SmartEffUserDataNonEdit;

public class TableOpenButton extends NaviOpenButton{
	private static final long serialVersionUID = 1L;
	
	public TableOpenButton(WidgetPage<?> page, String id, String pid, String text,
			//Class<? extends Resource> type,
			ExtensionNavigationPageI<SmartEffUserDataNonEdit, ExtensionResourceAccessInitData> exPage,
			ButtonControlProvider controlProvider) {
		super(page, id, pid, text, exPage, PageType.TABLE_PAGE, false, controlProvider);
	}

	public TableOpenButton(OgemaWidget parent, String id, String pid, String text,
			//Class<? extends Resource> type,
			ExtensionNavigationPageI<SmartEffUserDataNonEdit, ExtensionResourceAccessInitData> exPage,
			ButtonControlProvider controlProvider, OgemaHttpRequest req) {
		super(parent, id, pid, text, exPage, PageType.TABLE_PAGE, false, controlProvider, req);
	}
}
