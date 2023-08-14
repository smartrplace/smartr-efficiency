package org.smartrplace.apps.alarmingconfig.history;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.ogema.core.application.ApplicationManager;
import org.ogema.core.model.Resource;
import org.ogema.core.model.ResourceList;
import org.ogema.core.model.simple.StringResource;
import org.ogema.timeseries.eval.simple.api.TimeProcUtil;
import org.smartrplace.apps.alarmingconfig.AlarmingConfigAppController;
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
import de.iwes.widgets.html.form.button.Button;

@SuppressWarnings("serial")
public class IssueHistoryPage extends ObjectGUITablePageNamed<IssueHistoryItem, Resource> {
	private final static Long cleanUpAgoHours = Long.getLong("org.smatrplace.apps.hw.install.gui.mainexpert.historyCleanupMaintenanceHours", 680);
	private final HardwareInstallConfig hwConfig;
	private final ResourceList<StringResource> history;
	private final AlarmingConfigAppController controller;
	//private final boolean isExpert;
	
	public IssueHistoryPage(WidgetPage<?> page, AlarmingConfigAppController controller) {
		super(page, controller.appMan, new IssueHistoryItem());
		hwConfig = appMan.getResourceAccess().getResource("hardwareInstallConfig");
		history = hwConfig.issueHistory();
		this.controller = controller;
		triggerPageBuild();
	}

	@Override
	public String getHeader(OgemaLocale locale) {
		return "Issue Setting History";
	}
	
	@Override
	public void addWidgetsAboveTable() {
		super.addWidgetsAboveTable();

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
		
		Button updateIssueEvaluationForSuperior = new Button(page, "updateIssueEvaluationForSuperior", "Update Eval for Superior") {
			@Override
			public void onPOSTComplete(String data, OgemaHttpRequest req) {
				controller.qualityEval.performEval(controller);
			}
		};
		topTable.setContent(0, 4, updateIssueEvaluationForSuperior);
		page.append(topTable);
	}
	
	@Override
	public void addWidgets(IssueHistoryItem object, ObjectResourceGUIHelper<IssueHistoryItem, Resource> vh, String id,
			OgemaHttpRequest req, Row row, ApplicationManager appMan) {
		addNameLabel(object, vh, id, row, req);
		//vh.stringLabel("ID", id, ""+object.id, row);
		vh.stringLabel("User", id, object.user, row);
		vh.stringLabel("Time", id, object.time, row);
		vh.stringLabel("DeviceId", id, object.deviceId, row);
		vh.stringLabel("Action", id, object.action, row);
		vh.stringLabel("New Value", id, object.newValue, row);
		vh.stringLabel("Old Value", id, object.previousValue, row);
	}

	@Override
	protected String getTypeName(OgemaLocale locale) {
		return "ID";
	}

	@Override
	protected String getLabel(IssueHistoryItem obj, OgemaHttpRequest req) {
		return String.format("%04d", obj.id);
	}

	@Override
	public Collection<IssueHistoryItem> getObjectsInTable(OgemaHttpRequest req) {
		List<IssueHistoryItem> result = new ArrayList<>();
		List<StringResource> all = history.getAllElements();
		for(StringResource sres: all) {
			String val = sres.getValue();
			List<String> vals = StringFormatHelper.getListFromString(val, "$,");
			IssueHistoryItem item = new IssueHistoryItem();
			try {
				try {
					item.id = Integer.parseInt(vals.get(0));
				} catch(NumberFormatException e) {}
				item.user = vals.get(1);
				item.time = vals.get(2);
				item.action = vals.get(3);
				item.newValue = vals.get(4);
				item.deviceId = vals.get(5);
				item.previousValue = vals.get(6);
				result.add(item);
			} catch(IndexOutOfBoundsException e) {
				appMan.getLogger().error("History(issue) element too short:"+sres.getLocation()+"  :  "+val);
			}
		}
		return result;
	}

	@Override
	public String getLineId(IssueHistoryItem object) {
		return String.format("%06d", 999999-object.id)+super.getLineId(object);
	}
}
