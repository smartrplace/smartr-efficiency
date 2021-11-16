package org.smartrplace.external.accessadmin.gui;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.ogema.accessadmin.api.ConfigurablePermission;
import org.ogema.accessadmin.api.UserPermissionService;
import org.ogema.accessadmin.api.UserPermissionUtil;
import org.ogema.tools.resource.util.ResourceUtils;
import org.smartrplace.external.accessadmin.AccessAdminController;
import org.smartrplace.external.accessadmin.config.AccessConfigUser;

import de.iwes.util.resource.ResourceHelper;
import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.html.StaticTable;
import de.iwes.widgets.api.widgets.localisation.OgemaLocale;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.alert.Alert;
import de.iwes.widgets.html.alert.AlertData;

@SuppressWarnings("serial")
public class UserConfigPermissionPage extends StandardPermissionPage<AccessConfigUser> {
	protected final AccessAdminController controller;
	
	public UserConfigPermissionPage(WidgetPage<?> page, AccessAdminController controller) {
		super(page, controller.appMan, ResourceHelper.getSampleResource(AccessConfigUser.class));
		this.controller = controller;
		triggerPageBuild();
	}

	@Override
	protected String getHeader(OgemaLocale locale) {
		return "6. User Config Permissions";
	}
	
	@Override
	protected List<String> getPermissionNames() {
		List<String> result = getPermissionNamesBase();
		return result;
	}
	protected List<String> getPermissionNamesBase() {
		return Arrays.asList(UserPermissionService.APP_CONFIG_PERMISSIONS);
	}

	@Override
	protected ConfigurablePermission getAccessConfig(AccessConfigUser userAcc, String permissionID,
			OgemaHttpRequest req) {
		String userName = userAcc.name().getValue();
		//String userName = userFilter.getSelectedUser(req);
		//AccessConfigUser userAcc = UserPermissionUtil.getUserPermissions(
		//		userPerms, userName);
		ConfigurablePermission result = new ConfigurablePermission();

		result.accessConfig = userAcc.otherResourcepermissionData(); //.roompermissionData();
		result.resourceId = UserPermissionUtil.SYSTEM_RESOURCE_ID;
		result.permissionId = permissionID;
		result.defaultStatus = controller.userPermService.getUserSystemPermission(userName,permissionID, true) > 0;
		return result;
	}

	@Override
	protected String getTypeName(OgemaLocale locale) {
		return "User Type";
	}

	@Override
	protected String getLabel(AccessConfigUser obj, OgemaHttpRequest req) {
		return ResourceUtils.getHumanReadableShortName(obj);
	}

	@Override
	public Collection<AccessConfigUser> getObjectsInTable(OgemaHttpRequest req) {
		List<AccessConfigUser> all = controller.getUserGroups(false);
		return all;
	}

	@Override
	public void addWidgetsAboveTable() {
		super.addWidgetsAboveTable();
		StaticTable topTable = new StaticTable(1, 4);
		page.append(topTable);
		Alert info = new Alert(page, "description","Explanation") {

			@Override
	    	public void onGET(OgemaHttpRequest req) {
	    		String text = "Set room control configuration permisions here.";
				setHtml(text, req);
	    		allowDismiss(true, req);
	    		autoDismiss(-1, req);
	    	}
	    	
	    };
	    info.addDefaultStyle(AlertData.BOOTSTRAP_INFO);
	    info.setDefaultVisibility(true);
	    page.append(info);
	}
}
