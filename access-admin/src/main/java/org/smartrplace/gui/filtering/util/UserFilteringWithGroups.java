package org.smartrplace.gui.filtering.util;

import org.ogema.internationalization.util.LocaleHelper;
import org.smartrplace.external.accessadmin.AccessAdminController;
import org.smartrplace.external.accessadmin.config.AccessConfigUser;
import org.smartrplace.gui.filtering.GenericFilterOption;

import de.iwes.widgets.api.widgets.WidgetPage;

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
	protected void addOptions() {
	}
	protected void addOptionsLoc() {
		for(AccessConfigUser grp: controller.getUserGroups(false)) {
			String name = grp.name().getValue();
			GenericFilterOption<String> newOption = new SingleUserOption(name);
			addOption(newOption , LocaleHelper.getLabelMap(name));			
		}
		super.addOptions();
	}
}
