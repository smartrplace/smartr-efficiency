package org.ogema.tools.app.createuser;

import java.util.List;

import org.ogema.accessadmin.api.ApplicationManagerPlus;
import org.ogema.core.administration.UserAccount;
import org.ogema.core.application.ApplicationManager;
import org.ogema.tools.resource.util.ResourceUtils;
import org.smartrplace.apps.hw.install.prop.GatewayMainPage;
import org.smartrplace.system.guiappstore.config.AppstoreConfig;
import org.smartrplace.system.guiappstore.config.GatewayData;
import org.smartrplace.util.directobjectgui.ObjectResourceGUIHelper;

import de.iwes.util.resource.ValueResourceHelper;
import de.iwes.util.resourcelist.ResourceListHelper;
import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.buttonconfirm.ButtonConfirm;
import de.iwes.widgets.html.complextable.RowTemplate.Row;
import de.iwes.widgets.html.form.button.Button;

@SuppressWarnings("serial")
public class GatewayMainPageExpert extends GatewayMainPage {
	protected final ApplicationManagerPlus controller;
	public static final String SERVER_INSTANCE_LINK =
			"https://gitlab.smartrplace.de/i1/smartrplace/smartrplace-main/-/wikis/Server-Administration/OGEMA-instances-on-Smartrplace-servers#server-instances-list";
	
	public GatewayMainPageExpert(WidgetPage<?> page, ApplicationManagerPlus controller, AppstoreConfig appConfigData) {
		super(page, controller.appMan(), appConfigData, SERVER_INSTANCE_LINK);
		this.controller = controller;
	}

	@Override
	public void addWidgets(GatewayData object, ObjectResourceGUIHelper<GatewayData, GatewayData> vh, String id,
			OgemaHttpRequest req, Row row, ApplicationManager appMan) {
		super.addWidgets(object, vh, id, req, row, appMan);
		if(req == null) {
			vh.registerHeaderEntry("Delete");
			vh.registerHeaderEntry("Edit Id");
			return;
		}
		if(object.name().exists())
			vh.stringEdit("Edit Id", id, object.name(), row, alert);
		ButtonConfirm deleteButton = new ButtonConfirm(mainTable, "deleteButton"+id, req) {
			@Override
			public void onPOSTComplete(String data, OgemaHttpRequest req) {
				object.delete();
			}
		};
		deleteButton.setDefaultText("Delete");
		deleteButton.setDefaultConfirmMsg("Really delete "+ResourceUtils.getHumanReadableName(object)+" ?");
		row.addCell("Delete", deleteButton);
	}
	
	@Override
	public void addWidgetsAboveTable() {
		super.addWidgetsAboveTable();
		Button updateBtn = new Button(page, "updateBtn", "Update gateways from REST users") {
			private static final long serialVersionUID = 1L;
			@Override
			public void onPOSTComplete(String data, OgemaHttpRequest req) {
				updateGatewaysFromRESTUsers(controller, appConfigData);
			}
		};

		Button addButton = new Button(page, "addButton", "Add new Gateway") {
			@Override
			public void onPOSTComplete(String data, OgemaHttpRequest req) {
				GatewayData newEl = appConfigData.gatewayData().add();
				ValueResourceHelper.setCreate(newEl.name(), newEl.getName());
				newEl.activate(true);
			}
		};
		topTable.setContent(0, 1, updateBtn);
		topTable.setContent(0, 3, addButton);
	}

	public static void updateGatewaysFromRESTUsers(ApplicationManagerPlus controller, AppstoreConfig appConfigData) {
		List<UserAccount> users = UserAdminBaseUtil.getRESTUsers(controller);
		for(UserAccount uacc: users) {
			String name = uacc.getName();
			if(name.toLowerCase().startsWith("rest"))
				continue;
			GatewayData gwData = ResourceListHelper.getOrCreateNamedElement(name,
					appConfigData.gatewayData());
			if(ValueResourceHelper.setIfNew(gwData.remoteSlotsGatewayId(), name)) {
				gwData.activate(true);
			}
		}	
	}
}

