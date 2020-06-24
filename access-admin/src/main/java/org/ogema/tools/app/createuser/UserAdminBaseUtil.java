package org.ogema.tools.app.createuser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import org.ogema.accessadmin.api.ApplicationManagerPlus;
import org.ogema.accessadmin.api.UserPermissionService;
import org.ogema.accessadmin.api.UserStatus;
import org.ogema.accesscontrol.AppPermissionFilter;
import org.ogema.core.administration.UserAccount;
import org.osgi.framework.Version;
import org.osgi.service.condpermadmin.ConditionalPermissionAdmin;
import org.osgi.service.condpermadmin.ConditionalPermissionInfo;
import org.osgi.service.permissionadmin.PermissionInfo;

public class UserAdminBaseUtil {
	protected static final String WEB_ACCESS_PERM_STARTSTRING = "(org.ogema.accesscontrol.WebAccessPermission \"name=";
	public static Collection<String> BASE_APPS = Arrays.asList(new String[]{"org.ogema.widgets.ogema-js-bundle",
		"org.ogema.ref-impl.framework-gui",
		"org.ogema.widgets.widget-experimental",
		"org.ogema.widgets.widget-collection"});
	public static Collection<String> GUEST_APPS;
	static {
		GUEST_APPS = new ArrayList<String>(BASE_APPS);
		GUEST_APPS.add("org.smartrplace.apps.smartrplace-heatcontrol-servlet");
	}

	public static Collection<String> USER_APPS(UserPermissionService userPermService, boolean useWorkingCopy) {
		return getPermissionsCoordinates(UserStatus.USER_STD, userPermService, useWorkingCopy);
		
	}

	public static Collection<String> SECRETARY_APPS(UserPermissionService userPermService, boolean useWorkingCopy) {
		List<String> result = getPermissionsCoordinates(UserStatus.SECRETARY, userPermService, useWorkingCopy);
		//Just to identify the user level, has no GUI
		result.add("org.smartrplace.api.smartr-efficiency-api");
		return result;		
	}

	public static Collection<String> ADMIN_APPS(UserPermissionService userPermService, boolean useWorkingCopy) {
		List<String> result = getPermissionsCoordinates(UserStatus.ADMIN, userPermService, useWorkingCopy);
		//Just to identify the user level, has no GUI
		result.add("org.smartrplace.apps.smartr-efficiency-util");
		return result;
	}
	
	public static Collection<String> SUPERADMIN_APPS(UserPermissionService userPermService, boolean useWorkingCopy) {
		List<String> result = getPermissionsCoordinates(UserStatus.ADMIN, userPermService, useWorkingCopy);
		result.add("org.ogema.ref-impl.framework-administration");
		result.add("org.ogema.messaging.message-settings");
		result.add("org.ogema.apps.room-link");
		result.add("com.example.app.evaluation-offline-control");
		return result;
		
	};
	protected static List<String> getPermissionsCoordinates(UserStatus status,
			UserPermissionService userPermService, boolean useWorkingCopy) {
		List<String> result = new ArrayList<>(GUEST_APPS);
		for(String permType: UserPermissionService.APP_ACCESS_PERMISSIONS) {
			int hasPerm = userPermService.getUserStatusAppPermission(status, permType,
					useWorkingCopy);
			if(hasPerm <= 0)
				continue;
			switch(permType) {
			case UserPermissionService.ROOM_STATUS_CONTROL:
				result.add("org.smartrplace.apps.smartrplace-heatcontrol-v2");
				break;
			case UserPermissionService.MONITORING:
				result.add("org.smartrplace.apps.srcmon-app");
				result.add("org.ogema.tools.schedule-viewer-expert");
				break;
			case UserPermissionService.ALARMING:
				result.add("org.ogema.apps.window-opened-sp");
				result.add("de.iee.ogema.batterystatemonitoring");
				result.add("org.smartrplace.internal.message-reader-v2");
				result.add("org.ogema.messaging.message-forwarding");
				result.add("org.ogema.messaging.message-settings");
				result.add("org.smartrplace.apps.smartr-efficiency-fimon");
				result.add("org.smartrplace.apps.smartr-efficiency-admin");
				break;
			case UserPermissionService.USER_MANAGEMENT:
				result.add("org.ogema.tools.log-transfer-control-sp");
				break;
			case UserPermissionService.INSTALLATION_SETUP:
				result.add("org.smartrplace.apps.hardware-installation");
				result.add("org.smartrplace.drivers.bacnet-ogema-sp-gui");
				break;
			case UserPermissionService.GROUP_AND_PERMISSION_MANAGEMENT:
				result.add("org.smartrplace.apps.access-admin");
				break;
			case UserPermissionService.APPSTORE:
				result.add("org.smartrplace.internal.appstore-gui");
				break;
			}
		}
		return result ;
	}
	
	public static class UserStatusResult {
		public UserStatus status = null;
		public List<String> addPerms = null;
	}
	public static UserStatusResult getUserStatus(UserAccount userAccount, ApplicationManagerPlus appManPlus, boolean useWorkingCopy) {
		String userName = userAccount.getName();
		UserStatusResult result = new UserStatusResult();
		List<String> userPerms = null;
		final boolean natural = appManPlus.permMan().getAccessManager().isNatural(userName);
		if(natural) {
			final boolean admin =  appManPlus.permMan().getAccessManager().isAllAppsPermitted(userName);
			if(!admin) {
				Object sysAdmin = appManPlus.permMan().getSystemPermissionAdmin();
				appManPlus.permMan().getAccessManager().getSupportedAuthenticators(userName);
				if(sysAdmin instanceof ConditionalPermissionAdmin) {
					ConditionalPermissionAdmin cpa = (ConditionalPermissionAdmin) sysAdmin;
					userPerms = UserAdminBaseUtil.getPermissions(userName, cpa);
					if(!userPerms.contains("[(java.security.AllPermission)]")) {
						result.status = UserAdminBaseUtil.getUserStatus(userPerms, appManPlus.userPermService(), useWorkingCopy);
						result.addPerms = UserAdminBaseUtil.getAdditionalPerms(userPerms, result.status, appManPlus.userPermService(),
								useWorkingCopy);
					}
				}
			}
		}
		return result;
	}

	public static UserStatus getUserStatus(List<String> appPermissions, UserPermissionService userPermService, boolean useWorkingCopy) {
		if(hasAllPerms(appPermissions, SUPERADMIN_APPS(userPermService, useWorkingCopy)))
			return UserStatus.SUPERADMIN;
		//if(hasAllPerms(appPermissions, TESTER_APPS))
		//	return UserStatus.TESTER;
		if(hasAllPerms(appPermissions, ADMIN_APPS(userPermService, useWorkingCopy)))
			return UserStatus.ADMIN;
		if(hasAllPerms(appPermissions, SECRETARY_APPS(userPermService, useWorkingCopy)))
			return UserStatus.SECRETARY;
		if(hasAllPerms(appPermissions, USER_APPS(userPermService, useWorkingCopy)))
			return UserStatus.USER_STD;
		//if(hasAllPerms(appPermissions, GUEST_APPS))
		//	return UserStatus.GUEST;
		//if(hasAllPerms(appPermissions, BASE_APPS))
		//	return UserStatus.RAW;
		return UserStatus.DISABLED;
	}

	protected static boolean hasAllPerms(List<String> appPermissions, Collection<String> required) {
		for(String reqPerm: required) {
			if(!appPermissions.contains(reqPerm))
				return false;
		}
		return true;
	}
	
	public static List<String> getAdditionalPerms(List<String> appPermissions, UserStatus status,
			UserPermissionService userPermService, boolean useWorkingCopy) {
		switch(status) {
		case SUPERADMIN:
			return getAdditionalPerms(appPermissions, SUPERADMIN_APPS(userPermService, useWorkingCopy));
		//case TESTER:
		//	return getAdditionalPerms(appPermissions, TESTER_APPS);
		case ADMIN:
			return getAdditionalPerms(appPermissions, ADMIN_APPS(userPermService, useWorkingCopy));
		case SECRETARY:
			return getAdditionalPerms(appPermissions, SECRETARY_APPS(userPermService, useWorkingCopy));
		case USER_STD:
			return getAdditionalPerms(appPermissions, USER_APPS(userPermService, useWorkingCopy));
		//case GUEST:
		//	return getAdditionalPerms(appPermissions, GUEST_APPS);
		//case RAW:
		//	return getAdditionalPerms(appPermissions, BASE_APPS);
		case DISABLED:
			return appPermissions;
		}
		throw new IllegalStateException("Unknown user status type:"+status);
	}
	public static List<String> getAdditionalPerms(Collection<String> appPermissions, Collection<String> require) {
		List<String> result = new ArrayList<>();
		for(String perm: appPermissions) {
			if(!require.contains(perm))
				result.add(perm);
		}
		return result ;	
	}
	
	public static List<String> setPerms(UserAccount userData, List<String> currentUserPerms,
			UserStatus destinationStatus, List<String> additionalPermissionstoMaintain,
			ApplicationManagerPlus appManPlus, boolean useWorkingCopy) {
		switch(destinationStatus) {
		case SUPERADMIN:
			return setPerms(userData, currentUserPerms, SUPERADMIN_APPS(appManPlus.userPermService(), useWorkingCopy), additionalPermissionstoMaintain,
					appManPlus);
		//case TESTER:
		//	return setPerms(userData, currentUserPerms, TESTER_APPS, additionalPermissionstoMaintain);
		case ADMIN:
			return setPerms(userData, currentUserPerms, ADMIN_APPS(appManPlus.userPermService(), useWorkingCopy), additionalPermissionstoMaintain,
					appManPlus);
		case SECRETARY:
			return setPerms(userData, currentUserPerms, SECRETARY_APPS(appManPlus.userPermService(), useWorkingCopy), additionalPermissionstoMaintain,
					appManPlus);
		case USER_STD:
			return setPerms(userData, currentUserPerms, USER_APPS(appManPlus.userPermService(), useWorkingCopy), additionalPermissionstoMaintain,
					appManPlus);
		//case GUEST:
		//	return setPerms(userData, currentUserPerms, GUEST_APPS, additionalPermissionstoMaintain);
		//case RAW:
		//	return setPerms(userData, currentUserPerms, BASE_APPS, additionalPermissionstoMaintain);
		case DISABLED:
			return setPerms(userData, currentUserPerms, Collections.emptyList(), additionalPermissionstoMaintain,
					appManPlus);
		}
		throw new IllegalStateException("Unknown user status type:"+destinationStatus);		
	}

	public static List<String> setPerms(UserAccount userData, List<String> currentUserPerms,
			Collection<String> require, List<String> additionalPermissionstoMaintain,
			ApplicationManagerPlus appManPlus) {
		List<String> missingPerms = getAdditionalPerms(require, currentUserPerms);
		List<String> toRemovePerms = getAdditionalPerms(currentUserPerms, require);
		toRemovePerms.removeAll(additionalPermissionstoMaintain);
		removePerms(userData, toRemovePerms, appManPlus);
		
		addMissingPerms(userData, missingPerms, appManPlus);
		return missingPerms;
	}
	
	public static void addMissingPerms(UserAccount userData,
			Collection<String> missingPerms, ApplicationManagerPlus appManPlus) {
		for(String misPerm: missingPerms) {
			addBundlePermissionForUser(userData.getName(), misPerm, appManPlus);
		}
	}
	public static void removePerms(UserAccount userData,
			Collection<String> toRemovePerms, ApplicationManagerPlus appManPlus) {
		for(String misPerm: toRemovePerms) {
			removeBundlePermissionForUser(userData.getName(), misPerm, appManPlus);
		}
	}

	/** From smartr-efficiency-admin-multi/UserBuilder
	 * TODO: using static policy instead; see ogema.policy*/
	public static void addBundlePermissionForUser(String userName, String bundleSymbolicName,
			ApplicationManagerPlus appManPlus) {
		//AppID appID = findAppIdForString(appIdString);
		AppPermissionFilter props = new AppPermissionFilter(bundleSymbolicName, "*", "*",
				Version.emptyVersion
						.toString());
						// appID.getOwnerUser(), appID.getOwnerGroup(), appID.getVersion() 
		try {
			appManPlus.permMan().getAccessManager().addPermission(userName, props);
		} catch (Exception e) {
			appManPlus.appMan().getLogger().error("Could not add permissions for user {}",userName,e);
		}
	}
	public static void removeBundlePermissionForUser(String userName, String bundleSymbolicName,
			ApplicationManagerPlus appManPlus) {
		//AppID appID = findAppIdForString(appIdString);
		AppPermissionFilter props = new AppPermissionFilter(bundleSymbolicName, "*", "*",
				Version.emptyVersion
						.toString());
						// appID.getOwnerUser(), appID.getOwnerGroup(), appID.getVersion() 
		try {
			appManPlus.permMan().getAccessManager().removePermission(userName, props);
		} catch (Exception e) {
			appManPlus.appMan().getLogger().error("Could not add permissions for user {}",userName,e);
		}
	}


	/** From remote-user-administration/EditPageInit*/
	public static List<String> getPermissions(String userName, ConditionalPermissionAdmin cpa) {
		List<String> result = new ArrayList<>();
		//Stream<Object> baseList = cpa.newConditionalPermissionUpdate().getConditionalPermissionInfos().stream()
		//		.filter(perm -> appliesToSpecificUser(userName, perm))
		//		.flatMap(perm -> Arrays.stream(perm.getPermissionInfos()));
		Stream<String> secRes = cpa.newConditionalPermissionUpdate().getConditionalPermissionInfos().stream()
				.filter(perm -> appliesToSpecificUser(userName, perm))
				.flatMap(perm -> Arrays.stream(perm.getPermissionInfos()))
				.map(PermissionInfo::getEncoded);
		Object[] arrObj = secRes.toArray();
		//String[] arr = (String[]) secRes.toArray(new String[0]);
		result = new ArrayList<>();
		for(Object obj: arrObj) {
			if(obj instanceof String) {
				String fullStr = (String)obj;
				if(fullStr.startsWith(WEB_ACCESS_PERM_STARTSTRING)) {
					String redStr = fullStr.substring(WEB_ACCESS_PERM_STARTSTRING.length());
					int endIdx = getIndexOfExt(redStr, '"');
					endIdx = Math.min(endIdx, getIndexOfExt(redStr, ','));
					String permStr = redStr.substring(0, endIdx);
					result.add(permStr);
				} else
					result.add(fullStr);
			} else
				result.add(obj.toString());
		}
		//result = Arrays.asList(arr);
		return result;
	}
	
	private static int getIndexOfExt(String str, char ch) {
		int idx = str.indexOf(ch);
		if(idx < 0)
			return Integer.MAX_VALUE;
		else
			return idx;
	}
	
	private static boolean appliesToSpecificUser(final String user, final ConditionalPermissionInfo cpi) {
		return Arrays.stream(cpi.getConditionInfos())
			.filter(cond -> cond.getEncoded().contains("org.osgi.service.condpermadmin.BundleLocationCondition")) // XXX this is ugly
			.filter(cond -> cond.getArgs().length > 0)
			.filter(cond -> Arrays.stream(cond.getArgs())
								.filter(arg -> arg.equals("urp:" + user))
								.findAny().isPresent())
			.findAny().isPresent();
	}
	
	public static List<String> updateUserPermissions(UserStatus destStatus, UserAccount userData, ApplicationManagerPlus appManPlus) {
		UserStatusResult currentStatusRes = getUserStatus(userData, appManPlus, false);
		if(destStatus == null)
			destStatus = currentStatusRes.status;
		//List<String> result = addMissingPerms(object, userPermsFinal, destStatus);
		Object sysAdmin = appManPlus.permMan().getSystemPermissionAdmin();
		String userName = userData.getName();
		appManPlus.permMan().getAccessManager().getSupportedAuthenticators(userName);
		List<String> userPerms = null;
		if(sysAdmin instanceof ConditionalPermissionAdmin) {
			ConditionalPermissionAdmin cpa = (ConditionalPermissionAdmin) sysAdmin;
			userPerms  = UserAdminBaseUtil.getPermissions(userName, cpa);
		}
		return setPerms(userData, userPerms, destStatus, currentStatusRes.addPerms, appManPlus, false);	
	}
	public static List<String> updateUserPermissionsToWorkingStatus(UserAccount userData,
			ApplicationManagerPlus appManPlus) {
		UserStatusResult currentStatusRes = getUserStatus(userData, appManPlus, false);
		UserStatus destStatus = currentStatusRes.status;
		//List<String> result = addMissingPerms(object, userPermsFinal, destStatus);
		Object sysAdmin = appManPlus.permMan().getSystemPermissionAdmin();
		String userName = userData.getName();
		appManPlus.permMan().getAccessManager().getSupportedAuthenticators(userName);
		List<String> userPerms = null;
		if(sysAdmin instanceof ConditionalPermissionAdmin) {
			ConditionalPermissionAdmin cpa = (ConditionalPermissionAdmin) sysAdmin;
			userPerms  = UserAdminBaseUtil.getPermissions(userName, cpa);
		}
		return setPerms(userData, userPerms, destStatus, currentStatusRes.addPerms, appManPlus, true);	
	}
}
