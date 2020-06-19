package org.smartrplace.external.accessadmin.gui;

import java.util.List;

import org.ogema.accessadmin.api.util.UserPermissionUtil;
import org.ogema.core.application.ApplicationManager;
import org.ogema.core.model.ResourceList;
import org.ogema.core.model.simple.BooleanResource;
import org.smartrplace.external.accessadmin.config.AccessConfigBase;
import org.smartrplace.external.accessadmin.config.AccessConfigUser;
import org.smartrplace.util.directobjectgui.ObjectGUITablePage;
import org.smartrplace.util.directobjectgui.ObjectResourceGUIHelper;
import org.smartrplace.util.format.WidgetHelper;

import de.iwes.widgets.api.extended.WidgetData;
import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.localisation.OgemaLocale;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.complextable.RowTemplate.Row;
import de.iwes.widgets.html.form.button.ButtonData;
import de.iwes.widgets.html.form.checkbox.CheckboxData;
import de.iwes.widgets.html.form.checkbox.SimpleCheckbox;
import de.iwes.widgets.html.form.checkbox.SimpleCheckboxData;
import de.iwes.widgets.html.form.label.Header;

public abstract class StandardPermissionPage<T> extends ObjectGUITablePage<T, BooleanResource> {
	protected abstract String getTypeName(OgemaLocale locale);
	protected String getHeader(OgemaLocale locale) {
		return "Permission Configuration "+getTypeName(locale);
	}
	protected abstract String getLabel(T obj);
	
	public static class ConfigurablePermission implements PermissionCellData {
		String resourceId;
		String permissionId;
		AccessConfigBase accessConfig;
		boolean defaultStatus;
		ResourceList<AccessConfigUser> userPerms;
		String userName;
		
		@Override
		public Boolean getOwnstatus() {
			return UserPermissionUtil.getPermissionStatus(resourceId, permissionId, accessConfig);
		}
		@Override
		public void setOwnStatus(Boolean newStatus) {
			if(newStatus == null) {
				UserPermissionUtil.removePermissionSetting(resourceId, permissionId, accessConfig);				
			} else {
				UserPermissionUtil.addPermission(resourceId, permissionId, accessConfig,
						newStatus==null?null:(newStatus?1:0));
			}
		}
		@Override
		public boolean getDefaultStatus() {
			return defaultStatus;
		}
	}
	protected abstract List<String> getPermissionNames();
	protected abstract PermissionCellData getAccessConfig(T object, String permissionID,
			OgemaHttpRequest req);
	
	public StandardPermissionPage(WidgetPage<?> page, ApplicationManager appMan, T sampleObject) {
		super(page, appMan, sampleObject, false);
		//Trigger has to be done by implementing page
		//triggerPageBuild();
	}

	@Override
	public void addWidgets(T object, ObjectResourceGUIHelper<T, BooleanResource> vh, String id,
			OgemaHttpRequest req, Row row, ApplicationManager appMan) {
		//TODO: The type name and the permissions labels should depend on locale
		vh.stringLabel(getTypeName(null), id, getLabel(object), row);
		for(String label: getPermissionNames()) {
			if(req == null) {
				vh.registerHeaderEntry(label);
				continue;
			}
			PermissionCellData acc = getAccessConfig(object, label, req);
			SimpleCheckbox check = new SimpleCheckbox(mainTable, "check_"+label+id, "", req) {
				private static final long serialVersionUID = 1L;
				@Override
				public void onGET(OgemaHttpRequest req) {
					Boolean status = acc.getOwnstatus();
					setValue(status != null && status, req);
					if(status != null)
						addStyle(ButtonData.BOOTSTRAP_GREEN, req);
					else
						addStyle(ButtonData.BOOTSTRAP_DARKGREY, req);
				}
				@Override
				public void onPOSTComplete(String data, OgemaHttpRequest req) {
					boolean val = getValue(req);
					acc.setOwnStatus(val);				}
			};
			row.addCell(WidgetHelper.getValidWidgetId(label), check);
		}
	}

	@Override
	public BooleanResource getResource(T object, OgemaHttpRequest req) {
		return null;
	}

	@Override
	public void addWidgetsAboveTable() {
		Header header = new Header(page, WidgetHelper.getValidWidgetId("headerStdPermPage"+this.getClass().getSimpleName())) {
			private static final long serialVersionUID = 1L;

			@Override
			public void onGET(OgemaHttpRequest req) {
				setText(getHeader(req.getLocale()), req);
			}
		};
		page.append(header);
	}
}
