package org.ogema.accessadmin.api.util;

import java.util.ArrayList;
import java.util.List;

import org.ogema.accessadmin.api.UserPermissionService;
import org.ogema.accessadmin.api.util.UserPermissionUtil.PermissionForLevelProvider;
import org.ogema.core.model.ResourceList;
import org.ogema.model.locations.BuildingPropertyUnit;
import org.ogema.model.locations.Room;
import org.smartrplace.external.accessadmin.AccessAdminController;
import org.smartrplace.external.accessadmin.config.AccessConfigUser;

public class UserPermissionServiceImpl implements UserPermissionService {
	protected final AccessAdminController controller;
	protected final ResourceList<AccessConfigUser> userPerms;
	
	//TODO cash this map
	//protected Map<String, List<String>> getRoomGroups();
	
	/**Get all room groups in the order in which they shall be checked
	 * TODO: No hierarchy in room groups is supported in the current data model yet*/
	protected List<String> getRoomGroups(String roomLocation) {
		List<String> result = new ArrayList<>();
		for(BuildingPropertyUnit bu: controller.appConfigData.roomGroups().getAllElements()) {
			for(Room room: bu.rooms().getAllElements()) {
				if(room.getLocation().equals(roomLocation))
					result.add(bu.getLocation());
			}
		}
		return result ;
	}
	
	public UserPermissionServiceImpl(AccessAdminController controller) {
		this.controller = controller;
		this.userPerms = controller.appConfigData.userPermissions();
	}

	@Override
	public boolean hasUserPermissionForRoom(String userName, Room room) {
		return hasUserPermissionForRoom(userName, room, USER_ROOM_PERM);
	}

	@Override
	public boolean hasUserPermissionForRoom(String userName, Room room, String permissionType) {
		int result = getUserPermissionForRoom(userName, room, permissionType);
		return (result > 0);
	}

	@Override
	public int getUserPermissionForRoom(String userName, Room room, String permissionType) {
		AccessConfigUser userAcc = UserPermissionUtil.getUserPermissions(userPerms, userName);
		if(userAcc == null)
			return 0;
		String resourceId = room.getLocation();
		List<String> roomGroups = getRoomGroups(resourceId);
		Integer result = getUserPermissionForRoomLevel(userAcc, resourceId, permissionType, roomGroups);
		if(result != null)
			return result;
		result = UserPermissionUtil.getUserPermissionForUserGroupLevel(userAcc.superGroups().getAllElements(), resourceId,
				permissionType, new PermissionForLevelProvider() {

					@Override
					public Integer getUserPermissionForLevel(AccessConfigUser userAcc, String resourceId,
							String permissionType) {
						return getUserPermissionForRoomLevel(userAcc, resourceId, permissionType, roomGroups);
					}
			
		});
		if(result != null)
			return result;
		return 0;
	}
	
	public Integer getUserPermissionForRoomLevel(AccessConfigUser userAcc, String resourceId, String permissionType,
			List<String> roomGroups) {
		Integer result = UserPermissionUtil.getRoomAccessPermission(resourceId, permissionType, userAcc);
		if(result == null) {
			for(String roomGrp: roomGroups) {
				result = UserPermissionUtil.getRoomAccessPermission(roomGrp, permissionType, userAcc);
				if(result != null)
					return result;
			}
			return null;
		}
		if(result > 0 || ((permissionType != USER_READ_PERM) && (permissionType != USER_WRITE_PERM)))
			return result;
		return UserPermissionUtil.getRoomAccessPermission(resourceId, USER_ROOM_PERM, userAcc);
	}

	@Override
	public int getUserPermissionForUnitApps(String userName, String unitName, String appName, String permissionType) {
		AccessConfigUser userAcc = UserPermissionUtil.getUserPermissions(userPerms, userName);
		if(userAcc == null)
			return 0;
		Integer result = UserPermissionUtil.getOtherAccessPermission(unitName, appName, permissionType, userAcc);
		if(result != null)
			return result;
		result = UserPermissionUtil.getUserPermissionForUserGroupLevel(userAcc.superGroups().getAllElements(), appName,
				permissionType, new PermissionForLevelProvider() {

					@Override
					public Integer getUserPermissionForLevel(AccessConfigUser userAcc, String resourceId,
							String permissionType) {
						return UserPermissionUtil.getOtherAccessPermission(unitName, appName, permissionType, userAcc);
					}
			
		});
		if(result != null)
			return result;
		return 0;
	}

	@Override
	public int getUserPermissionForApp(String userName, String appName, String permissionType) {
		AccessConfigUser userAcc = UserPermissionUtil.getUserPermissions(userPerms, userName);
		if(userAcc == null)
			return 0;
		Integer result = UserPermissionUtil.getAppAccessPermission(appName, permissionType, userAcc);
		if(result != null)
			return result;
		result = UserPermissionUtil.getUserPermissionForUserGroupLevel(userAcc.superGroups().getAllElements(), appName,
				permissionType, new PermissionForLevelProvider() {

					@Override
					public Integer getUserPermissionForLevel(AccessConfigUser userAcc, String resourceId,
							String permissionType) {
						return UserPermissionUtil.getAppAccessPermission(appName, permissionType, userAcc);
					}
			
		});
		if(result != null)
			return result;
		return 0;
	}

}
