package org.smartrplace.extensionservice.gui;

import org.smartrplace.extensionservice.ExtensionUserDataNonEdit;
import org.smartrplace.extensionservice.resourcecreate.ExtensionResourceAccessInitData;

import de.iwes.widgets.api.widgets.OgemaWidget;
import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.html.StaticTable;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
/**
 * Framework access for navigation pages
 */
public interface ExtensionNavigationPageI<T extends ExtensionUserDataNonEdit, C extends ExtensionResourceAccessInitData> {
	
	public void finalize(StaticTable table);
	
	public WidgetPage<?> getPage();
	
	public static interface InitListener {
		void onInitComplete(OgemaHttpRequest req);
	}
	public void registerInitExtension(InitListener initListener);
	
	public void registerDependentWidgetOnInit(OgemaWidget widget);

	public void registerAppTableWidgetsDependentOnInit(StaticTable table);
	
	public ExtensionResourceAccessInitData getAccessData(OgemaHttpRequest req);
	
	public WidgetProvider getSpecialWidgetManagement();
	
	public static void registerDependentWidgets(OgemaWidget governor, StaticTable table) {
		for(OgemaWidget el: table.getSubWidgets()) {
			governor.triggerOnPOST(el);
		}
	}
}
