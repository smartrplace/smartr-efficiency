package org.ogema.tools.app.createuser;

import org.ogema.accesscontrol.PermissionManager;
import org.ogema.core.administration.UserAccount;
import org.ogema.core.application.ApplicationManager;
import org.ogema.model.user.NaturalPerson;
import org.ogema.tools.app.useradmin.config.UserAdminData;
import org.smartrplace.widget.extensions.GUIUtilHelper;

import de.iwes.util.resource.ValueResourceHelper;

public class UserBuilder {
	final ApplicationManager appMan;
	//private final PermissionManager permissionManager;
	private UserAdminData ownConfigData() {
		return appMan.getResourceAccess().getResource("userAdminData");
	}
	
	public UserBuilder(ApplicationManager appMan, PermissionManager permissionManager) {
		this.appMan = appMan;
		//this.permissionManager = permissionManager;
	}
	public UserBuilder(ApplicationManager appMan) {
		this(appMan, null);
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
		return addUserAndInit(userName, pw, doInituserPermissions, true);
	}
	public UserAccount addUserAndInit(String userName, String pw, boolean doInituserPermissions, boolean isNatural) {
		//if(appMan.getAdministrationManager().getUser(userName) != null) throw new IllegalStateException("User name "+userName+" already exists!");
		//if(!pw.equals(userName)) throw new UnsupportedOperationException("Change password does not work for default admin");
		UserAccount account = appMan.getAdministrationManager().createUserAccount(userName, isNatural);
		try {
			Thread.sleep(1);
		} catch (InterruptedException e) {
		}
		if(!pw.equals(userName))
			account.setNewPassword(userName, pw);
		//if(doInituserPermissions)
		//	initUserPermissions();
		return account;
	}
	
	public boolean enableEmailSending() {
		return (ownConfigData() != null) && (ownConfigData().enableInviteMessagesForUserCreation().getValue() > 0);
	}
	
	public void notifyEmailInvitationSentOut(String userName) {
		NaturalPerson userData = getOrCreateNaturalUserData(userName, ownConfigData());
		ValueResourceHelper.setCreate(userData.inviteSentOut(), true);		
	}
	
    public static NaturalPerson getOrCreateNaturalUserData(String userName, UserAdminData ownConfigData) {
    	NaturalPerson userRes = GUIUtilHelper.getOrCreateUserPropertyResource(userName, ownConfigData.userData()); 
    	return userRes;
    }
}
