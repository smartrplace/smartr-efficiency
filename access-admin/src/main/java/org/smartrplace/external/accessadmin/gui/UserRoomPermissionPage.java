package org.smartrplace.external.accessadmin.gui;

import java.util.List;

import org.ogema.model.locations.Room;
import org.smartrplace.external.accessadmin.AccessAdminController;

import de.iwes.util.resource.ResourceHelper;
import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.html.StaticTable;
import de.iwes.widgets.api.widgets.localisation.OgemaLocale;

public class UserRoomPermissionPage extends StandardPermissionPage<Room> {
	protected final AccessAdminController controller;
	
	public UserRoomPermissionPage(WidgetPage<?> page, AccessAdminController controller) {
		super(page, controller.appMan, ResourceHelper.getSampleResource(Room.class));
		this.controller = controller;
	}

	@Override
	protected String getTypeName(OgemaLocale locale) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected String getLabel(Room obj) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected List<String> getPermissionNames() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected ConfigurablePermission getAccessConfig(Room object) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void addWidgetsAboveTable() {
		super.addWidgetsAboveTable();
		StaticTable topTable = new StaticTable(1, 5);
	}
}
