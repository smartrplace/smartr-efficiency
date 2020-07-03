package org.sp.smarteff.monitoring.alarming;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.ogema.core.application.ApplicationManager;
import org.ogema.core.model.Resource;
import org.ogema.tools.resource.util.ResourceUtils;
import org.smartrplace.smarteff.util.editgeneric.EditPageGeneric.DefaultSetModes;
import org.smartrplace.util.directobjectgui.ObjectGUITablePage;
import org.smartrplace.util.directobjectgui.ObjectResourceGUIHelper;

import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.html.StaticTable;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.complextable.RowTemplate.Row;
import de.iwes.widgets.html.form.button.Button;
import de.iwes.widgets.html.form.button.ButtonData;
import de.iwes.widgets.html.form.button.WindowCloseButton;
import de.iwes.widgets.html.form.label.Header;
import extensionmodel.smarteff.monitoring.AlarmConfigBase;

public class StandAloneAlarmingEditPage extends ObjectGUITablePage<AlarmConfigBase, AlarmConfigBase>  {

	public static final Map<String, String> ALARM_LEVEL_EN = new HashMap<>();
	static {
		ALARM_LEVEL_EN.put("0", "Keine Alarme");
		ALARM_LEVEL_EN.put("1", "Normal");
		ALARM_LEVEL_EN.put("2", "Hoch");
	}
	public static interface AlarmingUpdater {
		void updateAlarming();
	}
	
	//Set this to enable alarming updates
	public static AlarmingUpdater alarmingUpdater = null;
	
	private final Resource baseResource;
	

	public StandAloneAlarmingEditPage(WidgetPage<?> page, ApplicationManager
	appMan, AlarmConfigBase initSampleObject, Resource baseResource) {
		super(page, appMan, initSampleObject);
		this.baseResource = baseResource;
	}


	@Override
	public void addWidgets(AlarmConfigBase sr, ObjectResourceGUIHelper<AlarmConfigBase, AlarmConfigBase> vh,
			String id, OgemaHttpRequest req, Row row, ApplicationManager appMan) {
		if (null == sr) return;
		// TODO add relevant widgets
		vh.stringLabel( "Name", id, ResourceUtils.getHumanReadableShortName(sr), row);
		vh.booleanEdit("Alarm active", id, sr.sendAlarm(), row);
		vh.floatEdit("Lower Limit",
				id, sr.lowerLimit(), row, alert,
				Float.MIN_VALUE, Float.MAX_VALUE, "");
		vh.floatEdit("Upper Limit",
				id, sr.upperLimit(), row, alert,
				Float.MIN_VALUE, Float.MAX_VALUE, "");
		vh.floatEdit("Delay until the alarm is triggered (minutes)",
				id, sr.maxViolationTimeWithoutAlarm(), row, alert,
				Float.MIN_VALUE, Float.MAX_VALUE, "");
		vh.dropdown("Priority", id, sr.alarmLevel(), row, ALARM_LEVEL_EN);
		vh.floatEdit("Duration Blocking Sending the same alarm (minutes)",
				id, sr.alarmRepetitionTime(), row, alert,
				Float.MIN_VALUE, Float.MAX_VALUE, "");
		vh.floatEdit("Maximum duration until new value is received (min)",
				id, sr.alarmRepetitionTime(), row, alert,
				Float.MIN_VALUE, Float.MAX_VALUE, "");
		vh.booleanEdit("Monitoring Switch active", id, sr.performAdditinalOperations(), row);
	}


	@Override
	public void addWidgetsAboveTable() {
		Header h = new Header(page, "header", "Alarming Configuration");
		page.append(h);

		WindowCloseButton closeTabButton = new WindowCloseButton(page, "closeTabButtonBuilding",
				System.getProperty("org.ogema.app.navigation.closetabbuttontext", "Fertig")) {
			private static final long serialVersionUID = 1L;
			@Override
			public void onPrePOST(String data, OgemaHttpRequest req) {
				if(alarmingUpdater != null) {
					alarmingUpdater.updateAlarming();
				}
			}
		};
		closeTabButton.addDefaultStyle(ButtonData.BOOTSTRAP_RED);

		Button saveButton = new Button(page, "saveButton",
				System.getProperty("org.ogema.app.navigation.alarmchangesactivate", "Speichern")) {
			private static final long serialVersionUID = 1L;
			@Override
			public void onPrePOST(String data, OgemaHttpRequest req) {
				if(alarmingUpdater != null) {
					alarmingUpdater.updateAlarming();
				}
			}
		};
		closeTabButton.addDefaultStyle(ButtonData.BOOTSTRAP_GREEN);
		
		StaticTable topTable = new StaticTable(1, 2);
		topTable.setContent(0, 0, closeTabButton).setContent(0, 1, saveButton);
		
		page.append(topTable);
	}


	@Override
	public Collection<AlarmConfigBase> getObjectsInTable(OgemaHttpRequest arg0) {
		return baseResource.getSubResources(AlarmConfigBase.class, true);
	}


	@Override
	public AlarmConfigBase getResource(AlarmConfigBase arg0, OgemaHttpRequest arg1) {
		// TODO Auto-generated method stub
		return null;
	}


}
