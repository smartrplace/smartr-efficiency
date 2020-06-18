package org.smartrplace.external.accessadmin.gui;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.ogema.accessadmin.api.UserPermissionService;
import org.ogema.accessadmin.api.util.UserPermissionUtil;
import org.ogema.model.locations.Room;
import org.ogema.tools.resource.util.ResourceUtils;
import org.smartrplace.external.accessadmin.AccessAdminController;
import org.smartrplace.external.accessadmin.config.AccessConfigUser;
import org.smartrplace.gui.filtering.DualFiltering;
import org.smartrplace.gui.filtering.SingleFiltering;
import org.smartrplace.gui.filtering.SingleFiltering.OptionSavingMode;
import org.smartrplace.gui.filtering.util.RoomFilteringWithGroups;
import org.smartrplace.gui.filtering.util.UserFilteringBase;

import de.iwes.util.resource.ResourceHelper;
import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.html.StaticTable;
import de.iwes.widgets.api.widgets.localisation.OgemaLocale;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;

public class UserRoomPermissionPage extends StandardPermissionPage<Room> {
	protected final AccessAdminController controller;
	
	protected DualFiltering<String, Room, Room> dualFiltering;
	
	public UserRoomPermissionPage(WidgetPage<?> page, AccessAdminController controller) {
		super(page, controller.appMan, ResourceHelper.getSampleResource(Room.class));
		this.controller = controller;
	}

	@Override
	protected String getTypeName(OgemaLocale locale) {
		return "Room";
	}

	@Override
	protected String getLabel(Room obj) {
		return ResourceUtils.getHumanReadableShortName(obj);
	}

	@Override
	protected List<String> getPermissionNames() {
		return Arrays.asList(UserPermissionService.ROOMPERMISSONS);
	}

	@Override
	protected ConfigurablePermission getAccessConfig(Room object, String permissionID,
			OgemaHttpRequest req) {
		SingleFiltering<String, Room> drop = dualFiltering.getDropdownA();
		
		String userName = ((UserFilteringBase<Room>)drop).getSelectedUser(req);
		AccessConfigUser userAcc = UserPermissionUtil.getUserPermissions(
				controller.appConfigData.userPermissions(), userName);
		ConfigurablePermission result = new ConfigurablePermission();
		//We have to choose the right permission data for the page here
		result.accessConfig = userAcc.roompermissionData();
		result.resourceId = object.getLocation();
		result.permissionId = permissionID;
		return result;
	}

	@Override
	public void addWidgetsAboveTable() {
		super.addWidgetsAboveTable();
		StaticTable topTable = new StaticTable(1, 5);
		RoomFilteringWithGroups<Room> roomFilter = new RoomFilteringWithGroups<Room>(page, "roomFilter",
				OptionSavingMode.PER_USER, controller.appConfigData.roomGroups()) {
			private static final long serialVersionUID = 1L;

			@Override
			protected Room getAttribute(Room object) {
				return object;
			}
		};
		UserFilteringBase<Room> userFilter = new UserFilteringBase<Room>(page, "userFilter",
				OptionSavingMode.GENERAL, controller.appMan);
		topTable.setContent(0, 1, userFilter).setContent(0,  2, roomFilter);
		page.append(topTable);
		dualFiltering = new DualFiltering<String, Room, Room>(
				userFilter, roomFilter);
	}

	@Override
	public Collection<Room> getObjectsInTable(OgemaHttpRequest req) {
		List<Room> all = controller.appMan.getResourceAccess().getToplevelResources(Room.class);
		Collection<Room> result = dualFiltering.getFiltered(all, req);
		return result;
	}
}
