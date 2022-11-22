package org.smartrplace.apps.hw.install.gui.expert;

import java.util.Collection;

import org.ogema.accessadmin.api.ApplicationManagerPlus;
import org.ogema.core.application.ApplicationManager;
import org.ogema.core.model.Resource;
import org.ogema.devicefinder.api.DatapointService;
import org.smartrplace.gui.tablepages.ObjectGUITablePageNamed;
import org.smartrplace.model.sync.mqtt.GatewaySyncData;
import org.smartrplace.tissue.util.resource.GatewaySyncUtil;
import org.smartrplace.util.directobjectgui.ObjectResourceGUIHelper;
import org.smartrplace.util.format.WidgetHelper;

import de.iwes.util.resource.ResourceHelper;
import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.html.StaticTable;
import de.iwes.widgets.api.widgets.localisation.OgemaLocale;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.buttonconfirm.ButtonConfirm;
import de.iwes.widgets.html.complextable.RowTemplate.Row;

@SuppressWarnings("serial")
public class CascadingOverviewPage extends ObjectGUITablePageNamed<GatewaySyncData, GatewaySyncData> {
	private final DatapointService dpService;
	
	public CascadingOverviewPage(WidgetPage<?> page, ApplicationManagerPlus appManPlus) {
		super(page, appManPlus.appMan(), ResourceHelper.getSampleResource(GatewaySyncData.class));
		this.dpService = appManPlus.dpService();
		triggerPageBuild();
	}

	@Override
	public String getHeader(OgemaLocale locale) {
		return "Cascading Gateways Configured on this System";
	}
	
	@Override
	public void addWidgetsAboveTable() {
		StaticTable topTable = new StaticTable(1, 4);
		if(!Boolean.getBoolean("org.smartrplace.apps.subgateway")) {
			//TODO: We need to process this automatically when GatewaySyncData resources appear
			ButtonConfirm createGatewayResBut = new ButtonConfirm(page, "createGatewayResBut",
					"Check/Create Gateway Resources for MQTT-Synch based on replication-Resources") {
				@Override
				public void onPOSTComplete(String data, OgemaHttpRequest req) {
					Collection<GatewaySyncData> all = getObjectsInTable(null);
					for(GatewaySyncData gwSyncData: all) {
						String gatewayBaseId = GatewaySyncUtil.getGatewayBaseId(gwSyncData);
						if(gatewayBaseId == null)
							continue;
						GatewaySyncUtil.getOrCreateGatewayResource(gatewayBaseId, appMan);						
					}
				}
			};
			createGatewayResBut.setDefaultConfirmMsg(
					"Really create resources foreseen to collect sub gateway data based on replication-Resources?");
			topTable.setContent(0, 0, createGatewayResBut);
		}
		page.append(topTable);
	}
	
	@Override
	public void addWidgets(final GatewaySyncData object, ObjectResourceGUIHelper<GatewaySyncData, GatewaySyncData> vh,
			String id, OgemaHttpRequest req, Row row, final ApplicationManager appMan) {
		String gatewayBaseId = addNameLabelPlus(object, vh, id, row, req);
		vh.stringLabel("Location", id, object.getLocation(), row);
		if(req == null) {
			vh.registerHeaderEntry("GWRes Location");
			vh.registerHeaderEntry("Read device rooms and set locally");
			vh.registerHeaderEntry("Write device rooms");
			return;
		}
		if(gatewayBaseId.isEmpty())
			return;
		final Resource gatewayRes = GatewaySyncUtil.getGatewayResource(gatewayBaseId, appMan.getResourceAccess());
		String text;
		if(Boolean.getBoolean("org.smartrplace.apps.subgateway")) {
			if(gatewayRes == null)
				text = "(Subgateway)";
			else
				text = "!! Should not exist on subgatway:"+gatewayRes.getLocation();
		} else if(gatewayRes == null)
			text = "No GatewayResource";
		else
			text = gatewayRes.getLocation();
		vh.stringLabel("GWRes Location", id, text, row);
		
		ButtonConfirm roomSetLocationBut = new ButtonConfirm(mainTable, "roomSetLocationBut"+id, req) {
			@Override
			public void onPOSTComplete(String data, OgemaHttpRequest req) {
				GatewaySyncUtil.setDeviceRoomLocations(object, appMan);
			}
		};
		if(Boolean.getBoolean("org.smartrplace.apps.subgateway"))
			roomSetLocationBut.setDefaultConfirmMsg("Really update and overwrite device room data from data provided by superior?");
		else
			roomSetLocationBut.setDefaultConfirmMsg("Really update and overwrite device room data from data provided by sub gateways?");
		roomSetLocationBut.setDefaultText("Read device rooms and set locally");
		row.addCell(WidgetHelper.getValidWidgetId("Read device rooms and set locally"), roomSetLocationBut);
		
			
		ButtonConfirm roomWriteLocationBut = new ButtonConfirm(mainTable, "roomWriteLocationBut"+id, req) {
			@Override
			public void onPOSTComplete(String data, OgemaHttpRequest req) {
				if(Boolean.getBoolean("org.smartrplace.apps.subgateway")) {
					GatewaySyncUtil.writeDeviceNamesEntriesOnSubGw(object, dpService);
				} else {
					GatewaySyncUtil.writeDeviceNamesEntriesOnSuperior(object, gatewayRes);
				}
			}
		};
		if(Boolean.getBoolean("org.smartrplace.apps.subgateway"))
			roomWriteLocationBut.setDefaultConfirmMsg("Really synchronize local device room data to superior?");
		else
			roomWriteLocationBut.setDefaultConfirmMsg("Really synchronize local device room data to sub gateways?");
		roomWriteLocationBut.setDefaultText("Write device rooms");
		row.addCell(WidgetHelper.getValidWidgetId("Write device rooms"), roomWriteLocationBut);
	}

	@Override
	protected String getTypeName(OgemaLocale locale) {
		return "GatewayBaseId";
	}

	@Override
	protected String getLabel(GatewaySyncData obj, OgemaHttpRequest req) {
		String result = GatewaySyncUtil.getGatewayBaseId(obj);
		if(result != null)
			return result;
		return "";
	}

	@Override
	public Collection<GatewaySyncData> getObjectsInTable(OgemaHttpRequest req) {
		return appMan.getResourceAccess().getResources(GatewaySyncData.class);
	}

}
