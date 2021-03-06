package org.smartrplace.smarteff.admin.gui;

import java.util.Collection;

import org.ogema.core.application.ApplicationManager;
import org.ogema.core.model.Resource;
import org.ogema.generictype.GenericDataTypeDeclaration;
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

/** Page that shows all Navi-pages in the system and allows to open top-level pages directly*/
public class NaviOverviewPage extends ObjectGUITablePage<NavigationPageData, Resource> {
	private final SpEffAdminController app;
	
	public NaviOverviewPage(final WidgetPage<?> page, final SpEffAdminController app,
			NavigationPageData initData, boolean autoBuildPage) {
		super(page, app.appMan, initData, autoBuildPage);
		this.app = app;
		//retardationOnGET = 1000;
	}
	
	@Override
	public void addWidgetsAboveTable() {
		Header header = new Header(page, "header", "Navigation Page Overview Administration");
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
			if(text == null) text = t.getType().representingResourceType().getSimpleName();
			else text += "; "+t.getType().representingResourceType().getSimpleName();
		}
		vh.stringLabel("Entry Types", id, text, row);
		if(object.provider.typesListedInTable() == null) text = "--";
		else for(GenericDataTypeDeclaration gt: object.provider.typesListedInTable()) {
			if(text == null) text = gt.representingResourceType().getSimpleName();
			else text += "; "+gt.representingResourceType().getSimpleName();			
		}
		vh.stringLabel("Table Types", id, text, row);
		//ExtensionResourceAccessInitData systemAccess = app.getUserAdmin().getAccessData(null, req, object.provider);
		//SPPageUtil.addOpenButton("Open", null, null, vh, id, row, object, systemAccess.systemAccess(), "Open", "--");
		if(object.provider.getEntryTypes() == null) {
			TemplateRedirectButton<NavigationPageData> but = vh.linkingButton("Open", id, null, row, "Open", object.url);
			but.setDefaultOpenInNewTab(false);
			if(req != null) {
				Button favoriteButton = new Button(vh.getParent(), "favoriteButton"+id, "", req) {
					private static final long serialVersionUID = 8917341845056555889L;
					@Override
					public void onGET(OgemaHttpRequest req) {
						if(app.pageAdmin.isInMenu(object.provider))
							setText("Remove from Favourites", req);
						else
							setText("Add to Favourites", req);
					}
					@Override
					public void onPOSTComplete(String data, OgemaHttpRequest req) {
						if(app.pageAdmin.isInMenu(object.provider))
							app.pageAdmin.removePageFromMenu(object.provider);
						else
							app.pageAdmin.addPageToMenu(object.provider);
					}
				};
				row.addCell("Favourite", favoriteButton);
				
				Button startPageButton = new Button(vh.getParent(), "startPageButton"+id, "", req) {
					private static final long serialVersionUID = 1L;
					@Override
					public void onGET(OgemaHttpRequest req) {
						if(app.pageAdmin.isStartPage(object.provider)) {
							disable(req);
							setText("Is start page", req);
						} else	{
							setText("Make start page", req);
							if(app.pageAdmin.isInMenu(object.provider)) {
								enable(req);
							} else {
								disable(req);
							}
						}
					}
					@Override
					public void onPOSTComplete(String data, OgemaHttpRequest req) {
						app.pageAdmin.makeStartPage(object.provider);
					}
				};
				row.addCell("Start_Page", startPageButton);
				triggerOnPost(favoriteButton, startPageButton);
			} else {
				vh.registerHeaderEntry("Favourite");
				vh.registerHeaderEntry("Start Page");
			}
		} else {
			vh.stringLabel("Open", id, "--", row);
			vh.stringLabel("Favourite", id, "--", row);
			vh.stringLabel("Start Page", id, "--", row);
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
