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

public abstract class UserFilteringWithGroups<T> extends UserFilteringBase<T> {
	private static final long serialVersionUID = 1L;

	public UserFilteringWithGroups(WidgetPage<?> page, String id, OptionSavingMode saveOptionMode,
			long optionSetUpdateRate, AccessAdminController controller) {
		super(page, id, saveOptionMode, optionSetUpdateRate, controller);
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
			if(grp.isGroup().getValue() >= 2) {
				GenericFilterFixedGroup<String, AccessConfigUser> filter = controller.userPermService.getUserGroupFiler(name);
				if(filter == null)
					continue;
				result.add(filter);
				continue;
			}
			GenericFilterFixedGroup<String, AccessConfigUser> newOption = new GenericFilterFixedGroup<String, AccessConfigUser>(
					grp, LocaleHelper.getLabelMap(name)) {

				@Override
				public boolean isInSelection(String object, AccessConfigUser grp) {
					AccessConfigUser userConfig = controller.getUserConfig(object);
					return ResourceHelper.containsLocation(userConfig.superGroups().getAllElements(), grp);
				}
			};
			result.add(newOption);			
		}
		return result;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public AccessConfigUser getSelectedUser(OgemaHttpRequest req) {
		GenericFilterOption<String> selected = getSelectedItem(req);
		if(selected instanceof SingleUserOption) {
			return super.getSelectedUser(req);
		}
		return ((GenericFilterFixedGroup<String, AccessConfigUser>)selected).getGroup();
	}

}
