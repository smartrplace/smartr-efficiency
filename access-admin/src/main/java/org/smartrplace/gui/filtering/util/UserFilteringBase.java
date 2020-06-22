package org.smartrplace.gui.filtering.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.ogema.core.administration.UserAccount;
import org.ogema.core.application.ApplicationManager;
import org.ogema.internationalization.util.LocaleHelper;
import org.smartrplace.gui.filtering.GenericFilterBase;
import org.smartrplace.gui.filtering.GenericFilterFixedSingle;
import org.smartrplace.gui.filtering.GenericFilterOption;
import org.smartrplace.gui.filtering.SingleFiltering;

import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.localisation.OgemaLocale;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;

public class UserFilteringBase<T> extends SingleFiltering<String, T> {
	private static final long serialVersionUID = 1L;
	//TODO: Add this as constructor parameter
	public static final long UPDATE_RATE = 5000;
	
	protected final ApplicationManager appMan;
	
	public static class SingleUserOption extends GenericFilterFixedSingle<String> {
		public SingleUserOption(String myUserName, Map<OgemaLocale, String> optionLab) {
			super(myUserName, optionLab);
		}
	}
	
	public UserFilteringBase(WidgetPage<?> page, String id, OptionSavingMode saveOptionMode,
			long optionSetUpdateRate, ApplicationManager appMan) {
		super(page, id, saveOptionMode, optionSetUpdateRate, false);
		this.appMan = appMan;
	}

	@Override
	protected boolean isAttributeSinglePerDestinationObject() {
		return true;
	}

	public String getSelectedUser(OgemaHttpRequest req) {
		return ((SingleUserOption)getSelectedItem(req)).getValue();
	}
	
	@Override
	protected long getFrameworkTime() {
		return appMan.getFrameworkTime();
	}
	
	@Override
	protected List<GenericFilterOption<String>> getOptionsDynamic(OgemaHttpRequest req) {
		List<GenericFilterOption<String>> result = new ArrayList<>();
		for(UserAccount ac: appMan.getAdministrationManager().getAllUsers()) {
			GenericFilterOption<String> newOption = new SingleUserOption(ac.getName(), LocaleHelper.getLabelMap(ac.getName()));
			result.add(newOption);
		}
		return result;
	}
}
