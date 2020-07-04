package org.smartrplace.external.accessadmin.gui;

import java.util.Collection;
import java.util.List;

import org.ogema.core.application.ApplicationManager;
import org.ogema.core.model.ResourceList;
import org.ogema.core.model.simple.BooleanResource;
import org.ogema.tools.resource.util.ResourceUtils;
import org.smartrplace.external.accessadmin.AccessAdminController;
import org.smartrplace.external.accessadmin.config.AccessConfigUser;
import org.smartrplace.gui.filtering.ObjectGUITablePageNamed;
import org.smartrplace.util.directobjectgui.ObjectResourceGUIHelper;

import de.iwes.util.resource.ResourceHelper;
import de.iwes.util.resource.ValueResourceHelper;
import de.iwes.util.resourcelist.ResourceListHelper;
import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.html.StaticTable;
import de.iwes.widgets.api.widgets.localisation.OgemaLocale;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.complextable.RowTemplate.Row;
import de.iwes.widgets.html.form.button.Button;
import de.iwes.widgets.html.form.button.RedirectButton;

@Deprecated
public class UserSetupPageV1 extends ObjectGUITablePageNamed<AccessConfigUser, BooleanResource> {
	protected final AccessAdminController controller;
	
	protected ResourceList<AccessConfigUser> userPerms;
	
	public UserSetupPageV1(WidgetPage<?> page, AccessAdminController controller) {
		super(page, controller.appMan, ResourceHelper.getSampleResource(AccessConfigUser.class));
		this.controller = controller;
		userPerms = controller.appConfigData.userPermissions();
		triggerPageBuild();
	}

	@Override
	protected String getHeader(OgemaLocale locale) {
		return "User - Setup";
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
		
		Button addUserGroup = new Button(page, "addUserGroup", "Add User Group") {
			private static final long serialVersionUID = 1L;
			@Override
			public void onPOSTComplete(String data, OgemaHttpRequest req) {
				AccessConfigUser grp = ResourceListHelper.createNewNamedElement(
						controller.appConfigData.userPermissions(),
						"New User Group", false);
				ValueResourceHelper.setCreate(grp.isGroup(), 1);
				grp.activate(true);
			}
		};
		addUserGroup.registerDependentWidget(mainTable);
		RedirectButton userAdminLink = new RedirectButton(page, "userAdminLink", "User Administration",
				"/de/iwes/ogema/apps/logtransfermodus/index.html");
		
		//topTable.setContent(0, 0, userFilter.getFirstDropdown()).setContent(0, 1, userFilter); //.setContent(0,  2, roomFilter);
		topTable.setContent(1, 1, addUserGroup).setContent(1, 2, userAdminLink);
		page.append(topTable);
		//dualFiltering = new DualFiltering<String, Room, Room>(
		//		userFilter, roomFilter);
	}
	
	@Override
	protected void addNameLabel(AccessConfigUser object,
			ObjectResourceGUIHelper<AccessConfigUser, BooleanResource> vh, String id, Row row) {
		vh.valueEdit(getTypeName(null), id, object.name(), row, alert);
	}

	@Override
	public Collection<AccessConfigUser> getObjectsInTable(OgemaHttpRequest req) {
		List<AccessConfigUser> all = controller.getUserGroups(false);
		return all;
	}

	@Override
	public void addWidgets(AccessConfigUser object, ObjectResourceGUIHelper<AccessConfigUser, BooleanResource> vh, String id,
			OgemaHttpRequest req, Row row, ApplicationManager appMan) {
		addNameLabel(object, vh, id, row);
	}
}
