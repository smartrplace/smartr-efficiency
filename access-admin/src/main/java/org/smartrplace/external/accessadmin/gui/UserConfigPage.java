package org.smartrplace.external.accessadmin.gui;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

import org.ogema.accessadmin.api.util.UserPermissionUtil;
import org.ogema.core.administration.UserAccount;
import org.ogema.core.application.ApplicationManager;
import org.ogema.tools.resource.util.ResourceUtils;
import org.smartrplace.external.accessadmin.AccessAdminController;
import org.smartrplace.external.accessadmin.config.AccessConfigUser;
import org.smartrplace.external.useradmin.gui.UserDataTbl;
import org.smartrplace.util.directobjectgui.ObjectGUITablePage;
import org.smartrplace.util.directobjectgui.ObjectResourceGUIHelper;
import org.smartrplace.util.format.WidgetHelper;

import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.localisation.OgemaLocale;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.complextable.RowTemplate.Row;
import de.iwes.widgets.html.form.label.Header;
import de.iwes.widgets.html.multiselect.TemplateMultiselect;
import de.iwes.widgets.template.DefaultDisplayTemplate;

public class UserConfigPage extends ObjectGUITablePage<UserDataTbl, AccessConfigUser> {
	protected final AccessAdminController controller;

	public UserConfigPage(WidgetPage<?> page, AccessAdminController controller) {
		super(page, controller.appMan, new UserDataTbl("Init User"), false);
		this.controller = controller;
		triggerPageBuild();
	}

	@Override
	public void addWidgets(UserDataTbl object, ObjectResourceGUIHelper<UserDataTbl, AccessConfigUser> vh, String id, OgemaHttpRequest req,
			Row row, ApplicationManager appMan) {
		vh.stringLabel("Name", id, object.userName(), row);
		if(req == null) {
			vh.registerHeaderEntry("User Groups");
			return;
		}
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
		row.addCell(WidgetHelper.getValidWidgetId("User Groups"), groupSelect);
		//MultiSelectExtendedStringArray<AccessConfigUser> groupSelect = new MultiSelectExtendedStringArray<AccessConfigUser>(mainTable, "groupSelect"+id,
		//		true, true, true, object.accessConfig.superGroups(), req);
	}

	@Override
	public AccessConfigUser getResource(UserDataTbl object, OgemaHttpRequest req) {
		return object.accessConfig;
	}

	@Override
	public void addWidgetsAboveTable() {
		Header header = new Header(page, "headerUserConfig", "User Configuration Page");
		page.append(header);
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

}
