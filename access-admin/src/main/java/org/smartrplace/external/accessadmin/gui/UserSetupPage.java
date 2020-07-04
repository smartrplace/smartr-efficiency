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
import org.smartrplace.gui.filtering.ObjectGUITablePageNamed;
import org.smartrplace.util.directobjectgui.ObjectResourceGUIHelper;

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
	protected static final String USER_GROUP_MAPPING_LINK = "/de/iwes/ogema/apps/logtransfermodus/index.html";

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
		return "3. User Attribute Configuration";
	}
	
	@Override
	protected String getTypeName(OgemaLocale locale) {
		return "User Group";
	}

	@Override
	protected String getLabel(AccessConfigUser obj) {
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
		//RedirectButton userAdminLink = new RedirectButton(page, "userAdminLink", "User Administration",
		//		"/de/iwes/ogema/apps/logtransfermodus/index.html");
		
		//topTable.setContent(0, 0, userFilter.getFirstDropdown()).setContent(0, 1, userFilter); //.setContent(0,  2, roomFilter);
		topTable.setContent(1, 1, addUserGroup); //.setContent(1, 2, userAdminLink);
		page.append(topTable);
		//dualFiltering = new DualFiltering<String, Room, Room>(
		//		userFilter, roomFilter);
		
		Alert info = new Alert(page, "description_user","Explanation") {

			@Override
	    	public void onGET(OgemaHttpRequest req) {
	    		String text = "Change the label of user attributes here. User types are defined in the User Administration, these"
	    				+ " labels cannot be edited. The mapping of individual users to the attributes can be set on the page "
	    				+ "<a href=\"" + USER_GROUP_MAPPING_LINK + "\"><b>User Administration</b></a>.";
				setHtml(text, req);
	    		allowDismiss(true, req);
	    		autoDismiss(-1, req);
	    	}
	    	
	    };
	    info.addDefaultStyle(AlertData.BOOTSTRAP_INFO);
	    info.setDefaultVisibility(true);
	    page.append(info);
	    
		/*String text = "Change the label of user attributes here. User types are defined in the User Administration, these"
				+ " labels cannot be edited. The mapping of individual users to the attributes can be set on the page "
				+ "<a href=\"" + USER_GROUP_MAPPING_LINK + "\"><b>User Attribute Configuration</b></a>.";
		alert.setDefaultTextAsHtml(true);
		alert.setDefaultText(text);
		alert.setDefaultAllowDismiss(true);
		alert.autoDismiss(-1, null);
	    alert.addDefaultStyle(AlertData.BOOTSTRAP_INFO);
	    alert.setDefaultVisibility(true);*/
	    //page.append(alert);
	}
	
	@Override
	protected void addNameLabel(AccessConfigUser object,
			ObjectResourceGUIHelper<AccessConfigUser, BooleanResource> vh, String id, Row row) {
		OgemaWidget edit = vh.valueEdit(getTypeName(null), id, object.name(), row, alert);
		if(object.isGroup().getValue() > 1)
			edit.disable(vh.getReq());
		//if(object.isGroup().getValue() > 1)
		//	vh.stringLabel(getTypeName(null), id, object.name(), row);
		//else
		//	vh.valueEdit(getTypeName(null), id, object.name(), row, alert);
	}

	@Override
	public Collection<AccessConfigUser> getObjectsInTable(OgemaHttpRequest req) {
		List<AccessConfigUser> all = AccessAdminController.getUserGroups(false, true, appConfigData);
		return all;
	}

	@Override
	public void addWidgets(AccessConfigUser object, ObjectResourceGUIHelper<AccessConfigUser, BooleanResource> vh, String id,
			OgemaHttpRequest req, Row row, ApplicationManager appMan) {
		addNameLabel(object, vh, id, row);
	}
}
