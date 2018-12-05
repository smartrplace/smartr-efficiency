package org.smartrplace.smarteff.util;

import org.ogema.core.application.ApplicationManager;
import org.ogema.core.model.Resource;
import org.smartrplace.util.directobjectgui.ObjectResourceGUIHelper;

import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.form.button.TemplateInitSingleEmpty;

public abstract class ObjectResourceGUIHelperExtPublic<T extends Resource> extends ObjectResourceGUIHelper<T, T> {

	public ObjectResourceGUIHelperExtPublic(WidgetPage<?> page, TemplateInitSingleEmpty<T> init,
			ApplicationManager appMan, boolean acceptMissingResources) {
		super(page, init, appMan, acceptMissingResources);
	}
	
	@Override
	protected abstract T getResource(T object, OgemaHttpRequest req);
	
	@Override
	public abstract T getGatewayInfo(OgemaHttpRequest req);
}
