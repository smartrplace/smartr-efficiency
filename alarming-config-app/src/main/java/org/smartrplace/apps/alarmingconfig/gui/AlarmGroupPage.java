package org.smartrplace.apps.alarmingconfig.gui;

import java.util.Collection;

import org.ogema.accessadmin.api.ApplicationManagerPlus;
import org.ogema.core.application.ApplicationManager;
import org.ogema.devicefinder.api.AlarmOngoingGroup;
import org.ogema.devicefinder.util.AlarmingConfigUtil;
import org.ogema.model.extended.alarming.AlarmGroupData;
import org.ogema.model.recplay.testing.RecReplayAlarmingGroupData;
import org.smartrplace.gui.tablepages.ObjectGUITablePageNamed;
import org.smartrplace.util.directobjectgui.ObjectResourceGUIHelper;
import org.smartrplace.util.format.WidgetHelper;

import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.localisation.OgemaLocale;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.complextable.RowTemplate.Row;
import de.iwes.widgets.html.form.button.RedirectButton;

public class AlarmGroupPage extends ObjectGUITablePageNamed<AlarmOngoingGroup, RecReplayAlarmingGroupData> {
	protected final ApplicationManagerPlus appManPlus;
	
	public AlarmGroupPage(WidgetPage<?> page, ApplicationManagerPlus appManPlus) {
		super(page, appManPlus.appMan(), null);
		this.appManPlus = appManPlus;
		triggerPageBuild();
	}

	@Override
	protected String getHeader(OgemaLocale locale) {
		return "Groups of ongoing Alarms";
	}
	
	@Override
	public void addWidgets(AlarmOngoingGroup object,
			ObjectResourceGUIHelper<AlarmOngoingGroup, RecReplayAlarmingGroupData> vh, String id, OgemaHttpRequest req,
			Row row, ApplicationManager appMan) {
		addNameLabel(object, vh, id, row, req);
		if(req == null) {
			vh.registerHeaderEntry("Finished");
			vh.registerHeaderEntry("Base Alarms");
			vh.registerHeaderEntry("Type");
			vh.registerHeaderEntry("Started");
			vh.registerHeaderEntry("Comment_Analysis");
			vh.registerHeaderEntry("Analysis_Assigned");
			vh.registerHeaderEntry("Task Tracking");
			vh.registerHeaderEntry("Edit TT");
			vh.registerHeaderEntry("ID");
			return;
		}
		vh.stringLabel("Finished", id, ""+object.isFinished(), row);
		vh.stringLabel("Base Alarms", id, ""+object.baseAlarms().size(), row);
		vh.stringLabel("Type", id, ""+object.getType().label(null), row);
		AlarmGroupData res = object.getResource(false);
		if(res != null) {
			res.create();
			vh.timeLabel("Started", id, res.ongoingAlarmStartTime(), row, 0);
			vh.stringEdit("Comment_Analysis",  id, res.comment(), row, alert);
			/*Map<String, String> valuesToSet = new LinkedHashMap<>();
			String curVal = res.acceptedByUser().getValue();
			if(curVal != null && (!curVal.isEmpty()))
				valuesToSet.put(curVal, curVal);
			valuesToSet.put("None", "None");
			for(UserAccount user: appMan.getAdministrationManager().getAllUsers()) {
				if(curVal != null && user.getName().equals(curVal))
					continue;
				valuesToSet.put(user.getName(), user.getName());
			}
			vh.dropdown("Analysis_Assigned", id, res.acceptedByUser(), row, valuesToSet);*/
			vh.dropdown("Analysis_Assigned", id, res.assigned(), row, AlarmingConfigUtil.ASSIGNEMENT_ROLES);
			if(!res.linkToTaskTracking().getValue().isEmpty()) {
				RedirectButton taskLink = new RedirectButton(mainTable, "taskLink"+id, "Task Tracking",
						res.linkToTaskTracking().getValue(), req);
				row.addCell(WidgetHelper.getValidWidgetId("Task Tracking"), taskLink);
			}
			vh.stringEdit("Edit TT",  id, res.linkToTaskTracking(), row, alert);
		}
		vh.stringLabel("ID", id, object.id(), row);
	}

	@Override
	protected String getTypeName(OgemaLocale locale) {
		return "Name";
	}

	@Override
	protected String getLabel(AlarmOngoingGroup obj, OgemaHttpRequest req) {
		return obj.label(req.getLocale());
	}

	@Override
	public Collection<AlarmOngoingGroup> getObjectsInTable(OgemaHttpRequest req) {
		return appManPlus.dpService().alarming().getOngoingGroups(true);
	}

}
