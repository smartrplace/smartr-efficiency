package org.smartrplace.external.accessadmin.gui;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.ogema.accessadmin.api.UserPermissionService;
import org.ogema.accessadmin.api.util.UserPermissionUtil;
import org.ogema.core.model.ResourceList;
import org.ogema.core.model.simple.BooleanResource;
import org.ogema.model.locations.BuildingPropertyUnit;
import org.ogema.model.locations.Room;
import org.ogema.tools.resource.util.ResourceUtils;
import org.smartrplace.external.accessadmin.AccessAdminController;
import org.smartrplace.external.accessadmin.config.AccessConfigUser;
import org.smartrplace.gui.filtering.SingleFiltering.OptionSavingMode;
import org.smartrplace.gui.filtering.util.UserFilteringBase;
import org.smartrplace.gui.filtering.util.UserFilteringWithGroups;
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

public class UserRoomGroupPermissionPage extends StandardPermissionPage<BuildingPropertyUnit> {
	protected final AccessAdminController controller;
	
	protected UserFilteringBase<Room> userFilter;
	//protected RoomFilteringWithGroups<Room> roomFilter;

	protected ResourceList<AccessConfigUser> userPerms;
	
	public UserRoomGroupPermissionPage(WidgetPage<?> page, AccessAdminController controller) {
		super(page, controller.appMan, ResourceHelper.getSampleResource(BuildingPropertyUnit.class));
		this.controller = controller;
		userPerms = controller.appConfigData.userPermissions();
		triggerPageBuild();
	}

	@Override
	protected String getTypeName(OgemaLocale locale) {
		return "Room Group";
	}

	@Override
	protected String getLabel(BuildingPropertyUnit obj) {
		return ResourceUtils.getHumanReadableShortName(obj);
	}

	@Override
	protected List<String> getPermissionNames() {
		return Arrays.asList(UserPermissionService.ROOMPERMISSONS);
	}

	@Override
	protected ConfigurablePermission getAccessConfig(BuildingPropertyUnit object, String permissionID,
			OgemaHttpRequest req) {
		String userName = userFilter.getSelectedUser(req);
		AccessConfigUser userAcc = UserPermissionUtil.getUserPermissions(
				userPerms, userName);
		ConfigurablePermission result = new ConfigurablePermission();
		//We have to choose the right permission data for the page here
		if(userAcc == null)
			userAcc = UserPermissionUtil.getOrCreateUserPermissions(userPerms, userName);
		result.accessConfig = userAcc.roompermissionData();
		result.resourceId = object.getLocation();
		result.permissionId = permissionID;
		result.defaultStatus = controller.userPermService.getUserPermissionForRoom(userName, result.resourceId,
				permissionID, true) > 0;
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
		userFilter = new UserFilteringWithGroups<Room>(page, "userFilter",
				OptionSavingMode.GENERAL, controller);
		
		Button addUserGroup = new Button(page, "addUserGroup", "Add User Group") {
			private static final long serialVersionUID = 1L;
			@Override
			public void onPOSTComplete(String data, OgemaHttpRequest req) {
				AccessConfigUser grp = ResourceListHelper.createNewNamedElement(
						controller.appConfigData.userPermissions(),
						"New User Group", false);
				ValueResourceHelper.setCreate(grp.isGroup(), true);
				grp.activate(true);
			}
		};
		Button addRoomGroup = new Button(page, "addRoomGroup", "Add Room Group") {
			private static final long serialVersionUID = 1L;
			@Override
			public void onPOSTComplete(String data, OgemaHttpRequest req) {
				BuildingPropertyUnit grp = ResourceListHelper.createNewNamedElement(
						controller.appConfigData.roomGroups(),
						"New Room Group", false);
				grp.activate(true);
			}
		};
		//roomFilter.registerDependentWidget(mainTable);
		userFilter.registerDependentWidget(mainTable);
		addRoomGroup.registerDependentWidget(mainTable);
		RedirectButton userAdminLink = new RedirectButton(page, "userAdminLink", "User App Access Configuration",
				"/de/iwes/ogema/apps/logtransfermodus/index.html");
		
		topTable.setContent(0, 1, userFilter); //.setContent(0,  2, roomFilter);
		topTable.setContent(1, 0, addUserGroup).setContent(1, 1, addRoomGroup).setContent(1, 2, userAdminLink);
		page.append(topTable);
		//dualFiltering = new DualFiltering<String, Room, Room>(
		//		userFilter, roomFilter);
	}
	
	@Override
	protected void addNameLabel(BuildingPropertyUnit object,
			ObjectResourceGUIHelper<BuildingPropertyUnit, BooleanResource> vh, String id, Row row) {
		vh.valueEdit(getTypeName(null), id, object.name(), row, alert);
	}

	@Override
	public Collection<BuildingPropertyUnit> getObjectsInTable(OgemaHttpRequest req) {
		List<BuildingPropertyUnit> all = controller.appConfigData.roomGroups().getAllElements();
		//List<Room> result = roomFilter.getFiltered(all, req);
		
		return all;
	}
}
