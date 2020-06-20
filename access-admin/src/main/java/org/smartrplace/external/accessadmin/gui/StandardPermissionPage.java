package org.smartrplace.external.accessadmin.gui;

import java.util.List;

import org.ogema.accessadmin.api.util.UserPermissionUtil;
import org.ogema.core.application.ApplicationManager;
import org.ogema.core.model.ResourceList;
import org.ogema.core.model.simple.BooleanResource;
import org.smartrplace.external.accessadmin.config.AccessConfigBase;
import org.smartrplace.external.accessadmin.config.AccessConfigUser;
import org.smartrplace.gui.filtering.ObjectGUITablePageNamed;
import org.smartrplace.util.directobjectgui.ObjectGUITablePage;
import org.smartrplace.util.directobjectgui.ObjectResourceGUIHelper;
import org.smartrplace.util.format.WidgetHelper;

import de.iwes.widgets.api.extended.WidgetData;
import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.localisation.OgemaLocale;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.complextable.RowTemplate.Row;
import de.iwes.widgets.html.form.button.Button;
import de.iwes.widgets.html.form.button.ButtonData;
import de.iwes.widgets.html.form.checkbox.CheckboxData;
import de.iwes.widgets.html.form.checkbox.SimpleCheckbox;
import de.iwes.widgets.html.form.checkbox.SimpleCheckboxData;
import de.iwes.widgets.html.form.label.Header;

public abstract class StandardPermissionPage<T> extends ObjectGUITablePageNamed<T, BooleanResource> {
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
		super(page, appMan, sampleObject);
		//Trigger has to be done by implementing page
		//triggerPageBuild();
	}

	@Override
	public void addWidgets(T object, ObjectResourceGUIHelper<T, BooleanResource> vh, String id,
			OgemaHttpRequest req, Row row, ApplicationManager appMan) {
		//TODO: The type name and the permissions labels should depend on locale
		addNameLabel(object, vh, id, row);
		for(String label: getPermissionNames()) {
			if(req == null) {
				vh.registerHeaderEntry(label);
				continue;
			}
			PermissionCellData acc = getAccessConfig(object, label, req);
			Button perm = new Button(mainTable, "perm_"+label+id, "", req) {
				private static final long serialVersionUID = 1L;
				@Override
				public void onGET(OgemaHttpRequest req) {
					Boolean status = acc.getOwnstatus();
					if (status == null) {
						boolean inheritedStatus = acc.getEffectiveStatus();
						setStyle(ButtonData.BOOTSTRAP_DEFAULT, req);
						//setGlyphicon(inheritedStatus ? Glyphicons.CHECK : Glyphicons.OFF, req);
						setText(inheritedStatus ? "(✓ granted)": "(✕ denied)", req);
						setToolTip("Permission was " + (inheritedStatus ? "granted" : "denied") + " by inheritance.", req);
						
					} else {
						//setGlyphicon(status ? Glyphicons.CHECK : Glyphicons.OFF, req);
						setText(status ? "✓ granted": "✕ denied", req);
						if (status) {
							setStyle(ButtonData.BOOTSTRAP_GREEN, req);
						} else {
							setStyle(ButtonData.BOOTSTRAP_RED, req);
						}
						setToolTip("Permission was explicitly " + (status ? "granted" : "denied") + ".", req);
					}
				}
				
				@Override
				public void onPOSTComplete(String data, OgemaHttpRequest req) {
					Boolean status = acc.getOwnstatus();
					if (status == null) status = true;
					else if (status == true) status = false;
					else status = null;
					acc.setOwnStatus(status);
				}
			};
			row.addCell(WidgetHelper.getValidWidgetId(label), perm);
			perm.triggerOnPOST(perm);
		}
	}
}
