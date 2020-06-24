package org.smartrplace.external.accessadmin.gui;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

import org.ogema.accessadmin.api.util.UserPermissionUtil;
import org.ogema.core.administration.UserAccount;
import org.ogema.tools.resource.util.ResourceUtils;
import org.smartrplace.external.accessadmin.AccessAdminController;
import org.smartrplace.external.accessadmin.config.AccessConfigUser;
import org.smartrplace.external.useradmin.gui.UserDataTbl;

import de.iwes.util.resource.ResourceHelper;
import de.iwes.util.resourcelist.ResourceListHelper;
import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.localisation.OgemaLocale;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;

public class UserConfigPage extends PerMultiselectConfigPage<UserDataTbl, AccessConfigUser, AccessConfigUser> {
	protected final AccessAdminController controller;

	public UserConfigPage(WidgetPage<?> page, AccessAdminController controller) {
		super(page, controller.appMan, new UserDataTbl("Init User"));
		this.controller = controller;
		triggerPageBuild();
	}

	@Override
	protected String getHeader(OgemaLocale locale) {
		return "User Attribute Configuration";
	}
	
	@Override
	public AccessConfigUser getResource(UserDataTbl object, OgemaHttpRequest req) {
		return object.accessConfig;
	}

	@Override
	public Collection<UserDataTbl> getObjectsInTable(OgemaHttpRequest req) {
		List<UserDataTbl> result = new ArrayList<>();
		for(UserAccount ac: appMan.getAdministrationManager().getAllUsers()) {
			UserDataTbl obj = new UserDataTbl(ac);
			obj.accessConfig = UserPermissionUtil.getOrCreateUserPermissions(controller.userPerms, ac.getName());
			result.add(obj);
		}
		result.sort(new Comparator<UserDataTbl>() {

			@Override
			public int compare(UserDataTbl o1, UserDataTbl o2) {
				return o1.userName().compareTo(o2.userName());
			}
		});
		return result;
	}

	@Override
	protected String getGroupColumnLabel() {
		return "User Attributes";
	}

	/*@Override
	protected TemplateMultiselect<AccessConfigUser> getMultiselect(UserDataTbl object, String lineId,
			OgemaHttpRequest req) {
		TemplateMultiselect<AccessConfigUser> groupSelect = new TemplateMultiselect<AccessConfigUser>(mainTable, "groupSelect"+id, req) {
			private static final long serialVersionUID = 1L;
			@Override
			public void onGET(OgemaHttpRequest req) {
				List<AccessConfigUser> selected = object.accessConfig.superGroups().getAllElements();
				List<AccessConfigUser> all = controller.getUserGroups(false);
				update(all, req);
				selectItems(selected, req);
			}
		};
		groupSelect.setTemplate(new DefaultDisplayTemplate<AccessConfigUser>() {
			@Override
			public String getLabel(AccessConfigUser object, OgemaLocale locale) {
				return ResourceUtils.getHumanReadableShortName(object);
			}
		});
		return groupSelect;
	}*/

	@Override
	protected String getTypeName(OgemaLocale locale) {
		return "User name";
	}

	@Override
	protected String getLabel(UserDataTbl obj) {
		return obj.userName();
	}

	@Override
	protected List<AccessConfigUser> getAllGroups(OgemaHttpRequest req) {
		return controller.getUserGroups(false, false);
	}

	@Override
	protected List<AccessConfigUser> getGroups(UserDataTbl object, OgemaHttpRequest req) {
		return ResourceListHelper.getAllElementsLocation(object.accessConfig.superGroups());
	}

	@Override
	protected String getGroupLabel(AccessConfigUser object) {
		return ResourceUtils.getHumanReadableShortName(object);
	}

	@Override
	protected void setGroups(UserDataTbl object, List<AccessConfigUser> groups, OgemaHttpRequest req) {
		if(!object.accessConfig.superGroups().exists()) {
			object.accessConfig.superGroups().create();
		}
		for(AccessConfigUser grp: groups) {
			if(object.accessConfig.superGroups().contains(grp))
				continue;
			object.accessConfig.superGroups().add(grp);
		}
		for(AccessConfigUser grp: object.accessConfig.superGroups().getAllElements()) {
			if(ResourceHelper.containsLocation(groups, grp))
				continue;
			grp.delete();
		}
		object.accessConfig.superGroups().activate(true);
	}

}
