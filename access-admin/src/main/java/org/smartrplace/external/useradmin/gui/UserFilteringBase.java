package org.smartrplace.external.useradmin.gui;

import java.util.ArrayList;
import java.util.List;

import org.ogema.core.administration.UserAccount;
import org.ogema.core.application.ApplicationManager;
import org.ogema.internationalization.util.LocaleHelper;
import org.smartrplace.external.accessadmin.AccessAdminController;
import org.smartrplace.external.accessadmin.config.AccessConfigUser;
import org.smartrplace.gui.filtering.GenericFilterOption;
import org.smartrplace.gui.filtering.SingleFiltering;
import org.smartrplace.gui.filtering.util.UserFiltering2Steps.SingleUserOption;

import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;

public abstract class UserFilteringBase<T> extends SingleFiltering<String, T> {
	private static final long serialVersionUID = 1L;
	//TODO: Add this as constructor parameter
	public static final long UPDATE_RATE = 5000;
	
	protected final ApplicationManager appMan;
	protected final AccessAdminController controller;
	
	public UserFilteringBase(WidgetPage<?> page, String id, OptionSavingMode saveOptionMode,
			long optionSetUpdateRate, AccessAdminController controller) {
		super(page, id, saveOptionMode, optionSetUpdateRate, false);
		this.controller = controller;
		this.appMan = controller.appMan;
	}

	@Override
	protected boolean isAttributeSinglePerDestinationObject() {
		return true;
	}

	public AccessConfigUser getSelectedUser(OgemaHttpRequest req) {
		GenericFilterOption<String> selected = getSelectedItem(req);
		String userName = ((SingleUserOption)selected).getValue();
		AccessConfigUser userConfig = controller.getUserConfig(userName);
		return userConfig;
	}
	
	@Override
	protected long getFrameworkTime() {
		return appMan.getFrameworkTime();
	}
	
	@Override
	protected List<GenericFilterOption<String>> getOptionsDynamic(OgemaHttpRequest req) {
		List<GenericFilterOption<String>> result = new ArrayList<>();
		for(UserAccount ac: controller.getAllNaturalUsers(req)) { //appMan.getAdministrationManager().getAllUsers()) {
			GenericFilterOption<String> newOption = new SingleUserOption(ac.getName(), LocaleHelper.getLabelMap(ac.getName()));
			result.add(newOption);
		}
		return result;
	}
	
}
