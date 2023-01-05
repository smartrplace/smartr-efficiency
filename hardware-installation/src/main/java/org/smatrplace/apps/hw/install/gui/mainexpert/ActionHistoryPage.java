package org.smatrplace.apps.hw.install.gui.mainexpert;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.ogema.core.application.ApplicationManager;
import org.ogema.core.model.Resource;
import org.ogema.core.model.ResourceList;
import org.ogema.core.model.simple.StringResource;
import org.ogema.timeseries.eval.simple.api.TimeProcUtil;
import org.smartrplace.apps.hw.install.config.HardwareInstallConfig;
import org.smartrplace.gui.tablepages.ObjectGUITablePageNamed;
import org.smartrplace.util.directobjectgui.ObjectResourceGUIHelper;

import de.iwes.util.format.StringFormatHelper;
import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.html.StaticTable;
import de.iwes.widgets.api.widgets.localisation.OgemaLocale;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.buttonconfirm.ButtonConfirm;
import de.iwes.widgets.html.complextable.RowTemplate.Row;

public class ActionHistoryPage extends ObjectGUITablePageNamed<ActionHistoryItem, Resource> {
	private final static Long cleanUpAgoHours = Long.getLong("org.smatrplace.apps.hw.install.gui.mainexpert.historyCleanupMaintenanceHours", 680);
	private final HardwareInstallConfig hwConfig;
	private final ResourceList<StringResource> history;
	private final boolean isExpert;
	
	public ActionHistoryPage(WidgetPage<?> page, ApplicationManager appMan) {
		this(page, appMan, true);
	}
	public ActionHistoryPage(WidgetPage<?> page, ApplicationManager appMan, boolean isExpert) {
		super(page, appMan, new ActionHistoryItem());
		this.isExpert = isExpert;
		hwConfig = appMan.getResourceAccess().getResource("hardwareInstallConfig");
		history = hwConfig.actionHistory();
		triggerPageBuild();
	}

	@Override
	public String getHeader(OgemaLocale locale) {
		return "Roomcontrol Configuration History";
	}
	
	@Override
	public void addWidgetsAboveTable() {
		if(!isExpert)
			return;
		StaticTable topTable = new StaticTable(1, 6);
		ButtonConfirm clearButton = new ButtonConfirm(page, "clearButton",
				"Clean up entries older than "+cleanUpAgoHours+" h (+old format)") {
			@Override
			public void onPOSTComplete(String data, OgemaHttpRequest req) {
				List<StringResource> all = history.getAllElements();
				long now = appMan.getFrameworkTime();
				long cleanUpAgo = cleanUpAgoHours * TimeProcUtil.HOUR_MILLIS;
				for(StringResource sres: all) {
					String val = sres.getValue();
					List<String> vals = StringFormatHelper.getListFromString(val, "$,");
					final boolean delete;
					if(vals.size() < 7)
						delete = true;
					else {
						long ago = now - sres.getLastUpdateTime();
						delete = (ago > cleanUpAgo);
					}
					if(delete)
						sres.delete();
				}
			}
		};
		clearButton.setDefaultConfirmMsg("Really delete older messages?");
		topTable.setContent(0, 5, clearButton);
		page.append(topTable);
	}
	
	@Override
	public void addWidgets(ActionHistoryItem object, ObjectResourceGUIHelper<ActionHistoryItem, Resource> vh, String id,
			OgemaHttpRequest req, Row row, ApplicationManager appMan) {
		vh.stringLabel("ID", id, object.id, row);
		vh.stringLabel("User", id, object.user, row);
		vh.stringLabel("Time", id, object.time, row);
		vh.stringLabel("Action", id, object.action, row);
		vh.stringLabel("New Value", id, object.newValue, row);
		vh.stringLabel("Tenant", id, object.tentant, row);
		vh.stringLabel("Room Type", id, object.roomType, row);
		vh.stringLabel("Single Room", id, object.singleRoom
				, row);
	}

	@Override
	protected String getTypeName(OgemaLocale locale) {
		return "ID";
	}

	@Override
	protected String getLabel(ActionHistoryItem obj, OgemaHttpRequest req) {
		return obj.id;
	}

	@Override
	public Collection<ActionHistoryItem> getObjectsInTable(OgemaHttpRequest req) {
		List<ActionHistoryItem> result = new ArrayList<>();
		List<StringResource> all = history.getAllElements();
		for(StringResource sres: all) {
			String val = sres.getValue();
			List<String> vals = StringFormatHelper.getListFromString(val, "$,");
			ActionHistoryItem item = new ActionHistoryItem();
			try {
				item.id = vals.get(0);
				item.user = vals.get(1);
				item.time = vals.get(2);
				item.action = vals.get(3);
				item.newValue = vals.get(4);
				item.tentant = vals.get(5);
				item.roomType = vals.get(6);
				item.singleRoom = vals.get(7);
				result.add(item);
			} catch(IndexOutOfBoundsException e) {
				appMan.getLogger().error("History element too short:"+sres.getLocation()+"  :  "+val);
			}
		}
		return result;
	}

	@Override
	public String getLineId(ActionHistoryItem object) {
		return object.id+super.getLineId(object);
	}
}
