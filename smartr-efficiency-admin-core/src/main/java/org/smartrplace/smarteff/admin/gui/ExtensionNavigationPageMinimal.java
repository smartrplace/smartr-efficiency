package org.smartrplace.smarteff.admin.gui;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.smartrplace.extenservice.resourcecreate.ExtensionResourceAccessInitData;
import org.smartrplace.extensionservice.gui.ExtensionNavigationPageI;
import org.smartrplace.smarteff.admin.StandardPageAdmin;
import org.smartrplace.smarteff.admin.UserAdmin;

import de.iwes.widgets.api.widgets.OgemaWidget;
import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.html.StaticTable;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.form.button.TemplateRedirectButton;
import extensionmodel.smarteff.api.base.SmartEffUserDataNonEdit;

public class ExtensionNavigationPageMinimal 
implements ExtensionNavigationPageI<SmartEffUserDataNonEdit, ExtensionResourceAccessInitData> {
	private final WidgetPage<?> page;
	private final UserAdmin userAdmin;
	//private final String userName;
	private final List<InitListener> initListeners = new ArrayList<>();

	@Override
	public void finalize(StaticTable table) {
	}

	@Override
	public WidgetPage<?> getPage() {
		return page;
	}

	@Override
	public void registerDependentWidgetOnInit(OgemaWidget widget) {}

	
	@Override
	public void registerInitExtension(InitListener initListener) {
		initListeners.add(initListener);
	}

	@Override
	public void registerAppTableWidgetsDependentOnInit(StaticTable table) {}

	@Override
	public ExtensionResourceAccessInitData getAccessData(OgemaHttpRequest req) {
		//SmartEffUserDataNonEdit userData = appM.getUserAdmin().getUserData(userName);
		String configId = getConfigId(req);
		return userAdmin.getAccessData(configId, req, null, StandardPageAdmin.NAVI_OVERVIEW_URL);
		//return appM.getUserAdmin().getAccessData(null, req, NaviOverviewPageMulti.this, userData, appM);
	}
	
	private String getConfigId(OgemaHttpRequest req) {
		Map<String,String[]> params = getPage().getPageParameters(req);
		if (params == null || params.isEmpty())
			return null;
		String[] patterns = params.get(TemplateRedirectButton.PAGE_CONFIG_PARAMETER);
		if (patterns == null || patterns.length == 0)
			return null;
		return patterns[0];	
	}

	public ExtensionNavigationPageMinimal(WidgetPage<?> page, UserAdmin userAdmin) { //, String userName) {
		this.page = page;
		this.userAdmin = userAdmin;
		//this.userName = userName;
	}
}