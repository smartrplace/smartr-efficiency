package org.smartrplace.external.useradmin.gui;

import org.ogema.core.administration.UserAccount;
import org.smartrplace.external.accessadmin.config.AccessConfigUser;

public class UserDataTbl {
	UserAccount userAccount;
	String alternativeUserName;
	public AccessConfigUser accessConfig;

	public UserDataTbl(UserAccount userAccount) {
		this.userAccount = userAccount;
	}

	public UserDataTbl(String onlyUserName) {
		this.userAccount = null;
		alternativeUserName = onlyUserName;
	}

	public String userName() {
		if(userAccount != null)
			return userAccount.getName();
		return alternativeUserName;
	}
}
