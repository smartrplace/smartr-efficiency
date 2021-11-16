package org.smartrplace.external.accessadmin.gui;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.ogema.accessadmin.api.ConfigurablePermission;
import org.ogema.accessadmin.api.UserPermissionService;
import org.ogema.accessadmin.api.UserPermissionUtil;
import org.ogema.core.model.simple.BooleanResource;
import org.ogema.tools.resource.util.ResourceUtils;
import org.smartrplace.external.accessadmin.AccessAdminController;
import org.smartrplace.external.accessadmin.config.AccessConfigUser;
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

public class UserGroupPermissionPage extends StandardPermissionPage<AccessConfigUser> {
	protected final AccessAdminController controller;
	
	//protected UserFilteringBase<Room> userFilter;
	//protected RoomFilteringWithGroups<Room> roomFilter;

	//protected ResourceList<AccessConfigUser> userPerms;
	
	public UserGroupPermissionPage(WidgetPage<?> page, AccessAdminController controller) {
		super(page, controller.appMan, ResourceHelper.getSampleResource(AccessConfigUser.class));
		this.controller = controller;
		//userPerms = controller.appConfigData.userPermissions();
		triggerPageBuild();
	}

	@Override
	protected String getHeader(OgemaLocale locale) {
		return "6. User Appstore Mapping";
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
	protected List<String> getPermissionNames() {
		return Arrays.asList(UserPermissionService.PROPUNITPERMISSIONS);
	}

	@Override
	protected ConfigurablePermission getAccessConfig(AccessConfigUser userAcc, String permissionID,
			OgemaHttpRequest req) {
		String userName = userAcc.name().getValue();
		//String userName = userFilter.getSelectedUser(req);
		//AccessConfigUser userAcc = UserPermissionUtil.getUserPermissions(
		//		userPerms, userName);
		ConfigurablePermission result = new ConfigurablePermission();

		result.accessConfig = userAcc.otherResourcepermissionData(); //.roompermissionData();
		result.resourceId = UserPermissionUtil.SYSTEM_RESOURCE_ID;
		result.permissionId = permissionID;
		result.defaultStatus = controller.userPermService.getUserSystemPermission(userName,permissionID, true) > 0;
		return result;
	}

	@Override
	public void addWidgetsAboveTable() {
		super.addWidgetsAboveTable();
		StaticTable topTable = new StaticTable(2, 5);
		/*roomFilter = new RoomFilteringWithGroups<Room>(page, "roomFilter",
				OptionSavingMode.PER_USER, controller.appConfigData.roomGroups()) {
			private static final long serialVersionUID = 1L;

			@Override
			protected Room getAttribute(Room object) {
				return object;
			}
		};*/
		//userFilter = new UserFilteringWithGroups<Room>(page, "userFilter",
		//		OptionSavingMode.GENERAL, controller);
		
		//roomFilter.registerDependentWidget(mainTable);
		//userFilter.registerDependentWidget(mainTable);
		RedirectButton userAdminLink = new RedirectButton(page, "userAdminLink", "User Administration",
				UserSetupPage.USER_ADMINISTRATION_LINK);
		
		//topTable.setContent(0, 1, userFilter); //.setContent(0,  2, roomFilter);
		if(!Boolean.getBoolean("org.smartrplace.external.accessadmin.gui.hideAddUserGroupButton")) {
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
			topTable.setContent(1, 0, addUserGroup);
		}
		topTable.setContent(1, 1, "").setContent(1, 2, userAdminLink);
		page.append(topTable);
		//dualFiltering = new DualFiltering<String, Room, Room>(
		//		userFilter, roomFilter);
	}
	
	@Override
	protected void addNameLabel(AccessConfigUser object,
			ObjectResourceGUIHelper<AccessConfigUser, BooleanResource> vh, String id, Row row,
			OgemaHttpRequest req) {
		vh.valueEdit(getTypeName(null), id, object.name(), row, alert);
	}

	@Override
	public Collection<AccessConfigUser> getObjectsInTable(OgemaHttpRequest req) {
		List<AccessConfigUser> all = controller.getUserGroups(false);
		return all;
	}
}
