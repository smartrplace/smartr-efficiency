package org.smartrplace.external.accessadmin.gui;

import java.util.Collection;
import java.util.List;

import org.ogema.core.application.ApplicationManager;
import org.ogema.core.model.ResourceList;
import org.ogema.core.model.simple.BooleanResource;
import org.ogema.tools.resource.util.ResourceUtils;
import org.smartrplace.external.accessadmin.AccessAdminController;
import org.smartrplace.external.accessadmin.config.AccessAdminConfig;
import org.smartrplace.external.accessadmin.config.AccessConfigUser;
import org.smartrplace.gui.tablepages.ObjectGUITablePageNamed;
import org.smartrplace.util.directobjectgui.ObjectResourceGUIHelper;
import org.smartrplace.util.directresourcegui.GUIHelperExtension;

import de.iwes.util.resource.ResourceHelper;
import de.iwes.util.resource.ValueResourceHelper;
import de.iwes.util.resourcelist.ResourceListHelper;
import de.iwes.widgets.api.widgets.OgemaWidget;
import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.html.StaticTable;
import de.iwes.widgets.api.widgets.localisation.OgemaLocale;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.alert.Alert;
import de.iwes.widgets.html.alert.AlertData;
import de.iwes.widgets.html.complextable.RowTemplate.Row;
import de.iwes.widgets.html.form.button.Button;

@SuppressWarnings("serial")
public class UserSetupPage extends ObjectGUITablePageNamed<AccessConfigUser, BooleanResource> {
	protected static final String USER_ADMINISTRATION_LINK = "/de/iwes/ogema/apps/useradminsp/index.html";

	//protected final SPSystemBaseMgmtController controller;
	protected final AccessAdminConfig appConfigData;
	
	protected ResourceList<AccessConfigUser> userPerms;
	
	public UserSetupPage(WidgetPage<?> page, AccessAdminConfig appConfigData, ApplicationManager appMan) {
		super(page, appMan, ResourceHelper.getSampleResource(AccessConfigUser.class));
		this.appConfigData =appConfigData;
		userPerms = appConfigData.userPermissions();
		triggerPageBuild();
	}

	@Override
	protected String getHeader(OgemaLocale locale) {
		boolean isGw = Boolean.getBoolean("org.smartrplace.app.srcmon.isgateway");
		return isGw?"3.":"1."+" User Attribute Configuration";
	}
	
	@Override
	protected String getTypeName(OgemaLocale locale) {
		return "User Group";
	}

	@Override
	protected String getLabel(AccessConfigUser obj, OgemaHttpRequest req) {
		return ResourceUtils.getHumanReadableShortName(obj);
	}

	@Override
	public void addWidgetsAboveTable() {
		super.addWidgetsAboveTable();
		StaticTable topTable = new StaticTable(2, 5);
		
		Button addUserGroup = new Button(page, "addUserGroup", "Add User Attribute") {

			@Override
			public void onPOSTComplete(String data, OgemaHttpRequest req) {
				AccessConfigUser grp = ResourceListHelper.createNewNamedElement(
						appConfigData.userPermissions(),
						"New User Attribute", false);
				ValueResourceHelper.setCreate(grp.isGroup(), 1);
				grp.activate(true);
			}
		};
		addUserGroup.registerDependentWidget(mainTable);

		topTable.setContent(1, 1, addUserGroup); //.setContent(1, 2, userAdminLink);
		page.append(topTable);
		
		Alert info = new Alert(page, "description_user","Explanation") {

			@Override
	    	public void onGET(OgemaHttpRequest req) {
	    		String text = "Change the label of user attributes here. User types are defined in the User Administration, these"
	    				+ " labels cannot be edited. The mapping of individual users to the attributes can be set on the page "
	    				+ "<a href=\"" + USER_ADMINISTRATION_LINK + "\"><b>User Administration</b></a>.";
				setHtml(text, req);
	    		allowDismiss(true, req);
	    		autoDismiss(-1, req);
	    	}
	    	
	    };
	    info.addDefaultStyle(AlertData.BOOTSTRAP_INFO);
	    info.setDefaultVisibility(true);
	    page.append(info);
	}
	
	protected boolean addNameLabelExtended(AccessConfigUser object,
			ObjectResourceGUIHelper<AccessConfigUser, BooleanResource> vh, String id, Row row) {
		OgemaWidget edit = vh.valueEdit(getTypeName(null), id, object.name(), row, alert);
		if(object.isGroup().getValue() > 1) {
			edit.disable(vh.getReq());
			return false;
		}
		return true;
	}

	@Override
	public Collection<AccessConfigUser> getObjectsInTable(OgemaHttpRequest req) {
		List<AccessConfigUser> all = AccessAdminController.getUserGroups(false, true, appConfigData);
		return all;
	}

	@Override
	public void addWidgets(AccessConfigUser object, ObjectResourceGUIHelper<AccessConfigUser, BooleanResource> vh, String id,
			OgemaHttpRequest req, Row row, ApplicationManager appMan) {
		if(addNameLabelExtended(object, vh, id, row))
			GUIHelperExtension.addDeleteButton(null, object, mainTable, id, alert, row,
				vh, req);
	}
	
	@Override
	public String getLineId(AccessConfigUser object) {
		if(object.name().getValue().startsWith("New User Attribute"))
			return "0000"+super.getLineId(object);
		return "9999"+super.getLineId(object);
	}
}
