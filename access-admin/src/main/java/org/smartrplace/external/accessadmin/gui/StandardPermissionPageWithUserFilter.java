package org.smartrplace.external.accessadmin.gui;

import org.ogema.core.application.ApplicationManager;

import de.iwes.widgets.api.widgets.WidgetPage;

public abstract class StandardPermissionPageWithUserFilter<T extends UserTaggedTbl> extends StandardPermissionPage<T> {
	//protected UserFilteringBase<Room> userFilter;	
	
	public StandardPermissionPageWithUserFilter(WidgetPage<?> page, ApplicationManager appMan, T sampleObject) {
		super(page, appMan, sampleObject);
	}

	@Override
	public String getLineId(T object) {
		return object.index+super.getLineId(object)+object.userName;
	}
}
