package org.smartrplace.external.accessadmin;

import java.util.ArrayList;
import java.util.List;

import org.ogema.accessadmin.api.ApplicationManagerPlus;
import org.ogema.accessadmin.api.util.UserPermissionServiceImpl;
import org.ogema.core.administration.UserAccount;
import org.ogema.core.application.ApplicationManager;
import org.ogema.core.logging.OgemaLogger;
import org.ogema.core.model.ResourceList;
import org.ogema.model.locations.BuildingPropertyUnit;
import org.ogema.model.locations.Room;
import org.smartrplace.external.accessadmin.config.AccessAdminConfig;
import org.smartrplace.external.accessadmin.config.AccessConfigUser;
import org.smartrplace.external.accessadmin.gui.MainPage;
import org.smartrplace.external.accessadmin.gui.RoomConfigPage;
import org.smartrplace.external.accessadmin.gui.UserConfigPage;
import org.smartrplace.external.accessadmin.gui.UserGroupPermissionPage;
import org.smartrplace.external.accessadmin.gui.UserRoomGroupPermissionPage;
import org.smartrplace.external.accessadmin.gui.UserRoomPermissionPage;
import org.smartrplace.external.accessadmin.gui.UserStatusPermissionPage;
import org.smartrplace.gui.filtering.GenericFilterFixedGroup;

import de.iwes.widgets.api.widgets.WidgetApp;
import de.iwes.widgets.api.widgets.WidgetPage;

// here the controller logic is implemented
public class AccessAdminController {

	public OgemaLogger log;
    public ApplicationManager appMan;
    /** This will not be available in the constructor*/
    public UserPermissionServiceImpl userPermService;
    
	public AccessAdminConfig appConfigData;
	public AccessAdminApp accessAdminApp;
    public final ApplicationManagerPlus appManPlus;
	
	public MainPage mainPage;
	public UserRoomPermissionPage userRoomPermPage;
	public UserRoomGroupPermissionPage userRoomGroupPermPage;
	public RoomConfigPage roomConfigPage;
	public UserConfigPage userConfigPage;
	public UserGroupPermissionPage userGroupPermPage;
	WidgetApp widgetApp;

	public final ResourceList<AccessConfigUser> userPerms;
	public final ResourceList<BuildingPropertyUnit> roomGroups;
	public final UserStatusPermissionPage userStatusPage;
	
	public AccessAdminController(ApplicationManager appMan, WidgetPage<?> page, AccessAdminApp initApp) {
		this.appMan = appMan;
		this.log = appMan.getLogger();
		this.accessAdminApp = initApp;
		this.appManPlus = new ApplicationManagerPlus(appMan);
		appManPlus.setPermMan(initApp.permMan);
		//appManPlus.setUserPermService(userPermService);
		
		initConfigurationResource();
		userPerms = appConfigData.userPermissions();
		roomGroups = appConfigData.roomGroups();

		//mainPage = new MainPage(page, appMan);

		//WidgetPage<?> pageRes1 = initApp.widgetApp.createWidgetPage("userroomperm.html");
		WidgetPage<?> pageRes6 = initApp.widgetApp.createWidgetPage("userstatus.html");
		userStatusPage = new UserStatusPermissionPage(pageRes6, this);
		initApp.menu.addEntry("User Type App Access", pageRes6);
		initApp.configMenuConfig(pageRes6.getMenuConfiguration());

		WidgetPage<?> pageRes2 = initApp.widgetApp.createWidgetPage("userroomperm.html");
		userRoomGroupPermPage = new UserRoomGroupPermissionPage(pageRes2, this);
		initApp.menu.addEntry("Room Group Access Permissions", pageRes2);
		initApp.configMenuConfig(pageRes2.getMenuConfiguration());

		WidgetPage<?> pageRes3 = initApp.widgetApp.createWidgetPage("roomconfig.html");
		roomConfigPage = new RoomConfigPage(pageRes3, this);
		initApp.menu.addEntry("Room Group Configuration", pageRes3);
		initApp.configMenuConfig(pageRes3.getMenuConfiguration());

		WidgetPage<?> pageRes5 = initApp.widgetApp.createWidgetPage("usergrouperm.html");
		userGroupPermPage = new UserGroupPermissionPage(pageRes5, this);
		initApp.menu.addEntry("User Appstore Permissions", pageRes5);
		initApp.configMenuConfig(pageRes5.getMenuConfiguration());

		WidgetPage<?> pageRes4 = initApp.widgetApp.createWidgetPage("userconfig.html");
		userConfigPage = new UserConfigPage(pageRes4, this);
		initApp.menu.addEntry("User Attribute Configuration", pageRes4);
		initApp.configMenuConfig(pageRes4.getMenuConfiguration());

		WidgetPage<?> pageRes1 = page; //initApp.widgetApp.createWidgetPage("userroomperm.html");
		userRoomPermPage = new UserRoomPermissionPage(pageRes1, this);
		initApp.menu.addEntry("Single Room Access Permissions", pageRes1);
		initApp.configMenuConfig(pageRes1.getMenuConfiguration());


		initDemands();
	}

    /*
     * This app uses a central configuration resource, which is accessed here
     */
    private void initConfigurationResource() {
		//TODO provide Util?
		String name = AccessAdminConfig.class.getSimpleName().substring(0, 1).toLowerCase()+AccessAdminConfig.class.getSimpleName().substring(1);
		appConfigData = appMan.getResourceAccess().getResource(name);
		if (appConfigData != null) { // resource already exists (appears in case of non-clean start)
			appMan.getLogger().debug("{} started with previously-existing config resource", getClass().getName());
		}
		else {
			appConfigData = (AccessAdminConfig) appMan.getResourceManagement().createResource(name, AccessAdminConfig.class);
			appConfigData.name().create();
			//TODO provide different sample, provide documentation in code
			appConfigData.name().setValue("sampleName");
			appConfigData.activate(true);
			appMan.getLogger().debug("{} started with new config resource", getClass().getName());
		}
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
		List<AccessConfigUser> result = new ArrayList<>();
		for(AccessConfigUser user: appConfigData.userPermissions().getAllElements()) {
			if(!includetype2Groups && (user.isGroup().getValue() == 2))
				continue;
			if(includeNaturalUsers || (user.isGroup().getValue() > 0))
				result.add(user);
		}
		return result ;
	}
	
	public AccessConfigUser getUserConfig(String userName) {
		for(AccessConfigUser user: appConfigData.userPermissions().getAllElements()) {
			if(user.name().getValue().equals(userName))
				return user;
		}
		return null;
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
		List<AccessConfigUser> result = getLevel2GroupsForUser(naturalUser);
		result.addAll(naturalUser.superGroups().getAllElements());
		return result;
	}
	public List<AccessConfigUser> getLevel2GroupsForUser(AccessConfigUser naturalUser) {
		List<AccessConfigUser> result = new ArrayList<>();
		if(naturalUser.isGroup().getValue() > 0)
			return result;
		String userName = naturalUser.name().getValue();
		for(AccessConfigUser grp: appConfigData.userPermissions().getAllElements()) {
			if(grp.isGroup().getValue() < 2)
				continue;
			GenericFilterFixedGroup<String, AccessConfigUser> filter = userPermService.getUserGroupFiler(grp.name().getValue());
			if(filter == null)
				continue;
			if(filter.isInSelection(userName, grp))
				result.add(grp);
		}
		return result ;
	}
	
	public List<UserAccount> getAllNaturalUsers() {
		return getAllNaturalUsers(true);
	}
	public List<UserAccount> getAllNaturalUsers(boolean includeMaster) {
		List<UserAccount> allUsers = appMan.getAdministrationManager().getAllUsers();
		List<UserAccount> result = new ArrayList<>();
		for(UserAccount ac: allUsers) {
			if((!includeMaster) && (ac.getName().equals("master")||ac.getName().equals("guest2")))
				continue;
			if(!appManPlus.permMan().getAccessManager().isNatural(ac.getName()))
				continue;
			result.add(ac);
		}
		return result;
	}
}
