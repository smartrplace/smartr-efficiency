package org.smartrplace.apps.hw.install.gui.alarm;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.ogema.core.application.ApplicationManager;
import org.ogema.core.model.simple.IntegerResource;
import org.ogema.devicefinder.api.TimedJobMemoryData;
import org.ogema.tools.resource.util.ResourceUtils;
import org.ogema.util.timedjob.TimedJobMemoryDataImpl;
import org.smartrplace.alarming.escalation.model.AlarmingEscalationLevel;
import org.smartrplace.alarming.escalation.model.AlarmingMessagingApp;
import org.smartrplace.apps.alarmingconfig.AlarmingConfigAppController;
import org.smartrplace.apps.alarmingconfig.gui.MainPage;
import org.smartrplace.gui.tablepages.PerMultiselectConfigPage;
import org.smartrplace.util.directobjectgui.ObjectResourceGUIHelper;

import de.iwes.util.resource.ResourceHelper;
import de.iwes.util.resourcelist.ResourceListHelper;
import de.iwes.util.timer.AbsoluteTiming;
import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.html.StaticTable;
import de.iwes.widgets.api.widgets.localisation.OgemaLocale;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.buttonconfirm.ButtonConfirm;
import de.iwes.widgets.html.complextable.RowTemplate.Row;
import de.iwes.widgets.html.form.button.Button;
import de.iwes.widgets.html.form.label.Header;
import de.iwes.widgets.resource.widget.dropdown.ValueResourceDropdown;

@SuppressWarnings("serial")
public class AlarmingLevelPage extends PerMultiselectConfigPage<AlarmingEscalationLevel, AlarmingMessagingApp, AlarmingEscalationLevel> {
	protected final AlarmingConfigAppController controller;
	
	public AlarmingLevelPage(WidgetPage<?> page, AlarmingConfigAppController controller) {
		super(page, controller.appMan, ResourceHelper.getSampleResource(AlarmingEscalationLevel.class));
		this.controller = controller;
		triggerPageBuild();
	}

	@Override
	protected void addWidgetsBeforeMultiSelect(AlarmingEscalationLevel object,
			ObjectResourceGUIHelper<AlarmingEscalationLevel, AlarmingEscalationLevel> vh, String id,
			OgemaHttpRequest req, Row row, ApplicationManager appMan) {
		if(req == null) {
			vh.registerHeaderEntry("Active");
			vh.registerHeaderEntry("Interval (min)");
			vh.registerHeaderEntry("Aligned interval");
			//vh.registerHeaderEntry("Init run after");
			vh.registerHeaderEntry("Priority");
			vh.registerHeaderEntry("Blocked until");
			vh.registerHeaderEntry("Delay(generic minutes)");
			return;
		}
		
		String idProv = ResourceListHelper.getNameForElement(object.timedJobData().getLocationResource());
		TimedJobMemoryData tdata = controller.dpService.timedJobService().getProvider(idProv);
		Button activeBut = TimedJobMemoryDataImpl.getTimedJobStatusButton(tdata, mainTable, id, req, alert);
		row.addCell("Active", activeBut);
		//vh.booleanEdit("Active", id, object.isProviderActive(), row);
		vh.floatEdit("Interval (min)", id, object.timedJobData().interval(), row, alert, 0, Float.MAX_VALUE, "Negative values not allowed!", 0);
		vh.dropdown("Aligned interval", id, object.timedJobData().alignedInterval(), row, AbsoluteTiming.INTERVAL_NAME_MAP);
		//vh.floatEdit("Init run after", id, object.timedJobData().performOperationOnStartUpWithDelay(), row, alert,
		//		-1, Float.MAX_VALUE, "Values below -1 not allowed!");
		vh.dropdown("Priority", id, object.alarmLevel(), row, MainPage.ALARM_LEVEL_EN);
		vh.timeLabel("Blocked until", id, object.blockedUntil(), row, 0);
		vh.timeEdit("Delay(generic minutes)", id, object.standardDelay(), row, alert, 0, Long.MAX_VALUE,
				"Outside limit!", 2);
	}

	@Override
	public AlarmingEscalationLevel getResource(AlarmingEscalationLevel object, OgemaHttpRequest req) {
		return object;
	}

	@Override
	public void addWidgetsAboveTable() {
		Header header = new Header(page, "header", "7. Alarming Escalation Levels");
		page.append(header);
		
		ValueResourceDropdown<IntegerResource> reductionModeDrop = new ValueResourceDropdown<IntegerResource>(page, "reductionModeDrop",
				controller.hwTableData.appConfigData.alarmingReductionLevel(),
				Arrays.asList(new String[] {"Off-season", "Standard"}));
		reductionModeDrop.setDefaultIntegerValuesToUse(Arrays.asList(new Integer[] {-1, 0}));
		
		ButtonConfirm releaseAllUnassigned = new ButtonConfirm(page, "releaseAllUnBut") {
			@Override
			public void onPOSTComplete(String data, OgemaHttpRequest req) {
				DeviceKnownFaultsPage.releaseAllUnassigned(controller.dpService, appMan.getFrameworkTime());
			}
		};
		releaseAllUnassigned.setDefaultText("Release all Unassigned");
		releaseAllUnassigned.setDefaultConfirmMsg("Really release all known issues that are not assigned?");
		
		StaticTable topTable = new StaticTable(1, 4);
		topTable.setContent(0, 0, "Alarming Escalation Message mode:").setContent(0, 1, reductionModeDrop);
		topTable.setContent(0, 3, releaseAllUnassigned);
		page.append(topTable);
	}

	@Override
	public Collection<AlarmingEscalationLevel> getObjectsInTable(OgemaHttpRequest req) {
		return controller.hwTableData.appConfigData.escalation().levelData().getAllElements();
	}

	@Override
	protected String getGroupColumnLabel() {
		return "Destination Groups (M-Apps)";
	}

	@Override
	protected Collection<AlarmingMessagingApp> getAllGroups(AlarmingEscalationLevel object, OgemaHttpRequest req) {
		return controller.hwTableData.appConfigData.escalation().messagigApps().getAllElements();
	}

	@Override
	protected List<AlarmingMessagingApp> getGroups(AlarmingEscalationLevel object, OgemaHttpRequest req) {
		return ResourceListHelper.getAllElementsLocation(object.messagingApps());
	}

	@Override
	protected void setGroups(AlarmingEscalationLevel object, List<AlarmingMessagingApp> groups, OgemaHttpRequest req) {
		ResourceListHelper.setListToReferences(object.messagingApps(), groups, true);
	}

	@Override
	protected String getGroupLabel(AlarmingMessagingApp object, OgemaLocale locale) {
		return ResourceUtils.getHumanReadableShortName(object);
	}

	@Override
	protected String getTypeName(OgemaLocale locale) {
		return "Provider ID";
	}

	@Override
	protected String getLabel(AlarmingEscalationLevel obj, OgemaHttpRequest req) {
		return ResourceUtils.getHumanReadableShortName(obj);
	}

}
