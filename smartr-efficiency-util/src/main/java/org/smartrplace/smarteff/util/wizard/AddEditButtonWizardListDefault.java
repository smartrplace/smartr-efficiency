package org.smartrplace.smarteff.util.wizard;

import java.util.List;
import java.util.Map;

import org.ogema.core.model.Resource;
import org.smartrplace.extensionservice.gui.ExtensionNavigationPageI;
import org.smartrplace.extensionservice.resourcecreate.ExtensionResourceAccessInitData;
import org.smartrplace.smarteff.util.button.ButtonControlProvider;
import org.smartrplace.smarteff.util.editgeneric.DefaultWidgetProvider.SmartEffTimeSeriesWidgetContext;
import org.smartrplace.util.directobjectgui.ApplicationManagerMinimal;

import de.iwes.widgets.api.widgets.OgemaWidget;
import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.localisation.OgemaLocale;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import extensionmodel.smarteff.api.base.SmartEffUserDataNonEdit;

@Deprecated
public class AddEditButtonWizardListDefault<T extends Resource> extends AddEditButtonWizardList<T>{
	protected final String sub;
	
	public AddEditButtonWizardListDefault(WidgetPage<?> page, String id, String pid,
			ExtensionNavigationPageI<SmartEffUserDataNonEdit, ExtensionResourceAccessInitData> exPage,
			ButtonControlProvider controlProvider, BACKTYPE isBackButton, ApplicationManagerMinimal appManExt,
			String sub) {
		super(page, id, pid, exPage, controlProvider, isBackButton, appManExt);
		this.sub = sub;
	}

	public AddEditButtonWizardListDefault(OgemaWidget widget, String id, String pid,
			ExtensionNavigationPageI<SmartEffUserDataNonEdit, ExtensionResourceAccessInitData> exPage,
			ButtonControlProvider controlProvider, BACKTYPE isBackButton, ApplicationManagerMinimal appManExt,
			OgemaHttpRequest req, String sub) {
		super(widget, id, pid, exPage, controlProvider, isBackButton, appManExt, req);
		this.sub = sub;
	}

	private static final long serialVersionUID = 1L;

	@Override
	protected Resource getEntryResource(OgemaHttpRequest req) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected Class<T> getType() {
		// TODO Auto-generated method stub
		return null;
	}
}