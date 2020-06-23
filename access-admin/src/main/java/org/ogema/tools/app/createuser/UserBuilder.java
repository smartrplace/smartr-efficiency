package org.ogema.tools.app.createuser;

import org.ogema.accesscontrol.AppPermissionFilter;
import org.ogema.accesscontrol.PermissionManager;
import org.ogema.core.administration.UserAccount;
import org.ogema.core.application.ApplicationManager;
import org.osgi.framework.Version;

public class UserBuilder {
	private final ApplicationManager appMan;
	private final PermissionManager permissionManager;
	
	public UserBuilder(ApplicationManager appMan, PermissionManager permissionManager) {
		this.appMan = appMan;
		this.permissionManager = permissionManager;
	}


	public UserAccount addUser(String userName, String pw, boolean isAnonymous) {
		return addUser(userName, pw, isAnonymous, true);
	}
	public UserAccount addUser(String userName, String pw, boolean isAnonymous, boolean doInituserPermissions) {
		return addUserAndInit(userName, pw, doInituserPermissions);
	}	
	public UserAccount addUser(String userName, String pw) {
		return addUser(userName, pw, true);
	}
	public UserAccount addUserAndInit(String userName, String pw, boolean doInituserPermissions) {
		//if(appMan.getAdministrationManager().getUser(userName) != null) throw new IllegalStateException("User name "+userName+" already exists!");
		//if(!pw.equals(userName)) throw new UnsupportedOperationException("Change password does not work for default admin");
		UserAccount account = appMan.getAdministrationManager().createUserAccount(userName, true);
		if(!pw.equals(userName)) account.setNewPassword(userName, pw);
		//if(doInituserPermissions)
		//	initUserPermissions();
		return account;
	}
	
	// using static policy instead; see ogema.policy
	private void addBundlePermissionForUser(String userName, String bundleSymbolicName) {
		//AppID appID = findAppIdForString(appIdString);
		AppPermissionFilter props = new AppPermissionFilter(bundleSymbolicName, "*", "*",
				Version.emptyVersion
						.toString());
						// appID.getOwnerUser(), appID.getOwnerGroup(), appID.getVersion() 
		try {
			permissionManager.getAccessManager().addPermission(userName, props);
		} catch (Exception e) {
			appMan.getLogger().error("Could not add permissions for user {}",userName,e);
		}
	}

	/*private void addAllBundlePermissions(String userName) {
		addBundlePermissionForUser(userName, "de.iwes.widgets.ogema-js-bundle");
		addBundlePermissionForUser(userName, "org.ogema.ref-impl.framework-gui");
		addBundlePermissionForUser(userName, "de.iwes.widgets.widget-experimental");
		addBundlePermissionForUser(userName, "de.iwes.widgets.widget-collection");
		addBundlePermissionForUser(userName, "org.smartrplace.apps.smartr-efficiency-admin-multi");
	}
	private void initUserPermissions() {
		// it may take some time until all user bundles are available
		// if an exception is thrown nevertheless, it is caught downstream
		try {
			Thread.sleep(5000);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			return;
		}
		List<UserAccount> allAccounts = appMan.getAdministrationManager().getAllUsers();
		for(UserAccount acc: allAccounts) {
			if (acc.getName().equals("master") || acc.getName().equals("rest")) 
				continue;
			addAllBundlePermissions(acc.getName()); 
		}
	}*/

}
