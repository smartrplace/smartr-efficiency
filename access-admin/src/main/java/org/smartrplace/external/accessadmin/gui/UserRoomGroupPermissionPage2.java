package org.smartrplace.external.accessadmin.gui;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

import org.ogema.accessadmin.api.UserPermissionService;
import org.ogema.accessadmin.api.util.UserPermissionUtil;
import org.ogema.core.model.ResourceList;
import org.ogema.core.model.simple.BooleanResource;
import org.ogema.model.locations.BuildingPropertyUnit;
import org.ogema.tools.resource.util.ResourceUtils;
import org.smartrplace.external.accessadmin.AccessAdminController;
import org.smartrplace.external.accessadmin.config.AccessConfigBase;
import org.smartrplace.external.accessadmin.config.AccessConfigUser;

import de.iwes.util.resource.ResourceHelper;
import de.iwes.util.resource.ValueResourceHelper;
import de.iwes.util.resourcelist.ResourceListHelper;
import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.html.StaticTable;
import de.iwes.widgets.api.widgets.localisation.OgemaLocale;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.alert.Alert;
import de.iwes.widgets.html.alert.AlertData;
import de.iwes.widgets.html.form.button.Button;

@SuppressWarnings("serial")
public class UserRoomGroupPermissionPage2 extends PerMultiselectConfigPage<AccessConfigUser, BuildingPropertyUnit, BooleanResource> {
	protected static final String SINGLE_ROOM_MAPPING_LINK = "/de/iwes/ogema/apps/logtransfermodus/singleroom.html";

	protected final AccessAdminController controller;
	
	//protected UserFilteringBase<Room> userFilter;
	//protected UserFiltering2Steps<Room> userFilter;
	//protected RoomFilteringWithGroups<Room> roomFilter;

	protected ResourceList<AccessConfigUser> userPerms;
	
	public UserRoomGroupPermissionPage2(WidgetPage<?> page, AccessAdminController controller) {
		super(page, controller.appMan, ResourceHelper.getSampleResource(AccessConfigUser.class));
		this.controller = controller;
		userPerms = controller.appConfigData.userPermissions();
		triggerPageBuild();
	}

	@Override
	protected String getHeader(OgemaLocale locale) {
		return "4. User - Room Group Mapping";
	}
	
	@Override
	protected String getTypeName(OgemaLocale locale) {
		return "Room Group";
	}

	@Override
	protected String getLabel(AccessConfigUser obj) {
		return ResourceUtils.getHumanReadableShortName(obj);
	}

	//@Override
	//protected List<String> getPermissionNames() {
	//	return Arrays.asList(UserPermissionService.ROOMPERMISSONS);
	//}

	/*@Override
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
		result.resourceId = object.roomGrp.getLocation();
		result.permissionId = permissionID;
		result.defaultStatus = controller.userPermService.getUserPermissionForRoom(userName, result.resourceId,
				permissionID, true) > 0;
		return result;
	}*/

	@Override
	public void addWidgetsAboveTable() {
		// TODO Auto-generated method stub
		super.addWidgetsAboveTable();
	
		//userFilter = new UserFiltering2Steps<Room>(page, "userFilter",
		//		OptionSavingMode.GENERAL, 5000, controller.appConfigData, controller.appManPlus);
		
		//userFilter.registerDependentWidget(mainTable);
		
		StaticTable topTable;
		if(Boolean.getBoolean("org.smartrplace.external.accessadmin.gui.hideAddUserGroupButton")) {
			topTable = new StaticTable(1, 5);
		} else {
			topTable = new StaticTable(2, 5);
		}
		//topTable.setContent(0, 0, userFilter.getFirstDropdown()).setContent(0, 1, userFilter); //.setContent(0,  2, roomFilter);
		if(!Boolean.getBoolean("org.smartrplace.external.accessadmin.gui.hideAddUserGroupButton")) {
			Button addUserGroup = new Button(page, "addUserGroup", "Add User Group") {

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
		page.append(topTable);

		Alert info = new Alert(page, "description","Explanation") {

			@Override
	    	public void onGET(OgemaHttpRequest req) {
	    		String text = "Select a user via the dropdowns at the top of the page. Define which rooms the user can access "
	    				+ "(UserRoomPermission) and which rooms are displayed to the user as default (User Room Priority). You can"
	    				+ " also define for which the user may see data logs and perform administration access such as calendar "
	    				+ "adminitration. This page defines such access based on room groups / room attributes, to add settings for"
	    				+ " single room use the page "
	    				+ "<a href=\"" + SINGLE_ROOM_MAPPING_LINK + "\"><b>Single Room Access Permissions</b></a>.";
				setHtml(text, req);
	    		allowDismiss(true, req);
	    		autoDismiss(-1, req);
	    	}
	    	
	    };
	    info.addDefaultStyle(AlertData.BOOTSTRAP_INFO);
	    info.setDefaultVisibility(true);
	    page.append(info);
	}
	
	//@Override
	//protected void addNameLabel(BuildingPropertyUnit object,
	//		ObjectResourceGUIHelper<BuildingPropertyUnit, BooleanResource> vh, String id, Row row) {
	//	vh.valueEdit(getTypeName(null), id, object.roomGrp.name(), row, alert);
	//}

	@Override
	public Collection<AccessConfigUser> getObjectsInTable(OgemaHttpRequest req) {
		List<AccessConfigUser> all = AccessAdminController.getUserGroups(false, true, controller.appConfigData);
		return all;
	}

	@Override
	protected String getGroupColumnLabel() {
		return "Room Group Access";
	}

	@Override
	protected List<BuildingPropertyUnit> getAllGroups(OgemaHttpRequest req) {
		List<BuildingPropertyUnit> all = controller.appConfigData.roomGroups().getAllElements();
		//List<Room> result = roomFilter.getFiltered(all, req);
		all.sort(new Comparator<BuildingPropertyUnit>() {

			@Override
			public int compare(BuildingPropertyUnit o1, BuildingPropertyUnit o2) {
				return o1.name().getValue().compareTo(o2.name().getValue());
			}
		});
		return all;
		//String userName = userFilter.getSelectedUser(req);
		//if(userName == null)
		//	return Collections.emptyList();
		//List<BuildingPropertyUnit> result = new ArrayList<>();
		//for(BuildingPropertyUnit room: all) {
		//	result.add(new BuildingPropertyUnit(room, null));
		//}

		//return result;
	}

	@Override
	protected List<BuildingPropertyUnit> getGroups(AccessConfigUser object, OgemaHttpRequest req) {
		List<BuildingPropertyUnit> result = new ArrayList<>();
		AccessConfigBase configRes = object.roompermissionData();
		for(BuildingPropertyUnit roomGrp: controller.appConfigData.roomGroups().getAllElements()) {
			Boolean status = UserPermissionUtil.getPermissionStatus(roomGrp.getLocation(), UserPermissionService.USER_ROOM_PERM, configRes);
			if(status != null && status)
				result.add(roomGrp);
		}
		return result;
	}

	@Override
	protected void setGroups(AccessConfigUser object, List<BuildingPropertyUnit> groups, OgemaHttpRequest req) {
		AccessConfigBase configRes = object.roompermissionData();
		for(BuildingPropertyUnit roomGrp: controller.appConfigData.roomGroups().getAllElements()) {
			boolean status = false;
			for(BuildingPropertyUnit selected: groups) {
				if(selected.equalsLocation(roomGrp)) {
					status = true;
					break;
				}
			}
			if(status)
				UserPermissionUtil.addPermission(roomGrp.getLocation(), UserPermissionService.USER_ROOM_PERM, configRes);
			else
				UserPermissionUtil.removePermissionSetting(roomGrp.getLocation(), UserPermissionService.USER_ROOM_PERM, configRes);
		}
	}

	@Override
	protected String getGroupLabel(BuildingPropertyUnit object) {
		return ResourceUtils.getHumanReadableShortName(object);
	}
}
