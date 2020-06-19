package org.smartrplace.gui.filtering.util;

import org.ogema.core.administration.UserAccount;
import org.ogema.core.application.ApplicationManager;
import org.ogema.internationalization.util.LocaleHelper;
import org.smartrplace.gui.filtering.GenericFilterOption;
import org.smartrplace.gui.filtering.SingleFiltering;

import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;

public class UserFilteringBase<T> extends SingleFiltering<String, T> {
	private static final long serialVersionUID = 1L;
	
	protected final ApplicationManager appMan;

	protected class SingleUserOption implements GenericFilterOption<String> {
		public SingleUserOption(String myUserName) {
			this.myUserName = myUserName;
		}

		public final String myUserName;

		@Override
		public boolean isInSelection(String object, OgemaHttpRequest req) {
			return myUserName.equals(object);
		}	
	}
	
	public UserFilteringBase(WidgetPage<?> page, String id, OptionSavingMode saveOptionMode,
			ApplicationManager appMan) {
		super(page, id, saveOptionMode);
		this.appMan = appMan;
		addOptions();
	}

	protected void addOptions() {
		for(UserAccount ac: appMan.getAdministrationManager().getAllUsers()) {
			GenericFilterOption<String> newOption = new SingleUserOption(ac.getName());
			addOption(newOption , LocaleHelper.getLabelMap(ac.getName()));
		}
		setDefaultAddEmptyOption(false);		
		finishOptionsSetup();
	}
	
	@Override
	protected boolean isAttributeSinglePerDestinationObject() {
		return true;
	}

	public String getSelectedUser(OgemaHttpRequest req) {
		return ((SingleUserOption)getSelectedItem(req)).myUserName;
	}
}
