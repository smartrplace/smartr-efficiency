package org.smartrplace.gui.filtering.util;

import java.util.ArrayList;
import java.util.List;

import org.ogema.internationalization.util.LocaleHelper;
import org.smartrplace.external.accessadmin.AccessAdminController;
import org.smartrplace.external.accessadmin.config.AccessConfigUser;
import org.smartrplace.gui.filtering.GenericFilterFixedGroup;
import org.smartrplace.gui.filtering.GenericFilterOption;

import de.iwes.util.resource.ResourceHelper;
import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;

public class UserFilteringWithGroups<T> extends UserFilteringBase<T> {
	private static final long serialVersionUID = 1L;
	protected final AccessAdminController controller;

	public UserFilteringWithGroups(WidgetPage<?> page, String id, OptionSavingMode saveOptionMode,
			long optionSetUpdateRate, AccessAdminController controller) {
		super(page, id, saveOptionMode, optionSetUpdateRate, controller.appMan);
		this.controller = controller;
	}


	@Override
	protected List<GenericFilterOption<String>> getOptionsDynamic(OgemaHttpRequest req) {
		@SuppressWarnings({ "unchecked", "rawtypes" })
		List<GenericFilterOption<String>> result = (List<GenericFilterOption<String>>)(List)getOptionsDynamic(controller, req);
		result.addAll(super.getOptionsDynamic(req));
		return result;
	}
	public static List<GenericFilterFixedGroup<String, AccessConfigUser>> getOptionsDynamic(AccessAdminController controller, OgemaHttpRequest req) {
		List<GenericFilterFixedGroup<String, AccessConfigUser>> result = new ArrayList<>();
		for(AccessConfigUser grp: controller.getUserGroups(false)) {
			String name = grp.name().getValue();
			GenericFilterFixedGroup<String, AccessConfigUser> newOption = new GenericFilterFixedGroup<String, AccessConfigUser>(
					grp, LocaleHelper.getLabelMap(name)) {

				@Override
				public boolean isInSelection(String object, OgemaHttpRequest req) {
					AccessConfigUser userConfig = controller.getUserConfig(object);
					return ResourceHelper.containsLocation(userConfig.superGroups().getAllElements(), grp);
				}
			};
			result.add(newOption);			
		}
		return result;
	}
}
