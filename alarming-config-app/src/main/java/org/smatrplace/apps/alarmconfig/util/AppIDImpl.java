package org.smatrplace.apps.alarmconfig.util;

import java.net.URL;

import javax.servlet.http.HttpSession;

import org.ogema.core.application.AppID;
import org.ogema.core.application.Application;
import org.osgi.framework.Bundle;

public class AppIDImpl implements AppID {
	protected final AppID init;
	protected final String suffix;
	
	public AppIDImpl(AppID init, String suffix) {
		this.init = init;
		this.suffix = suffix;
	}

	@Override
	public String getIDString() {
		return init.getIDString()+"_"+suffix;
	}

	@Override
	public String getLocation() {
		return init.getLocation();
	}

	@Override
	public Bundle getBundle() {
		return init.getBundle();
	}

	@Override
	public Application getApplication() {
		return init.getApplication();
	}

	@Override
	public String getOwnerUser() {
		return init.getOwnerUser();
	}

	@Override
	public String getOwnerGroup() {
		return init.getOwnerGroup();
	}

	@Override
	public String getVersion() {
		return init.getVersion();
	}

	@Override
	public URL getOneTimePasswordInjector(String path, HttpSession ses) {
		return init.getOneTimePasswordInjector(path, ses);
	}

	@Override
	public boolean isActive() {
		return init.isActive();
	}

}
