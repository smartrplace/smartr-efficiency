package org.smartrplace.gui.filtering.util;

import java.util.ArrayList;
import java.util.List;

import org.ogema.core.administration.UserAccount;
import org.ogema.internationalization.util.LocaleHelper;
import org.smartrplace.external.accessadmin.AccessAdminController;
import org.smartrplace.external.accessadmin.config.AccessConfigUser;
import org.smartrplace.gui.filtering.DualFiltering2Steps;
import org.smartrplace.gui.filtering.GenericFilterFixedSingle;
import org.smartrplace.gui.filtering.GenericFilterOption;
import org.smartrplace.gui.filtering.util.UserFilteringBase.SingleUserOption;

import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;

public class UserFiltering2Steps<T> extends DualFiltering2Steps<String, AccessConfigUser, T> {
	private static final long serialVersionUID = 1L;
	protected final AccessAdminController controller;

	public UserFiltering2Steps(WidgetPage<?> page, String id, OptionSavingMode saveOptionMode, long optionSetUpdateRate,
			AccessAdminController controller) {
		super(page, id, saveOptionMode, optionSetUpdateRate, false);
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
	protected List<GenericFilterFixedSingle<AccessConfigUser>> getGroupOptionsDynamic(OgemaHttpRequest req) {
		List<GenericFilterFixedSingle<AccessConfigUser>> result = getOptionsDynamic2S(controller, req);
		return result;
	}
	
	public static List<GenericFilterFixedSingle<AccessConfigUser>> getOptionsDynamic2S(AccessAdminController controller, OgemaHttpRequest req) {
		List<GenericFilterFixedSingle<AccessConfigUser>> result = new ArrayList<>();
		for(AccessConfigUser grp: controller.getUserGroups(false)) {
			String name = grp.name().getValue();
			GenericFilterFixedSingle<AccessConfigUser> newOption = new GenericFilterFixedSingle<AccessConfigUser>(
					grp, LocaleHelper.getLabelMap(name));  /* {

				@Override
				public boolean isInSelection(AccessConfigUser userConfig, OgemaHttpRequest req) {
					//AccessConfigUser userConfig = controller.getUserConfig(object);
					return userConfig.equalsLocation(grp);
					//return ResourceHelper.containsLocation(userConfig, grp);
				}
			};*/
			result.add(newOption);			
		}
		return result;
	}
	

	@Override
	protected List<AccessConfigUser> getGroups(String object) {
		AccessConfigUser userConfig = controller.getUserConfig(object);
		List<AccessConfigUser> result = new ArrayList<>();
		result.addAll(userConfig.superGroups().getAllElements());
		return result ;
	}
	
	@Override
	protected boolean isAttributeSinglePerDestinationObject() {
		return true;
	}
	
	public String getSelectedUser(OgemaHttpRequest req) {
		GenericFilterOption<String> selected = getSelectedItem(req);
		if(selected == null) {
			onGET(req);
			return getSelectedUser(req);
			//return ((SingleUserOption)getItems(req).get(0)).getValue();
		}
		return ((SingleUserOption)selected).getValue();
	}
	
	@Override
	protected long getFrameworkTime() {
		return controller.appMan.getFrameworkTime();
	}
}
