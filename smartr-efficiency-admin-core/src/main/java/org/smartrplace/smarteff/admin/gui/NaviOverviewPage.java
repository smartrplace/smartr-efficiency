package org.smartrplace.smarteff.admin.gui;

import java.util.Collection;

import org.ogema.core.application.ApplicationManager;
import org.ogema.core.model.Resource;
import org.ogema.core.model.simple.TimeResource;
import org.smartrplace.extensionservice.ExtensionCapabilityPublicData.EntryType;
import org.smartrplace.smarteff.admin.SpEffAdminController;
import org.smartrplace.smarteff.admin.object.NavigationPageData;
import org.smartrplace.smarteff.util.SPPageUtil;
import org.smartrplace.util.directobjectgui.ObjectGUITablePage;
import org.smartrplace.util.directobjectgui.ObjectResourceGUIHelper;

import de.iwes.widgets.api.extended.WidgetData;
import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.complextable.RowTemplate.Row;
import de.iwes.widgets.html.form.button.Button;
import de.iwes.widgets.html.form.button.TemplateRedirectButton;
import de.iwes.widgets.html.form.label.Header;
import de.iwes.widgets.resource.widget.textfield.ValueResourceTextField;

/** Page that shows all Navi-pages in the system and allows to open top-level pages directly*/
public class NaviOverviewPage extends ObjectGUITablePage<NavigationPageData, Resource> {
	private final SpEffAdminController app;
	
	ValueResourceTextField<TimeResource> updateInterval;

	public NaviOverviewPage(final WidgetPage<?> page, final SpEffAdminController app,
			NavigationPageData initData) {
		super(page, app.appMan, initData);
		this.app = app;
	}
	
	@Override
	public void addWidgetsAboveTable() {
		Header header = new Header(page, "header", "Navigation Page Overview");
		header.addDefaultStyle(WidgetData.TEXT_ALIGNMENT_LEFT);
		page.append(header);
	}
	
	@Override
	public Collection<NavigationPageData> getObjectsInTable(OgemaHttpRequest req) {
		Collection<NavigationPageData> providers = app.guiPageAdmin.getAllProviders(); 
		return providers;
	}

	@Override
	public void addWidgets(NavigationPageData object, ObjectResourceGUIHelper<NavigationPageData, Resource> vh,
			String id, OgemaHttpRequest req, Row row, ApplicationManager appMan) {
		//ExtensionNavigationPageI<SmartEffUserDataNonEdit, ExtensionResourceAccessInitData> exPage = null;
		if(req != null) {
			vh.stringLabel("Name", id, object.provider.label(req.getLocale()), row);
		} else
			vh.registerHeaderEntry("Name");
		String text = null;
		if(object.provider.getEntryTypes() == null) text = "Start Page";
		else for(EntryType t: object.provider.getEntryTypes()) {
			if(text == null) text = t.getType().getSimpleName();
			else text += "; "+t.getType().getSimpleName();
		}
		vh.stringLabel("Entry Types", id, text, row);
		//ExtensionResourceAccessInitData systemAccess = app.getUserAdmin().getAccessData(null, req, object.provider);
		//SPPageUtil.addOpenButton("Open", null, null, vh, id, row, object, systemAccess.systemAccess(), "Open", "--");
		if(object.provider.getEntryTypes() == null) {
			TemplateRedirectButton<NavigationPageData> but = vh.linkingButton("Open", id, null, row, "Open", object.url);
			but.setDefaultOpenInNewTab(false);
			if(req != null) {
				Button favoriteButton = new Button(vh.getParent(), "favoriteButton"+id, "Add to Favourites", req) {
					private static final long serialVersionUID = 8917341845056555889L;
					@Override
					public void onPOSTComplete(String data, OgemaHttpRequest req) {
						app.pageAdmin.addPageToMenu(object.provider);
					}
				};
				row.addCell("Favourite", favoriteButton);
			} else vh.registerHeaderEntry("Favourite");
		} else {
			vh.stringLabel("Open", id, "--", row);
			vh.stringLabel("Favourite", id, "--", row);
		}
	}
	
	@Override
	public Resource getResource(NavigationPageData object, OgemaHttpRequest req) {
		return null;
	}
	
	@Override
	public String getLineId(NavigationPageData object) {
		String name = SPPageUtil.buildValidWidgetId(object.provider);
		return name; // + super.getLineId(object);
	}
}
