package org.smartrplace.external.accessadmin.gui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.ogema.accessadmin.api.UserPermissionService;
import org.ogema.accessadmin.api.util.UserPermissionUtil;
import org.ogema.core.model.ResourceList;
import org.ogema.model.locations.BuildingPropertyUnit;
import org.ogema.model.locations.Room;
import org.ogema.tools.resource.util.ResourceUtils;
import org.smartrplace.external.accessadmin.AccessAdminController;
import org.smartrplace.external.accessadmin.config.AccessConfigUser;
import org.smartrplace.external.accessadmin.gui.UserTaggedTbl.RoomGroupTbl;
import org.smartrplace.gui.filtering.SingleFiltering.OptionSavingMode;
import org.smartrplace.gui.filtering.util.UserFiltering2Steps;

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

@Deprecated
@SuppressWarnings("serial")
public class UserRoomGroupPermissionPage extends StandardPermissionPageWithUserFilter<RoomGroupTbl> {
	protected static final String SINGLE_ROOM_MAPPING_LINK = "/org/smartrplace/external/actionadmin/singleroome.html";

	protected final AccessAdminController controller;
	
	//protected UserFilteringBase<Room> userFilter;
	protected UserFiltering2Steps<Room> userFilter;
	//protected RoomFilteringWithGroups<Room> roomFilter;

	protected ResourceList<AccessConfigUser> userPerms;
	
	public UserRoomGroupPermissionPage(WidgetPage<?> page, AccessAdminController controller) {
		super(page, controller.appMan, new RoomGroupTbl(ResourceHelper.getSampleResource(BuildingPropertyUnit.class), null));
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
	protected String getLabel(RoomGroupTbl obj) {
		return ResourceUtils.getHumanReadableShortName(obj.roomGrp);
	}

	@Override
	protected List<String> getPermissionNames() {
		return Arrays.asList(UserPermissionService.ROOMPERMISSONS);
	}

	@Override
	protected ConfigurablePermission getAccessConfig(RoomGroupTbl object, String permissionID,
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
	}

	@Override
	public void addWidgetsAboveTable() {
		super.addWidgetsAboveTable();
		/*roomFilter = new RoomFilteringWithGroups<Room>(page, "roomFilter",
				OptionSavingMode.PER_USER, controller.appConfigData.roomGroups()) {
			private static final long serialVersionUID = 1L;

			@Override
			protected Room getAttribute(Room object) {
				return object;
			}
		};*/
		userFilter = new UserFiltering2Steps<Room>(page, "userFilter",
				OptionSavingMode.GENERAL, 5000, controller.appConfigData, controller.appManPlus);
		
		/*Button addRoomGroup = new Button(page, "addRoomGroup", "Add Room Group") {
			private static final long serialVersionUID = 1L;
			@Override
			public void onPOSTComplete(String data, OgemaHttpRequest req) {
				BuildingPropertyUnit grp = ResourceListHelper.createNewNamedElement(
						controller.appConfigData.roomGroups(),
						"New Room Group", false);
				grp.activate(true);
			}
		};*/
		//roomFilter.registerDependentWidget(mainTable);
		userFilter.registerDependentWidget(mainTable);
		//addRoomGroup.registerDependentWidget(mainTable);
		//RedirectButton userAdminLink = new RedirectButton(page, "userAdminLink", "User Administration",
		//		"/de/iwes/ogema/apps/logtransfermodus/index.html");
		
		StaticTable topTable;
		if(Boolean.getBoolean("org.smartrplace.external.accessadmin.gui.hideAddUserGroupButton")) {
			topTable = new StaticTable(1, 5);
		} else {
			topTable = new StaticTable(2, 5);
		}
		topTable.setContent(0, 0, userFilter.getFirstDropdown()).setContent(0, 1, userFilter); //.setContent(0,  2, roomFilter);
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
		//topTable.setContent(1, 1, addRoomGroup).setContent(1, 2, userAdminLink);
		page.append(topTable);
		//dualFiltering = new DualFiltering<String, Room, Room>(
		//		userFilter, roomFilter);
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
	//protected void addNameLabel(RoomGroupTbl object,
	//		ObjectResourceGUIHelper<RoomGroupTbl, BooleanResource> vh, String id, Row row) {
	//	vh.valueEdit(getTypeName(null), id, object.roomGrp.name(), row, alert);
	//}

	@Override
	public Collection<RoomGroupTbl> getObjectsInTable(OgemaHttpRequest req) {
		List<BuildingPropertyUnit> all = controller.appConfigData.roomGroups().getAllElements();
		//List<Room> result = roomFilter.getFiltered(all, req);
		all.sort(new Comparator<BuildingPropertyUnit>() {

			@Override
			public int compare(BuildingPropertyUnit o1, BuildingPropertyUnit o2) {
				return o1.name().getValue().compareTo(o2.name().getValue());
			}
		});
		
		String userName = userFilter.getSelectedUser(req);
		if(userName == null)
			return Collections.emptyList();
		List<RoomGroupTbl> result = new ArrayList<>();
		for(BuildingPropertyUnit room: all) {
			result.add(new RoomGroupTbl(room, userName));
		}

		return result;
	}
}
