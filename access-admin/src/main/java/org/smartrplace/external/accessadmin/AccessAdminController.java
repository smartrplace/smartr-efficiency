package org.smartrplace.external.accessadmin;

import java.util.ArrayList;
import java.util.List;

import org.ogema.accessadmin.api.ApplicationManagerPlus;
import org.ogema.accessadmin.api.SubcustomerUtil;
import org.ogema.core.administration.UserAccount;
import org.ogema.core.application.ApplicationManager;
import org.ogema.core.logging.OgemaLogger;
import org.ogema.core.model.ResourceList;
import org.ogema.model.locations.BuildingPropertyUnit;
import org.ogema.model.locations.Room;
import org.ogema.tools.app.createuser.UserAdminBaseUtil;
import org.smartrplace.apps.hw.install.config.HardwareInstallConfig;
import org.smartrplace.external.accessadmin.config.AccessAdminConfig;
import org.smartrplace.external.accessadmin.config.AccessConfigUser;
import org.smartrplace.external.accessadmin.gui.MainPage;
import org.smartrplace.external.accessadmin.gui.RoomConfigPage;
import org.smartrplace.external.accessadmin.gui.RoomSetupPage;
import org.smartrplace.external.accessadmin.gui.UserGroupPermissionPage;
import org.smartrplace.external.accessadmin.gui.UserRoomGroupPermissionPage2;
import org.smartrplace.external.accessadmin.gui.UserRoomPermissionPage;
import org.smartrplace.external.accessadmin.gui.UserSetupPage;
import org.smartrplace.external.accessadmin.gui.UserStatusPermissionPage;
import org.smartrplace.gui.filtering.util.UserFiltering2Steps;

import de.iwes.util.logconfig.LogHelper;
import de.iwes.widgets.api.widgets.WidgetApp;
import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;

// here the controller logic is implemented
public class AccessAdminController {

	public OgemaLogger log;
    public ApplicationManager appMan;
    /** This will not be available in the constructor*/
    public UserPermissionServiceImpl userPermService;
    
	public AccessAdminConfig appConfigData;
	public final HardwareInstallConfig hwInstallConfig;
	public AccessAdminApp accessAdminApp;
    public final ApplicationManagerPlus appManPlus;
	
	public MainPage mainPage;
	public UserRoomPermissionPage userRoomPermPage;
	public UserRoomGroupPermissionPage2 userRoomGroupPermPage2;
	public RoomConfigPage roomConfigPage;
	public UserSetupPage userSetupPage;
	//public UserConfigPage userConfigPage;
	public UserGroupPermissionPage userGroupPermPage;
	WidgetApp widgetApp;

	public final ResourceList<AccessConfigUser> userPerms;
	public final ResourceList<BuildingPropertyUnit> roomGroups;
	public final UserStatusPermissionPage userStatusPage;
	public final RoomSetupPage roomSetupPage;
	//public final UserSetupPage userSetupPage;
	
	public AccessAdminController(ApplicationManager appMan, AccessAdminApp initApp) {
		this.appMan = appMan;
		this.log = appMan.getLogger();
		this.accessAdminApp = initApp;
		this.appManPlus = new ApplicationManagerPlus(appMan);
		appManPlus.setPermMan(initApp.permMan);
		//appManPlus.setUserPermService(userPermService);
		
		initConfigurationResource();
		hwInstallConfig = appMan.getResourceAccess().getResource("hardwareInstallConfig");
		userPerms = appConfigData.userPermissions();
		roomGroups = appConfigData.roomGroups();

		UserAdminBaseUtil.initAcc(appConfigData);
		
		//mainPage = new MainPage(page, appMan);

		boolean isGw = Boolean.getBoolean("org.smartrplace.app.srcmon.isgateway");
		if(isGw) {
			//WidgetPage<?> pageRes1 = initApp.widgetApp.createWidgetPage("userroomperm.html");
			WidgetPage<?> pageRes10 = initApp.widgetApp.createWidgetPage("roomsetup.html", true);
			roomSetupPage = new RoomSetupPage(pageRes10, this);
			initApp.menu.addEntry("1. Room Attribute Configuration", pageRes10);
			initApp.configMenuConfig(pageRes10.getMenuConfiguration());
	
			WidgetPage<?> pageRes3 = initApp.widgetApp.createWidgetPage("roomconfig.html");
			roomConfigPage = new RoomConfigPage(pageRes3, this);
			initApp.menu.addEntry("2. Room Configuration", pageRes3);
			initApp.configMenuConfig(pageRes3.getMenuConfiguration());
		} else {
			roomSetupPage = null;
			roomConfigPage = null;
		}
		WidgetPage<?> pageRes11 = initApp.widgetApp.createWidgetPage("usersetup.html", !isGw);
		userSetupPage = new UserSetupPage(pageRes11, appConfigData, appMan);
		initApp.menu.addEntry((isGw?"3.":"1.")+" User Attribute Configuration", pageRes11);
		initApp.configMenuConfig(pageRes11.getMenuConfiguration());

		if(isGw) {
			WidgetPage<?> pageRes2 = initApp.widgetApp.createWidgetPage("userroomperm.html");
			userRoomGroupPermPage2 = new UserRoomGroupPermissionPage2(pageRes2, this, false);
			initApp.menu.addEntry("4. User - Room Group Mapping", pageRes2);
			initApp.configMenuConfig(pageRes2.getMenuConfiguration());
		} else
			userRoomGroupPermPage2 = null;
		//WidgetPage<?> pageRes2V = initApp.widgetApp.createWidgetPage("userroompermv1.html");
		//userRoomGroupPermPage = new UserRoomGroupPermissionPage(pageRes2V, this);
		//initApp.menu.addEntry("4. User - Room Group Mapping (V1)", pageRes2V);
		//initApp.configMenuConfig(pageRes2V.getMenuConfiguration());

		WidgetPage<?> pageRes6 = initApp.widgetApp.createWidgetPage("userstatus.html");
		userStatusPage = new UserStatusPermissionPage(pageRes6, this);
		initApp.menu.addEntry((isGw?"5.":"2.")+" User App Mapping", pageRes6);
		initApp.configMenuConfig(pageRes6.getMenuConfiguration());

		if(Boolean.getBoolean("org.ogema.accessadmin.api.isappstore")) {
			WidgetPage<?> pageRes5 = initApp.widgetApp.createWidgetPage("usergrouperm.html");
			userGroupPermPage = new UserGroupPermissionPage(pageRes5, this);
			initApp.menu.addEntry("6. User Appstore Mapping", pageRes5);
			initApp.configMenuConfig(pageRes5.getMenuConfiguration());
		}

		//WidgetPage<?> pageRes4 = initApp.widgetApp.createWidgetPage("userconfig.html");
		//userConfigPage = new UserConfigPage(pageRes4, this);
		//initApp.menu.addEntry("User Configuration", pageRes4);
		//initApp.configMenuConfig(pageRes4.getMenuConfiguration());

		WidgetPage<?> pageRes1 =  initApp.widgetApp.createWidgetPage("expert.html");//initApp.widgetApp.createWidgetPage("userroomperm.html");
		userRoomPermPage = new UserRoomPermissionPage(pageRes1, appConfigData, appManPlus);
		//initApp.menu.addEntry("Single Room Access Permissions", pageRes1);
		initApp.configMenuConfig(pageRes1.getMenuConfiguration());


		initDemands();

        LogHelper.logStartup(16, appMan);
	}

    /*
     * This app uses a central configuration resource, which is accessed here
     */
    private void initConfigurationResource() {
    	appConfigData = getAppConfigDataInit(appMan);
    }
    public static AccessAdminConfig getAppConfigDataInit(ApplicationManager appMan) {
		//TODO provide Util?
		String name = AccessAdminConfig.class.getSimpleName().substring(0, 1).toLowerCase()+AccessAdminConfig.class.getSimpleName().substring(1);
		AccessAdminConfig appConfigData = appMan.getResourceAccess().getResource(name);
		if (appConfigData != null) { // resource already exists (appears in case of non-clean start)
			appMan.getLogger().debug("{} started with previously-existing config resource", AccessAdminController.class.getName());
		}
		else {
			appConfigData = (AccessAdminConfig) appMan.getResourceManagement().createResource(name, AccessAdminConfig.class);
			appConfigData.name().create();
			//TODO provide different sample, provide documentation in code
			appConfigData.name().setValue("sampleName");
			appConfigData.activate(true);
			appMan.getLogger().debug("{} started with new config resource", AccessAdminController.class.getName());
		}
		return appConfigData;
    }
    
    /*
     * register ResourcePatternDemands. The listeners will be informed about new and disappearing
     * patterns in the OGEMA resource tree
     */
    public void initDemands() {
    }

	public void close() {
	}

	//TODO: Provide user and room group access via service
	public List<AccessConfigUser> getUserGroups(boolean includeNaturalUsers) {
		return getUserGroups(includeNaturalUsers, true);
		/*List<AccessConfigUser> result = new ArrayList<>();
		for(AccessConfigUser user: appConfigData.userPermissions().getAllElements()) {
			if(includeNaturalUsers || (user.isGroup().getValue() > 0))
				result.add(user);
		}
		return result ;*/
	}
	public List<AccessConfigUser> getUserGroups(boolean includeNaturalUsers, boolean includetype2Groups) {
		return SubcustomerUtil.getUserGroups(includeNaturalUsers, includetype2Groups, appConfigData);
	}
	
	public AccessConfigUser getUserConfig(String userName) {
		return UserFiltering2Steps.getUserConfig(userName, appConfigData);
	}
	
	public List<BuildingPropertyUnit> getGroups(Room object) {
		List<BuildingPropertyUnit> result = new ArrayList<>();
		for(BuildingPropertyUnit bu: roomGroups.getAllElements()) {
			if(bu.rooms().contains(object))
				result.add(bu);
		}
		return result ;
	}
	
	public List<AccessConfigUser> getAllGroupsForUser(AccessConfigUser naturalUser) {
		return UserFiltering2Steps.getAllGroupsForUser(naturalUser, appConfigData, userPermService);
	}
	
	public List<UserAccount> getAllNaturalUsers(OgemaHttpRequest req) {
		return UserAdminBaseUtil.getNaturalUsers(appManPlus, req);
	}
}
