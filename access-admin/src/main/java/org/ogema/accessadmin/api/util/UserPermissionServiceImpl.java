package org.ogema.accessadmin.api.util;

import java.util.ArrayList;
import java.util.List;

import org.ogema.accessadmin.api.UserPermissionService;
import org.ogema.accessadmin.api.UserStatus;
import org.ogema.accessadmin.api.util.UserPermissionUtil.PermissionForLevelProvider;
import org.ogema.accessadmin.api.util.UserPermissionUtil.RoomPermissionData;
import org.ogema.core.administration.UserAccount;
import org.ogema.core.model.ResourceList;
import org.ogema.internationalization.util.LocaleHelper;
import org.ogema.model.locations.BuildingPropertyUnit;
import org.ogema.model.locations.Room;
import org.ogema.tools.app.createuser.UserAdminBaseUtil;
import org.ogema.tools.app.createuser.UserAdminBaseUtil.UserStatusResult;
import org.smartrplace.external.accessadmin.AccessAdminController;
import org.smartrplace.external.accessadmin.config.AccessConfigUser;
import org.smartrplace.gui.filtering.GenericFilterFixedGroup;

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
		return getUserPermissionForRoom(userName, room.getLocation(), permissionType, false);
	}
	/**
	 * @param getSuperSetting if true the specific setting for the user and room will be ignored and
	 * 		only the more general setting will be returned
	 */
	public int getUserPermissionForRoom(String userName, String resourceId, String permissionType,
			boolean getSuperSetting) {
		AccessConfigUser userAcc = UserPermissionUtil.getUserPermissions(userPerms, userName);
		if(userAcc == null)
			return 0;
		List<String> roomGroups = getRoomGroups(resourceId);
		Integer result;
		result = getUserPermissionForRoomLevel(userAcc, resourceId, permissionType, roomGroups, getSuperSetting);
		if(result != null)
			return result;
		result = UserPermissionUtil.getUserPermissionForUserGroupLevel(controller.getAllGroupsForUser(userAcc), resourceId,
				permissionType, new PermissionForLevelProvider() {

					@Override
					public Integer getUserPermissionForLevel(AccessConfigUser userAcc, String resourceId,
							String permissionType) {
						return getUserPermissionForRoomLevel(userAcc, resourceId, permissionType, roomGroups, false);
					}
			
		});
		if(result != null)
			return result;
		return 0;
	}
	
	public Integer getUserPermissionForRoomLevel(AccessConfigUser userAcc, String resourceId, String permissionType,
			List<String> roomGroups, boolean getSuperSetting) {
		Integer result;
		if(getSuperSetting)
			result = null;
		else
			result = UserPermissionUtil.getRoomAccessPermission(resourceId, permissionType, userAcc);
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

	/*@Override
	/public int getUserPermissionForUnitApps(String userName, String unitName, String appName, String permissionType) {
	/	return getUserPermissionForUnitApps(userName, unitName, appName, permissionType, false);
	/}
	public int getUserPermissionForUnitApps(String userName, String unitName, String appName, String permissionType,
			boolean getSuperSetting) {
		AccessConfigUser userAcc = UserPermissionUtil.getUserPermissions(userPerms, userName);
		if(userAcc == null)
			return 0;
		Integer result = null;
		if(!getSuperSetting)
			result = UserPermissionUtil.getOtherAccessPermission(unitName, appName, permissionType, userAcc);
		if(result != null)
			return result;
		result = UserPermissionUtil.getUserPermissionForUserGroupLevel(controller.getAllGroupsForUser(userAcc), appName,
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
	}*/

	@Override
	public int getUserPermissionForApp(String userName, String appName, String permissionType) {
		return getUserPermissionForApp(userName, appName, permissionType, false);
	}
	public int getUserPermissionForApp(String userName, String appName, String permissionType,
			boolean getSuperSetting) {
		AccessConfigUser userAcc = UserPermissionUtil.getUserPermissions(userPerms, userName);
		if(userAcc == null)
			return 0;
		Integer result = null;
		if(!getSuperSetting)
			result = UserPermissionUtil.getAppAccessPermission(appName, permissionType, userAcc);
		if(result != null)
			return result;
		result = UserPermissionUtil.getUserPermissionForUserGroupLevel(controller.getAllGroupsForUser(userAcc), appName,
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

	@Override
	public int getUserSystemPermission(String userName, String permissionType) {
		return getUserSystemPermission(userName, permissionType, false);
	}
	public int getUserSystemPermission(String userName, String permissionType,
			boolean getSuperSetting) {
		AccessConfigUser userAcc = UserPermissionUtil.getUserPermissions(userPerms, userName);
		if(userAcc == null)
			return 0;
		Integer result = null;
		if(!getSuperSetting)
			result = UserPermissionUtil.getOtherSystemAccessPermission(permissionType, userAcc);
		if(result != null)
			return result;
		result = UserPermissionUtil.getUserPermissionForUserGroupLevel(controller.getAllGroupsForUser(userAcc), 
				UserPermissionUtil.SYSTEM_RESOURCE_ID,
				permissionType, new PermissionForLevelProvider() {

					@Override
					public Integer getUserPermissionForLevel(AccessConfigUser userAcc, String resourceId,
							String permissionType) {
						return UserPermissionUtil.getOtherSystemAccessPermission(permissionType, userAcc);
					}
			
		});
		if(result != null)
			return result;
		return 0;
	}

	@Override
	public int getUserStatusAppPermission(UserStatus userStatus, String permissionType, boolean useWorkingCopy) {
		RoomPermissionData mapData = UserPermissionUtil.getResourcePermissionData(userStatus.name(),
				useWorkingCopy?controller.appConfigData.userStatusPermissionWorkingCopy():controller.appConfigData.userStatusPermission());
		
		Integer result = mapData.permissions.get(permissionType);
		if(result == null)
			return 0;
		return result;
	}
	
	@Override
	public GenericFilterFixedGroup<String, AccessConfigUser> getUserGroupFiler(String userGroupName) {
		List<AccessConfigUser> grps = controller.getUserGroups(false);
		for(AccessConfigUser grp: grps) {
			if(grp.isGroup().getValue() < 2)
				continue;
			if(grp.name().getValue().equals(userGroupName)) {
				return new GenericFilterFixedGroup<String, AccessConfigUser>(grp, LocaleHelper.getLabelMap(userGroupName)) {

					@Override
					public boolean isInSelection(String object, AccessConfigUser group) {
						UserAccount userAccount = controller.appMan.getAdministrationManager().getUser(object);
						UserStatusResult status = UserAdminBaseUtil.getUserStatus(userAccount, controller.appManPlus, false);
						return userGroupName.equals(UserStatus.getLabel(status.status, null));
					}
				};
			}
		}
		return null;
	}
}
