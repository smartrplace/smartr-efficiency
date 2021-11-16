package org.smartrplace.external.accessadmin.gui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.ogema.accessadmin.api.ApplicationManagerPlus;
import org.ogema.accessadmin.api.ConfigurablePermission;
import org.ogema.accessadmin.api.SubcustomerUtil;
import org.ogema.accessadmin.api.SubcustomerUtil.SubCustomer;
import org.ogema.accessadmin.api.UserPermissionService;
import org.ogema.accessadmin.api.UserPermissionUtil;
import org.ogema.core.model.ResourceList;
import org.ogema.model.locations.Room;
import org.ogema.tools.resource.util.ResourceUtils;
import org.smartrplace.external.accessadmin.config.AccessAdminConfig;
import org.smartrplace.external.accessadmin.config.AccessConfigUser;
import org.smartrplace.external.accessadmin.config.SubCustomerData;
import org.smartrplace.gui.filtering.SingleFiltering.OptionSavingMode;
import org.smartrplace.gui.filtering.util.UserFiltering2Steps;

import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.html.StaticTable;
import de.iwes.widgets.api.widgets.localisation.OgemaLocale;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;

@SuppressWarnings("serial")
@Deprecated //maybe not required
public class UserSubcustomerPermissionPage extends StandardPermissionPage<SubCustomer> {
	//protected final AccessAdminController controller;
	protected final AccessAdminConfig appConfigData;
	protected final ApplicationManagerPlus appManPlus;
	
	protected UserFiltering2Steps<Room> userFilter;

	protected ResourceList<AccessConfigUser> userPerms;
	
	public UserSubcustomerPermissionPage(WidgetPage<?> page, AccessAdminConfig appConfigData, ApplicationManagerPlus appManPlus) {
		super(page, appManPlus.appMan(), null);
		this.appConfigData = appConfigData;
		this.appManPlus = appManPlus;
		userPerms = appConfigData.userPermissions();
		triggerPageBuild();
	}

	@Override
	protected String getHeader(OgemaLocale locale) {
		return "Single Room Access Permissions";
	}
	
	@Override
	protected String getTypeName(OgemaLocale locale) {
		return "Room";
	}

	@Override
	protected String getLabel(SubCustomer obj, OgemaHttpRequest req) {
		return ResourceUtils.getHumanReadableShortName(obj.res);
	}

	@Override
	protected List<String> getPermissionNames() {
		return Arrays.asList(UserPermissionService.USER_ROOM_PERM);
		//TODO: Would have to be reactivated if used again
		//return Arrays.asList(UserPermissionService.SUBCUSTOMER_PERMISSONS);
	}

	@Override
	protected ConfigurablePermission getAccessConfig(SubCustomer object, String permissionID,
			OgemaHttpRequest req) {
		String userName = userFilter.getSelectedUser(req);
		AccessConfigUser userAcc = UserPermissionUtil.getUserPermissions(
				userPerms, userName);
		ConfigurablePermission result = new ConfigurablePermission() {
			@Override
			public boolean supportsUnset() {
				return true;
			}
		};
		//We have to choose the right permission data for the page here
		if(userAcc == null)
			userAcc = UserPermissionUtil.getOrCreateUserPermissions(userPerms, userName);
		result.accessConfig = userAcc.roompermissionData();
		result.resourceId = object.res.getLocation();
		result.permissionId = permissionID;
		//String userName = userAcc.name().getValue();
		result.defaultStatus = appManPlus.userPermService().getUserPermissionForRoom(userName, result.resourceId,
				permissionID, true) > 0;
		return result;
	}

	@Override
	public void addWidgetsAboveTable() {
		super.addWidgetsAboveTable();
		StaticTable topTable = new StaticTable(1, 5);
		userFilter = new UserFiltering2Steps<Room>(page, "userFilter",
				OptionSavingMode.GENERAL, 5000, appConfigData, appManPlus) {

					@Override
					protected String getAttribute(Room object) {
						throw new IllegalStateException("GetAttribute should never be called on this UserFiltering! This does not really filter for room(s)...");
					}
			
		};
		
		userFilter.registerDependentWidget(mainTable);
		
		topTable.setContent(0, 0, userFilter.getFirstDropdown()).setContent(0, 1, userFilter);
		page.append(topTable);
	}

	@Override
	public Collection<SubCustomer> getObjectsInTable(OgemaHttpRequest req) {
		List<SubCustomerData> all = appConfigData.subCustomers().getAllElements();
		
		List<SubCustomer> result = new ArrayList<>();
		for(SubCustomerData subc: all) {
			SubCustomer data = SubcustomerUtil.getFullObject(subc);
			result.add(data);
		}
		
		return result;
	}
	
	@Override
	public String getLineId(SubCustomer object) {
		return ResourceUtils.getHumanReadableName(object.res)+super.getLineId(object);
	}	
}
