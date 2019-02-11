package org.smartrplace.smarteff.util.button;

import org.ogema.core.model.Resource;
import org.smartrplace.extensionservice.gui.ExtensionNavigationPageI;
import org.smartrplace.extensionservice.resourcecreate.ExtensionResourceAccessInitData;

import de.iwes.widgets.api.widgets.OgemaWidget;
import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import extensionmodel.smarteff.api.base.SmartEffUserDataNonEdit;

public class AddEditButtonForCreate extends AddEditButton implements CreateButtonI {
	private static final long serialVersionUID = 1L;

	/**Overwrite this if type shall be dynamic. Then set type=null in constructor*/
	@Override
	public Class<? extends Resource> typeToCreate(ExtensionResourceAccessInitData appData, OgemaHttpRequest req) {
		return type;
	}

	private final Class<? extends Resource> type;
	
	public AddEditButtonForCreate(WidgetPage<?> page, String id, String pid,
			Class<? extends Resource> type,
			ExtensionNavigationPageI<SmartEffUserDataNonEdit, ExtensionResourceAccessInitData> exPage,
			ButtonControlProvider tabButton) {
		super(page, id, pid, exPage, tabButton);
		this.type = type;
	}
	
	public AddEditButtonForCreate(OgemaWidget parent, String id, String pid,
			Class<? extends Resource> type,
			ExtensionNavigationPageI<SmartEffUserDataNonEdit, ExtensionResourceAccessInitData> exPage,
			ButtonControlProvider tabButton, OgemaHttpRequest req) {
		super(parent, id, pid, exPage, tabButton, req);
		this.type = type;
	}
}
