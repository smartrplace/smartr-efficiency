package org.smartrplace.external.accessadmin.gui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.ogema.accessadmin.api.UserPermissionService;
import org.ogema.accessadmin.api.UserStatus;
import org.ogema.core.administration.UserAccount;
import org.ogema.core.model.simple.BooleanResource;
import org.ogema.tools.app.createuser.UserAdminBaseUtil;
import org.smartrplace.external.accessadmin.AccessAdminController;

import de.iwes.util.resource.OGEMAResourceCopyHelper;
import de.iwes.util.resource.OGEMAResourceCopyHelper.CopyParams;
import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.html.StaticTable;
import de.iwes.widgets.api.widgets.localisation.OgemaLocale;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.alert.Alert;
import de.iwes.widgets.html.alert.AlertData;
import de.iwes.widgets.html.form.button.Button;
import de.iwes.widgets.html.form.button.ButtonData;

@SuppressWarnings("serial")
public class UserStatusPermissionPage extends StandardPermissionPage<UserStatus> {
	protected final AccessAdminController controller;
	protected Button commitBtn;
	
	public UserStatusPermissionPage(WidgetPage<?> page, AccessAdminController controller) {
		super(page, controller.appMan, UserStatus.DISABLED);
		this.controller = controller;
		triggerPageBuild();
	}

	@Override
	protected String getHeader(OgemaLocale locale) {
		return "5. User App Mapping";
	}
	
	@Override
	protected List<String> getPermissionNames() {
		List<String> result = getPermissionNamesBase();
		if(System.getProperty("org.ogema.drivers.bacnet.processOnlyFirstIamMessagePerDevice") != null) {
			ArrayList<String> result2 = new ArrayList<String>(result);
			result2.add(UserPermissionService.BACNET);
			return result2;
		}
		return result;
	}
	protected List<String> getPermissionNamesBase() {
		if(Boolean.getBoolean("org.ogema.accessadmin.api.isappstore"))
			return Arrays.asList(UserPermissionService.APP_ACCESS_PERMISSIONS_WITHAPPSTORE);
		else
			return Arrays.asList(UserPermissionService.APP_ACCESS_PERMISSIONS);
	}

	@Override
	protected PermissionCellData getAccessConfig(UserStatus object, String permissionID, OgemaHttpRequest req) {
		ConfigurablePermission result = new ConfigurablePermission() {
			@Override
			public void setOwnStatus(Boolean newStatus) {
				super.setOwnStatus(newStatus);
				controller.appConfigData.userStatusPermissionChanged().<BooleanResource>create().setValue(true);
			}
		};
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
		commitBtn = new Button(page, "commitBtn", "Commit (Effect may take several minutes)") {

			@Override
			public void onGET(OgemaHttpRequest req) {
				boolean stat = controller.appConfigData.userStatusPermissionChanged().getValue();
				if(stat) {
					setStyle(ButtonData.BOOTSTRAP_RED, req);
				} else
					setStyle(ButtonData.BOOTSTRAP_DEFAULT, req);
			}
			@Override
			public void onPOSTComplete(String data, OgemaHttpRequest req) {
				List<UserAccount> allUsers = controller.getAllNaturalUsers(); //controller.appMan.getAdministrationManager().getAllUsers();
				for(UserAccount ac: allUsers) {
					//if((ac.getName().equals("master")||ac.getName().equals("guest2")))
					//	continue;
					//if(!controller.appManPlus.permMan().getAccessManager().isNatural(ac.getName()))
					//	continue;
					UserAdminBaseUtil.updateUserPermissionsToWorkingStatus(
							ac, controller.appManPlus);
				}
				CopyParams copyParams = new CopyParams(appMan, true, 0);
				OGEMAResourceCopyHelper.copySubResourceIntoDestination(controller.appConfigData.userStatusPermission(),
						controller.appConfigData.userStatusPermissionWorkingCopy(), copyParams );
				controller.appConfigData.userStatusPermissionChanged().setValue(false);
			}
		};
		commitBtn.registerDependentWidget(commitBtn);
		topTable.setContent(0, 1, commitBtn);
		page.append(topTable);
		Alert info = new Alert(page, "description","Explanation") {

			@Override
	    	public void onGET(OgemaHttpRequest req) {
	    		String text = "Set permissions for app access here. Note that you have to commit your changes"
	    				+ " before they will take effect! Committing you changes may take several minutes "
	    				+ "as the permissions for all users concerned have to be updated. When the Commit button "
	    				+ "is marked red you have uncommitted changes.";
				setHtml(text, req);
	    		allowDismiss(true, req);
	    		autoDismiss(-1, req);
	    	}
	    	
	    };
	    info.addDefaultStyle(AlertData.BOOTSTRAP_INFO);
	    info.setDefaultVisibility(true);
	    page.append(info);
	}
	
	@Override
	protected void finishPermissionButton(Button perm) {
		perm.registerDependentWidget(commitBtn);
	}
}
