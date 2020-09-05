package org.smartrplace.external.accessadmin.gui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

import org.ogema.accessadmin.api.ApplicationManagerPlus;
import org.ogema.accessadmin.api.UserPermissionService;
import org.ogema.accessadmin.api.util.UserPermissionUtil;
import org.ogema.core.model.ResourceList;
import org.ogema.model.locations.Room;
import org.ogema.timeseries.eval.simple.api.KPIResourceAccess;
import org.ogema.timeseries.eval.simple.api.TimeProcUtil;
import org.ogema.tools.resource.util.ResourceUtils;
import org.smartrplace.external.accessadmin.config.AccessAdminConfig;
import org.smartrplace.external.accessadmin.config.AccessConfigUser;
import org.smartrplace.external.accessadmin.gui.UserTaggedTbl.RoomTbl;
import org.smartrplace.gui.filtering.SingleFiltering.OptionSavingMode;
import org.smartrplace.gui.filtering.util.RoomFilteringWithGroups;
import org.smartrplace.gui.filtering.util.UserFiltering2Steps;

import de.iwes.util.resource.ResourceHelper;
import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.html.StaticTable;
import de.iwes.widgets.api.widgets.localisation.OgemaLocale;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;

@SuppressWarnings("serial")
public class UserRoomPermissionPage extends StandardPermissionPageWithUserFilter<RoomTbl> {
	//protected final AccessAdminController controller;
	protected final AccessAdminConfig appConfigData;
	protected final ApplicationManagerPlus appManPlus;
	
	protected UserFiltering2Steps<Room> userFilter;
	protected RoomFilteringWithGroups<Room> roomFilter;

	protected ResourceList<AccessConfigUser> userPerms;
	
	public UserRoomPermissionPage(WidgetPage<?> page, AccessAdminConfig appConfigData, ApplicationManagerPlus appManPlus) {
		super(page, appManPlus.appMan(), new RoomTbl(ResourceHelper.getSampleResource(Room.class), null));
		this.appConfigData = appConfigData;
		this.appManPlus = appManPlus;
		userPerms = appConfigData.userPermissions();
		triggerPageBuild();
	}

	@Override
	protected String getHeader(OgemaLocale locale) {
		return "Single Room Access Permissions";
	}
	
	@Override
	protected String getTypeName(OgemaLocale locale) {
		return "Room";
	}

	@Override
	protected String getLabel(RoomTbl obj, OgemaHttpRequest req) {
		return ResourceUtils.getHumanReadableShortName(obj.room);
	}

	@Override
	protected List<String> getPermissionNames() {
		return Arrays.asList(UserPermissionService.ROOMPERMISSONS);
	}

	@Override
	protected ConfigurablePermission getAccessConfig(RoomTbl object, String permissionID,
			OgemaHttpRequest req) {
		String userName = userFilter.getSelectedUser(req);
		AccessConfigUser userAcc = UserPermissionUtil.getUserPermissions(
				userPerms, userName);
		ConfigurablePermission result = new ConfigurablePermission() {
			@Override
			public boolean supportsUnset() {
				return true;
			}
		};
		//We have to choose the right permission data for the page here
		if(userAcc == null)
			userAcc = UserPermissionUtil.getOrCreateUserPermissions(userPerms, userName);
		result.accessConfig = userAcc.roompermissionData();
		result.resourceId = object.room.getLocation();
		result.permissionId = permissionID;
		//String userName = userAcc.name().getValue();
		result.defaultStatus = appManPlus.userPermService().getUserPermissionForRoom(userName, result.resourceId,
				permissionID, true) > 0;
		return result;
	}

	@Override
	public void addWidgetsAboveTable() {
		super.addWidgetsAboveTable();
		StaticTable topTable = new StaticTable(1, 5);
		roomFilter = new RoomFilteringWithGroups<Room>(page, "roomFilter",
				OptionSavingMode.PER_USER, TimeProcUtil.HOUR_MILLIS, appConfigData.roomGroups(), false, appMan) {
			@Override
			protected Room getAttribute(Room object) {
				return object;
			}
		};
		//userFilter = new UserFilteringWithGroups<Room>(page, "userFilter",
		//		OptionSavingMode.GENERAL, 5000, controller);
		userFilter = new UserFiltering2Steps<Room>(page, "userFilter",
				OptionSavingMode.GENERAL, 5000, appConfigData, appManPlus) {

					@Override
					protected String getAttribute(Room object) {
						throw new IllegalStateException("GetAttribute should never be called on this UserFiltering! This does not really filter for room(s)...");
					}
			
		};
		
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
		roomFilter.registerDependentWidget(mainTable);
		userFilter.registerDependentWidget(mainTable);
		//addRoomGroup.registerDependentWidget(mainTable);
		//RedirectButton userAdminLink = new RedirectButton(page, "userAdminLink", "User Administration",
		//		"/de/iwes/ogema/apps/logtransfermodus/index.html");
		
		topTable.setContent(0, 0, userFilter.getFirstDropdown()).setContent(0, 1, userFilter).setContent(0,  3, roomFilter);
		//topTable.setContent(1, 1, addRoomGroup).setContent(1, 2, userAdminLink);
		page.append(topTable);
		//dualFiltering = new DualFiltering<String, Room, Room>(
		//		userFilter, roomFilter);
	}

	@Override
	public Collection<RoomTbl> getObjectsInTable(OgemaHttpRequest req) {
		List<Room> all = KPIResourceAccess.getRealRooms(appMan.getResourceAccess()); //.getToplevelResources(Room.class);
		List<Room> result1 = roomFilter.getFiltered(all, req);
		result1.sort(new Comparator<Room>() {

			@Override
			public int compare(Room o1, Room o2) {
				return o1.name().getValue().compareTo(o2.name().getValue());
			}
		});
		
		String user = userFilter.getSelectedUser(req);
		List<RoomTbl> result = new ArrayList<>();
		for(Room room: result1) {
			result.add(new RoomTbl(room, user));
		}

		return result;
	}
}
