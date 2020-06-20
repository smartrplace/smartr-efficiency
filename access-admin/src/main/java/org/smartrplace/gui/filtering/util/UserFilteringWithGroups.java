package org.smartrplace.gui.filtering.util;

import java.util.ArrayList;
import java.util.List;

import org.ogema.internationalization.util.LocaleHelper;
import org.smartrplace.external.accessadmin.AccessAdminController;
import org.smartrplace.external.accessadmin.config.AccessConfigUser;
import org.smartrplace.gui.filtering.GenericFilterOption;

import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;

public class UserFilteringWithGroups<T> extends UserFilteringBase<T> {
	private static final long serialVersionUID = 1L;
	protected final AccessAdminController controller;

	public UserFilteringWithGroups(WidgetPage<?> page, String id, OptionSavingMode saveOptionMode,
			AccessAdminController controller) {
		super(page, id, saveOptionMode, controller.appMan);
		this.controller = controller;
		addOptionsLoc();
	}


	@Override
	protected List<GenericFilterOption<String>> getOptionsDynamic(OgemaHttpRequest req) {
		List<GenericFilterOption<String>> result = new ArrayList<>();
		for(AccessConfigUser grp: controller.getUserGroups(false)) {
			String name = grp.name().getValue();
			GenericFilterOption<String> newOption = new SingleUserOption(name, LocaleHelper.getLabelMap(name));
			result.add(newOption);			
		}
		result.addAll(super.getOptionsDynamic(req));
		return result;
	}
	
	protected void addOptionsLoc() {
	}
}
