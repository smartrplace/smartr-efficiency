package org.smartrplace.gui.filtering;

import org.ogema.core.application.ApplicationManager;
import org.ogema.core.model.Resource;
import org.smartrplace.util.directobjectgui.ObjectGUITablePage;
import org.smartrplace.util.directobjectgui.ObjectResourceGUIHelper;
import org.smartrplace.util.format.WidgetHelper;

import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.localisation.OgemaLocale;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.complextable.RowTemplate.Row;
import de.iwes.widgets.html.form.label.Header;

public abstract class ObjectGUITablePageNamed <T, R extends Resource> extends ObjectGUITablePage<T, R> {
	public ObjectGUITablePageNamed(WidgetPage<?> page, ApplicationManager appMan, T initSampleObject) {
		super(page, appMan, initSampleObject, false);
		//You have to call triggerPageBuild yourself
	}
	protected abstract String getTypeName(OgemaLocale locale);
	protected String getHeader(OgemaLocale locale) {
		return "View and Configuration for "+getTypeName(locale);
	}
	protected abstract String getLabel(T obj);

	@Override
	public R getResource(T object, OgemaHttpRequest req) {
		return null;
	}

	protected void addNameLabel(T object, ObjectResourceGUIHelper<T, R> vh, String id, Row row) {
		vh.stringLabel(getTypeName(null), id, getLabel(object), row);
	};

	@Override
	public void addWidgetsAboveTable() {
		Header header = new Header(page, WidgetHelper.getValidWidgetId("headerStdPermPage"+this.getClass().getSimpleName())) {
			private static final long serialVersionUID = 1L;

			@Override
			public void onGET(OgemaHttpRequest req) {
				setText(getHeader(req.getLocale()), req);
			}
		};
		page.append(header);
	}
}
