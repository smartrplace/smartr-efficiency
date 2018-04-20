package org.smartrplace.smarteff.admin.gui;

import java.util.Collection;

import org.ogema.core.application.ApplicationManager;
import org.ogema.core.model.Resource;
import org.ogema.core.model.simple.TimeResource;
import org.smartrplace.extenservice.resourcecreate.ExtensionResourceAccessInitData;
import org.smartrplace.extensionservice.gui.ExtensionNavigationPageI;
import org.smartrplace.extensionservice.gui.NavigationGUIProvider.PageType;
import org.smartrplace.extensionservice.gui.NavigationPublicPageData;
import org.smartrplace.smarteff.admin.SpEffAdminController;
import org.smartrplace.smarteff.admin.UserAdmin;
import org.smartrplace.smarteff.admin.object.SmartrEffExtResourceTypeData;
import org.smartrplace.smarteff.defaultservice.BaseDataService;
import org.smartrplace.smarteff.util.SPPageUtil;
import org.smartrplace.util.directobjectgui.ObjectGUITablePage;
import org.smartrplace.util.directobjectgui.ObjectResourceGUIHelper;
import org.smartrplace.util.format.WidgetHelper;

import de.iwes.widgets.api.extended.WidgetData;
import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.complextable.RowTemplate.Row;
import de.iwes.widgets.html.form.label.Header;
import de.iwes.widgets.resource.widget.textfield.ValueResourceTextField;
import extensionmodel.smarteff.api.base.SmartEffUserDataNonEdit;

/**
 * An HTML page, generated from the Java code.
 */
public class ResTypePage extends ObjectGUITablePage<SmartrEffExtResourceTypeData, Resource> {
	public static final float MIN_COMFORT_TEMP = 4;
	public static final float MAX_COMFORT_TEMP = 30;
	public static final float DEFAULT_COMFORT_TEMP = 21;
	
	private final SpEffAdminController app;
	
	ValueResourceTextField<TimeResource> updateInterval;

	public ResTypePage(final WidgetPage<?> page, final SpEffAdminController app,
			SmartrEffExtResourceTypeData initData) {
		super(page, app.appMan, initData);
		this.app = app;
	}
	
	@Override
	public void addWidgetsAboveTable() {
		Header header = new Header(page, "header", "Data Type Overview");
		header.addDefaultStyle(WidgetData.TEXT_ALIGNMENT_LEFT);
		page.append(header);
	}
	
	@Override
	public Collection<SmartrEffExtResourceTypeData> getObjectsInTable(OgemaHttpRequest req) {
		Collection<SmartrEffExtResourceTypeData> providers = app.typeAdmin.resourceTypes.values(); 
		return providers;
	}

	@Override
	public void addWidgets(SmartrEffExtResourceTypeData object, ObjectResourceGUIHelper<SmartrEffExtResourceTypeData, Resource> vh,
			String id, OgemaHttpRequest req, Row row, ApplicationManager appMan) {
		if(req != null) {
			vh.stringLabel("Name", id, object.typeDeclaration.label(req.getLocale()), row);
		} else vh.registerHeaderEntry("Name");
		vh.stringLabel("Resource Type", id, object.resType.getName(), row);
		if(req != null) {
			vh.stringLabel("Public", id, ""+app.appManExt.globalData().getSubResources(object.resType, true).size(), row);
			int editNum = getEditNum(object.resType);
			int nonEditNum = getNonEditNum(object.resType);
			vh.stringLabel("ReadOnly", id, ""+(nonEditNum-editNum), row);
			vh.stringLabel("ReadWrite", id, ""+editNum, row);
			ExtensionNavigationPageI<SmartEffUserDataNonEdit, ExtensionResourceAccessInitData> exPage =
					new ExtensionNavigationPageMinimal(page, getUserAdmin());
					//appM.getUserAdmin().getNaviPage(page, "navioverview/url", "dataExplorer.html", myId, null);
			ExtensionResourceAccessInitData appData = exPage.getAccessData(req);
			
			NavigationPublicPageData pageData = appData.systemAccessForPageOpening().getPageByProvider(SPPageUtil.getProviderURL(BaseDataService.RESBYTYPE_PROVIDER));
			String context = object.resType.getName();
			SPPageUtil.addOpenButton("Data Explorer", null, vh, id, row, pageData ,
					appData.systemAccessForPageOpening(), "Resources", "No ResPage", true, PageType.TABLE_PAGE,
					null, context, req);
		} else {
			vh.registerHeaderEntry("Public");
			vh.registerHeaderEntry("ReadOnly");
			vh.registerHeaderEntry("ReadWrite");
			vh.registerHeaderEntry("Data Explorer");
		}
//if(configRes != null) try {Thread.sleep(1000);} catch (InterruptedException e) {e.printStackTrace();}
		
	}

	@Override
	public Resource getResource(SmartrEffExtResourceTypeData object, OgemaHttpRequest req) {
		return null;
	}
	
	@Override
	public String getLineId(SmartrEffExtResourceTypeData object) {
		String name = WidgetHelper.getValidWidgetId(object.typeDeclaration.label(null));
		return name + super.getLineId(object);
	}
	
	protected UserAdmin getUserAdmin() {
		return app.getUserAdmin();
	}
	protected int getEditNum(Class<? extends Resource> resType) {
		return getUserAdmin().getUserData().editableData().getSubResources(resType, true).size();
	}
	protected int getNonEditNum(Class<? extends Resource> resType) {
		return getUserAdmin().getUserData().getSubResources(resType, true).size();
	}
}
