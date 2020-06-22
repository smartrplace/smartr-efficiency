package org.smartrplace.gui.filtering.util;

import java.util.ArrayList;
import java.util.List;

import org.ogema.core.administration.UserAccount;
import org.ogema.internationalization.util.LocaleHelper;
import org.smartrplace.external.accessadmin.AccessAdminController;
import org.smartrplace.external.accessadmin.config.AccessConfigUser;
import org.smartrplace.gui.filtering.DualFiltering2Steps;
import org.smartrplace.gui.filtering.GenericFilterFixedGroup;
import org.smartrplace.gui.filtering.GenericFilterOption;
import org.smartrplace.gui.filtering.util.UserFilteringBase.SingleUserOption;

import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;

public class UserFiltering2Steps<T> extends DualFiltering2Steps<String, AccessConfigUser, T> {
	private static final long serialVersionUID = 1L;
	protected final AccessAdminController controller;

	public UserFiltering2Steps(WidgetPage<?> page, String id, OptionSavingMode saveOptionMode, long optionSetUpdateRate,
			boolean addAllOption, AccessAdminController controller) {
		super(page, id, saveOptionMode, optionSetUpdateRate, true);
		this.controller = controller;
	}

	@Override
	protected List<GenericFilterOption<String>> getOptionsDynamic(AccessConfigUser group, OgemaHttpRequest req) {
		List<GenericFilterOption<String>> result = new ArrayList<>();
		for(UserAccount ac: controller.appMan.getAdministrationManager().getAllUsers()) {
			if(!firstDropDown.isInSelection(ac.getName(), req))
				continue;
			GenericFilterOption<String> newOption = new SingleUserOption(ac.getName(), LocaleHelper.getLabelMap(ac.getName()));
			result.add(newOption);
		}
		return result;
	}

	@Override
	protected List<GenericFilterFixedGroup<String, AccessConfigUser>> getGroupOptionsDynamic(OgemaHttpRequest req) {
		List<GenericFilterFixedGroup<String, AccessConfigUser>> result = UserFilteringWithGroups.getOptionsDynamic(controller, req);
		return result;
	}

	@Override
	protected List<AccessConfigUser> getGroups(String object) {
		AccessConfigUser userConfig = controller.getUserConfig(object);
		List<AccessConfigUser> result = new ArrayList<>();
		result.add(userConfig);
		return result ;
	}
	
	@Override
	protected boolean isAttributeSinglePerDestinationObject() {
		return true;
	}
	
	public String getSelectedUser(OgemaHttpRequest req) {
		return ((SingleUserOption)getSelectedItem(req)).getValue();
	}
}
