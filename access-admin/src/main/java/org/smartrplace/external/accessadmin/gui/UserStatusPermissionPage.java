package org.smartrplace.external.accessadmin.gui;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.ogema.accessadmin.api.UserPermissionService;
import org.ogema.accessadmin.api.UserStatus;
import org.ogema.core.administration.UserAccount;
import org.ogema.tools.app.createuser.UserAdminBaseUtil;
import org.smartrplace.external.accessadmin.AccessAdminController;

import de.iwes.util.resource.OGEMAResourceCopyHelper;
import de.iwes.util.resource.OGEMAResourceCopyHelper.CopyParams;
import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.html.StaticTable;
import de.iwes.widgets.api.widgets.localisation.OgemaLocale;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.form.button.Button;

public class UserStatusPermissionPage extends StandardPermissionPage<UserStatus> {
	protected final AccessAdminController controller;
	
	public UserStatusPermissionPage(WidgetPage<?> page, AccessAdminController controller) {
		super(page, controller.appMan, UserStatus.DISABLED);
		this.controller = controller;
		triggerPageBuild();
	}

	@Override
	protected String getHeader(OgemaLocale locale) {
		return "User Type App Access";
	}
	
	@Override
	protected List<String> getPermissionNames() {
		return Arrays.asList(UserPermissionService.APP_ACCESS_PERMISSIONS);
	}

	@Override
	protected PermissionCellData getAccessConfig(UserStatus object, String permissionID, OgemaHttpRequest req) {
		ConfigurablePermission result = new ConfigurablePermission();
		CopyParams copyParams = new CopyParams(appMan, true, 0);
		if(!controller.appConfigData.userStatusPermissionWorkingCopy().isActive())
			OGEMAResourceCopyHelper.copySubResourceIntoDestination(
					controller.appConfigData.userStatusPermissionWorkingCopy(),
					controller.appConfigData.userStatusPermission(), copyParams );
		result.accessConfig = controller.appConfigData.userStatusPermissionWorkingCopy(); //userAcc.roompermissionData();
		result.resourceId = object.name();
		result.permissionId = permissionID;
		result.defaultStatus = false; //controller.userPermService.getUserStatusAppPermission(object, permissionID, true) > 0;
		return result;
	}

	@Override
	protected String getTypeName(OgemaLocale locale) {
		return "User Type";
	}

	@Override
	protected String getLabel(UserStatus obj) {
		return UserStatus.getLabel(obj, null);
	}

	@Override
	public Collection<UserStatus> getObjectsInTable(OgemaHttpRequest req) {
		return Arrays.asList(UserStatus.values());
	}

	@Override
	public String getLineId(UserStatus object) {
		int idx = 0;
		for(UserStatus val: UserStatus.values()) {
			if(val == object)
				break;
			idx++;
		}
		return String.format("%02d", idx)+super.getLineId(object);
	}
	
	
	@Override
	public void addWidgetsAboveTable() {
		super.addWidgetsAboveTable();
		StaticTable topTable = new StaticTable(1, 4);
		Button commitBtn = new Button(page, "commitBtn", "Commit (Effect may take several minutes)") {
			private static final long serialVersionUID = 1L;
			@Override
			public void onPOSTComplete(String data, OgemaHttpRequest req) {
				List<UserAccount> allUsers = controller.appMan.getAdministrationManager().getAllUsers();
				for(UserAccount ac: allUsers) {
					if((ac.getName().equals("master")||ac.getName().equals("guest2")))
						continue;
					if(!controller.appManPlus.permMan().getAccessManager().isNatural(ac.getName()))
						continue;
					UserAdminBaseUtil.updateUserPermissionsToWorkingStatus(
							ac, controller.appManPlus);
				}
				CopyParams copyParams = new CopyParams(appMan, true, 0);
				OGEMAResourceCopyHelper.copySubResourceIntoDestination(controller.appConfigData.userStatusPermission(),
						controller.appConfigData.userStatusPermissionWorkingCopy(), copyParams );
				controller.appConfigData.userStatusPermissionWorkingCopy().delete();
			}
		};
		topTable.setContent(0, 1, commitBtn);
		page.append(topTable);
	}
}
