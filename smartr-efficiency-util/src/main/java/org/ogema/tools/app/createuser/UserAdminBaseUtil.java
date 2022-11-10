package org.ogema.tools.app.createuser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import org.ogema.accessadmin.api.ApplicationManagerPlus;
import org.ogema.accessadmin.api.UserPermissionService;
import org.ogema.accessadmin.api.UserPermissionService.UserStatusResult;
import org.ogema.accessadmin.api.UserStatus;
import org.ogema.accesscontrol.AppPermissionFilter;
import org.ogema.accesscontrol.PermissionManager;
import org.ogema.core.administration.UserAccount;
import org.ogema.core.application.ApplicationManager;
import org.ogema.model.user.NaturalPerson;
import org.ogema.tools.app.useradmin.config.UserAdminData;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Version;
import org.osgi.service.condpermadmin.BundleLocationCondition;
import org.osgi.service.condpermadmin.Condition;
import org.osgi.service.condpermadmin.ConditionInfo;
import org.osgi.service.condpermadmin.ConditionalPermissionAdmin;
import org.osgi.service.condpermadmin.ConditionalPermissionInfo;
import org.osgi.service.condpermadmin.ConditionalPermissionUpdate;
import org.osgi.service.permissionadmin.PermissionInfo;
import org.smartrplace.external.accessadmin.config.AccessAdminConfig;
import org.smartrplace.external.accessadmin.config.AccessConfigUser;
import org.smartrplace.widget.extensions.GUIUtilHelper;

import de.iwes.util.format.StringFormatHelper;
import de.iwes.util.resource.ValueResourceHelper;
import de.iwes.util.resourcelist.ResourceListHelper;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;

public class UserAdminBaseUtil {
	protected static final String WEB_ACCESS_PERM_STARTSTRING = "(org.ogema.accesscontrol.WebAccessPermission \"name=";
	public static Collection<String> BASE_APPS = Arrays.asList(new String[]{"org.ogema.widgets.ogema-js-bundle",
		"org.ogema.ref-impl.framework-gui",
		"org.ogema.widgets.widget-experimental",
		"org.ogema.widgets.widget-collection",
		"org.smartrplace.internal.user-invitation"});
	public static Collection<String> GUEST_APPS;
	static {
		GUEST_APPS = new ArrayList<String>(BASE_APPS);
		GUEST_APPS.add("org.smartrplace.apps.smartrplace-heatcontrol-servlet");
		GUEST_APPS.add("org.smartrplace.apps.heatcontrol-frontend");
		GUEST_APPS.add("org.smartrplace.apps.react.roomcontrol-we");

		//GUEST_APPS.add("org.smartrplace.apps.react.roomcontrol-we");
		//GUEST_APPS.add("org.smartrplace.apps.react.roomcontrol-2d");
		if(Boolean.getBoolean("org.ogema.apps.overview.usermgmt.centralonly"))
			GUEST_APPS.add("org.smartrplace.apps.smartrcockpit-link");
		if(Boolean.getBoolean("org.ogema.apps.overview.usermgmt.showsavings"))
			GUEST_APPS.add("org.smartrplace.apps.smartrcop-savings-link");
		if(Boolean.getBoolean("org.ogema.apps.overview.usermgmt.showcustomerdoc"))
			GUEST_APPS.add("org.smartrplace.apps.smartrcop-customerdoc-link");
	}

	public static Collection<String> DISPLAY_APPS(UserPermissionService userPermService, boolean useWorkingCopy) {
		return getPermissionsCoordinates(UserStatus.DISPLAY, userPermService, useWorkingCopy);
		
	}

	protected static void addGeneralUserApps(Collection<String> result) {
		result.add("org.smartrplace.apps.overview-src");		
	}
	
	public static Collection<String> USER_APPS(UserPermissionService userPermService, boolean useWorkingCopy) {
		Collection<String> result = getPermissionsCoordinates(UserStatus.USER_STD, userPermService, useWorkingCopy);
		addGeneralUserApps(result);
		return result;
	}

	public static Collection<String> SECRETARY_APPS(UserPermissionService userPermService, boolean useWorkingCopy) {
		Collection<String> result = getPermissionsCoordinates(UserStatus.SECRETARY, userPermService, useWorkingCopy);
		addGeneralUserApps(result);
		//Just to identify the user level, has no GUI
		result.add("org.smartrplace.api.smartr-efficiency-api");
		return result;		
	}

	public static Collection<String> ADMIN_APPS(UserPermissionService userPermService, boolean useWorkingCopy) {
		Collection<String> result = getPermissionsCoordinates(UserStatus.ADMIN, userPermService, useWorkingCopy);
		addGeneralUserApps(result);
		//Just to identify the user level, has no GUI
		result.add("org.smartrplace.apps.smartr-efficiency-util");
		return result;
	}
	
	public static Collection<String> SUPERADMIN_APPS(UserPermissionService userPermService, boolean useWorkingCopy) {
		Collection<String> result = getPermissionsCoordinates(UserStatus.SUPERADMIN, userPermService, useWorkingCopy);
		addGeneralUserApps(result);
		result.add("org.ogema.ref-impl.framework-administration");
		result.add("org.ogema.messaging.message-settings");
		result.add("org.ogema.apps.room-link");
		result.add("com.example.app.evaluation-offline-control");
		result.add("org.ogema.tools.log-transfer-control-sp");
		result.add("org.smartrplace.apps.srcmon-app-expert");
		return result;
		
	};
	protected static Collection<String> getPermissionsCoordinates(UserStatus status,
			UserPermissionService userPermService, boolean useWorkingCopy) {
		Set<String> result = new HashSet<>(GUEST_APPS); //new ArrayList<>(GUEST_APPS);
		for(String permType: UserPermissionService.APP_ACCESS_PERMISSIONS_ALL) {
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
				result.add("org.ogema.apps.grafana-logging-sp");
				break;
			case UserPermissionService.ALARMING:
				result.add("org.smartrplace.apps.alarming-config-app");
				result.add("org.ogema.apps.window-opened-sp");
				result.add("de.iee.ogema.batterystatemonitoring");
				/*result.add("org.smartrplace.internal.message-reader-v2");
				result.add("org.ogema.messaging.message-forwarding");
				result.add("org.ogema.messaging.message-settings");
				result.add("org.smartrplace.apps.smartr-efficiency-fimon");
				result.add("org.smartrplace.apps.smartr-efficiency-admin");*/
				break;
			case UserPermissionService.USER_MANAGEMENT:
				if(Boolean.getBoolean("org.ogema.apps.overview.usermgmt.viacms"))
					result.add("org.smartrplace.apps.smartrcop-cms-link");
				else
					result.add("org.ogema.tools.user-administration");
				break;
			case UserPermissionService.INSTALLATION_SETUP:
				result.add("org.smartrplace.apps.hardware-installation");
				//result.add("org.smartrplace.drivers.bacnet-ogema-sp-gui");
				result.add("org.ogema.tools.modbus-sever-viewer");
				//custom apps
				result.add("org.smartrplace.tools.smartfactory-kpi");				
				result.add("org.smartrplace.apps.react.smartfactoryman");				
				result.add("org.smartrplace.apps.smartrcockpit-link");
				break;
			case UserPermissionService.GROUP_AND_PERMISSION_MANAGEMENT:
				//result.add("org.smartrplace.apps.smartrplace-heatcontrol-v2");
				result.add("org.smartrplace.apps.access-admin");
				break;
			case UserPermissionService.APPSTORE:
				result.add("org.smartrplace.internal.appstore-gui");
				// apps in test mode
				//result.add("org.smartrplace.apps.react.roomcontrol-we");
				//result.add("org.smartrplace.apps.react.roomcontrol-2d");
				break;
			case UserPermissionService.BACNET:
				result.add("org.smartrplace.drivers.bacnet-ogema-sp-gui");
				break;
			case UserPermissionService.DASHBOARD_GENERAL:
				result.add("org.smartrplace.apps.smartrcockpit-link");
				break;
			case UserPermissionService.DASHBOARD_SAVINGS:
				result.add("org.smartrplace.apps.smartrcop-savings-link");
				break;
			case UserPermissionService.DASHBOARD_CUSTOMERDOC:
				result.add("org.smartrplace.apps.smartrcop-customerdoc-link");
				break;
			case UserPermissionService.ROOMCONTROL_WE:
				result.add("org.smartrplace.apps.react.roomcontrol-we");
				break;
			case UserPermissionService.ROOMCONTROL_2D:
				result.add("org.smartrplace.apps.react.roomcontrol-2d");
				break;
			case UserPermissionService.MODBUS_SERVER:
				result.add("org.ogema.tools.modbus-sever-viewer");
				break;
			case UserPermissionService.MONTORING_CHART_EXPERT:
				result.add("org.smartrplace.apps.srcmon-app-expert");
				break;
			case UserPermissionService.RESOURCE_VIEW:
				result.add("org.ogema.ref-impl.framework-administration");
				break;
			}
		}
		return result ;
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

	public static UserStatusResult getUserStatus(String userName, ApplicationManagerPlus appManPlus) {
		try {
			UserAccount userAccount = appManPlus.appMan().getAdministrationManager().getUser(userName);
			UserStatusResult status = UserAdminBaseUtil.getUserStatus(userAccount, appManPlus, false);
			return status;
		} catch(NullPointerException e) {
			return null;
		}
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
		if(hasAllPerms(appPermissions, DISPLAY_APPS(userPermService, useWorkingCopy)))
			return UserStatus.DISPLAY;
		//if(hasAllPerms(appPermissions, GUEST_APPS))
		//	return UserStatus.GUEST;
		if(hasAllPerms(appPermissions, BASE_APPS))
			return UserStatus.PUBLIC;
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
		case DISPLAY:
			return getAdditionalPerms(appPermissions, DISPLAY_APPS(userPermService, useWorkingCopy));
		//case GUEST:
		//	return getAdditionalPerms(appPermissions, GUEST_APPS);
		//case RAW:
		//	return getAdditionalPerms(appPermissions, BASE_APPS);
		case PUBLIC:
			return getAdditionalPerms(appPermissions, BASE_APPS);
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
			UserStatus destinationStatus,
			ApplicationManagerPlus appManPlus, boolean useWorkingCopy) {
		switch(destinationStatus) {
		case SUPERADMIN:
			return setPerms(userData, currentUserPerms, SUPERADMIN_APPS(appManPlus.userPermService(), useWorkingCopy),
					appManPlus);
		//case TESTER:
		//	return setPerms(userData, currentUserPerms, TESTER_APPS, additionalPermissionstoMaintain);
		case ADMIN:
			return setPerms(userData, currentUserPerms, ADMIN_APPS(appManPlus.userPermService(), useWorkingCopy),
					appManPlus);
		case SECRETARY:
			return setPerms(userData, currentUserPerms, SECRETARY_APPS(appManPlus.userPermService(), useWorkingCopy),
					appManPlus);
		case USER_STD:
			return setPerms(userData, currentUserPerms, USER_APPS(appManPlus.userPermService(), useWorkingCopy),
					appManPlus);
		case DISPLAY:
			return setPerms(userData, currentUserPerms, DISPLAY_APPS(appManPlus.userPermService(), useWorkingCopy),
					appManPlus);
		//case GUEST:
		//	return setPerms(userData, currentUserPerms, GUEST_APPS, additionalPermissionstoMaintain);
		//case RAW:
		//	return setPerms(userData, currentUserPerms, BASE_APPS, additionalPermissionstoMaintain);
		case PUBLIC:
			return setPerms(userData, currentUserPerms, BASE_APPS,
					appManPlus);
		case DISABLED:
			return setPerms(userData, currentUserPerms, Collections.emptyList(),
					appManPlus);
		}
		throw new IllegalStateException("Unknown user status type:"+destinationStatus);		
	}

	public static List<String> setPerms(UserAccount userData, List<String> currentUserPerms,
			Collection<String> require,
			ApplicationManagerPlus appManPlus) {
		List<String> missingPerms = getAdditionalPerms(require, currentUserPerms);
		List<String> toRemovePerms = getAdditionalPerms(currentUserPerms, require);
System.out.println("CurrentPerms:"+StringFormatHelper.getListToPrint(currentUserPerms));
System.out.println("RequiredPerms:"+StringFormatHelper.getListToPrint(require));
System.out.println("Missing["+(missingPerms!=null?""+missingPerms.size():"!null!")+"]:"+StringFormatHelper.getListToPrint(missingPerms));
System.out.println("ToRemoveAll:"+StringFormatHelper.getListToPrint(toRemovePerms));

//		toRemovePerms.removeAll(additionalPermissionstoMaintain);
//System.out.println("ToRemoveReally:"+StringFormatHelper.getListToPrint(toRemovePerms));
		removePerms(userData, toRemovePerms, appManPlus);
		
		addMissingPerms(userData, missingPerms, appManPlus);
		return missingPerms;
	}
	
/*	public static List<String> setPerms(UserAccount userData, List<String> currentUserPerms,
			Collection<String> require, List<String> additionalPermissionstoMaintain,
			ApplicationManagerPlus appManPlus) {
		List<String> missingPerms = getAdditionalPerms(require, currentUserPerms);
		List<String> toRemovePerms = getAdditionalPerms(currentUserPerms, require);
System.out.println("CurrentPerms:"+StringFormatHelper.getListToPrint(currentUserPerms));
System.out.println("RequiredPerms:"+StringFormatHelper.getListToPrint(require));
System.out.println("Missing["+(missingPerms!=null?""+missingPerms.size():"!null!")+"]:"+StringFormatHelper.getListToPrint(missingPerms));
System.out.println("ToRemoveAll:"+StringFormatHelper.getListToPrint(toRemovePerms));

		toRemovePerms.removeAll(additionalPermissionstoMaintain);
System.out.println("ToRemoveReally:"+StringFormatHelper.getListToPrint(toRemovePerms));
		removePerms(userData, toRemovePerms, appManPlus);
		
		addMissingPerms(userData, missingPerms, appManPlus);
		return missingPerms;
	}*/

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
System.out.println("Removing app: "+bundleSymbolicName+ "::"+props.getAppname()+"::"+props.getFilterString());
			appManPlus.permMan().getAccessManager().removePermission(userName, props);
		} catch (Exception e) {
			appManPlus.appMan().getLogger().error("Could not add permissions for user {}",userName,e);
		}
System.out.println("Finished removing app: "+bundleSymbolicName);
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
//System.out.println("In UserAdminBaseUtil: Found "+arrObj.length+" user perms for "+userName);
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
	
	public static List<ConditionalPermissionInfo> userPermissions2(String user, ConditionalPermissionAdmin cpa,
			BundleContext ctx) {
		final Bundle b = ctx.getBundle("urp:" + user);
		if (b == null) {
			System.out.println("User " + user + " not found");
			return null;
		}
		return bundlePermissionsList(cpa, b);
	}
	
	/** Copied from security/ShellCommands*/
    private static List<ConditionalPermissionInfo> bundlePermissionsList(ConditionalPermissionAdmin cpa, Bundle b) {
        final ConditionalPermissionUpdate cpu = cpa.newConditionalPermissionUpdate();
        List<ConditionalPermissionInfo> l = new ArrayList<>();
        for (ConditionalPermissionInfo cpi : cpu.getConditionalPermissionInfos()) {
            ConditionInfo[] cis = cpi.getConditionInfos();
            for (ConditionInfo ci : cis) {
                if (ci.getType().equals("org.osgi.service.condpermadmin.BundleLocationCondition")) {
                    Condition blc = BundleLocationCondition.getCondition(b, ci);
                    if (blc.isSatisfied()) {
                        l.add(cpi);
                    }
                }
            }
            if (cis.length == 0) {
                l.add(cpi);
            }
        }
        return l;
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
		return setPerms(userData, userPerms, destStatus, appManPlus, false);	
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
		return setPerms(userData, userPerms, destStatus, appManPlus, true);	
	}
	
	public static void initAcc(AccessAdminConfig appConfigData) {
		for(UserStatus status: UserStatus.values()) {
			AccessConfigUser userGrp = ResourceListHelper.getOrCreateNamedElement(UserStatus.getLabel(status, null),
					appConfigData.userPermissions());
			ValueResourceHelper.setCreate(userGrp.isGroup(), 2);
		}
	}

	public static List<UserAccount> getNaturalUsers(ApplicationManagerPlus appManPlus,
			OgemaHttpRequest req) {
		return getNaturalUsers(appManPlus.appMan(), appManPlus.permMan(), req);
	}
	public static List<UserAccount> getNaturalUsers(ApplicationManager appMan, PermissionManager permMan,
			OgemaHttpRequest req) {
		final boolean showMasterUsers;
		if(req == null)
			showMasterUsers = true;
		else {
			String userLoggedIn = GUIUtilHelper.getUserLoggedIn(req);
			showMasterUsers = userLoggedIn.equals("master");
		}
		List<UserAccount> result = new ArrayList<UserAccount>();
		for(UserAccount ac: appMan.getAdministrationManager().getAllUsers()) {
			if((ac.getName().equals("master")||ac.getName().equals("guest2")||ac.getName().equals("guest")) && (!showMasterUsers))
				continue;
			if(permMan.getAccessManager().isNatural(ac.getName())) {
				result .add(ac);
			}
		}
		return result;
	}
	public static List<UserAccount> getRESTUsers(ApplicationManagerPlus appManPlus) {
		return getRESTUsers(appManPlus.appMan(), appManPlus.permMan());
	}
	public static List<UserAccount> getRESTUsers(ApplicationManager appMan, PermissionManager permMan) {
		//String userLoggedIn = GUIUtilHelper.getUserLoggedIn(req);
		List<UserAccount> result = new ArrayList<UserAccount>();
		for(UserAccount ac: appMan.getAdministrationManager().getAllUsers()) {
			if(!permMan.getAccessManager().isNatural(ac.getName())) {
				result .add(ac);
			}
		}
		return result;
	}
	
    public static void updateUserStatusInResources(UserAdminData ownConfigData,
    		ApplicationManagerPlus appManPlus) {
    	ownConfigData.userData().create();
    	for(UserAccount ac: UserAdminBaseUtil.getNaturalUsers(appManPlus.appMan(),
    			appManPlus.permMan(), null)) {
			NaturalPerson userRes = ResourceListHelper.getOrCreateNamedElementFlex(ac.getName(), ownConfigData.userData());
			UserStatusResult status = UserAdminBaseUtil.getUserStatus(ac, appManPlus, false);
			ValueResourceHelper.setCreate(userRes.userRole(), status.status.toString());
		}
    	ownConfigData.restUserData().create();
    	for(UserAccount ac: UserAdminBaseUtil.getRESTUsers(appManPlus.appMan(), appManPlus.permMan())) {
			ResourceListHelper.getOrCreateNamedElementFlex(ac.getName(), ownConfigData.restUserData());
		}  	
    }
    
    public static List<String> setUserRole(UserAccount userAccount, UserStatus destStatus,
    		UserAdminData ownConfigData, ApplicationManagerPlus appManPlus, ConditionalPermissionAdmin cpa) {
    	List<String> currentUserPerms = getPermissions(userAccount.getName(), cpa);
    	return setUserRole(userAccount, destStatus, ownConfigData, appManPlus, currentUserPerms);
    }
    
	public static List<String> setUserRole(UserAccount userAccount, UserStatus destStatus, UserAdminData ownConfigData,
			ApplicationManagerPlus appManPlus, List<String> currentUserPerms) {
    	List<String> result = UserAdminBaseUtil.setPerms(userAccount, currentUserPerms, destStatus,
				appManPlus, false);
		updateUserStatusInResources(ownConfigData, appManPlus);
    	return result;
    }
    
    /** Set user role and access permissions
     * 
     * @param userAccount for which role shall be changed
     * @param destStatus new status of user account
     * @param ownConfigData the general UserAdminData resource (usually toplevel)
     * @param appMan
     * @param permMan
     * @param userPermService accessible as OSGi service
     * @param cpa
     * @return new user permissions set for the new role
     */
    public static List<String> setUserRole(UserAccount userAccount, UserStatus destStatus,
    		UserAdminData ownConfigData,
    		ApplicationManager appMan, PermissionManager permMan, UserPermissionService userPermService,
    		ConditionalPermissionAdmin cpa) {
    	ApplicationManagerPlus appManPlus = new ApplicationManagerPlus(appMan);
    	appManPlus.setPermMan(permMan);
    	appManPlus.setUserPermService(userPermService);
    	return setUserRole(userAccount, destStatus, ownConfigData, appManPlus, cpa);
    }
}
