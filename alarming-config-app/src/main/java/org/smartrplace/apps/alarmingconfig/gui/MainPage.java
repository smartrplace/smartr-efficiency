package org.smartrplace.apps.alarmingconfig.gui;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.ogema.core.application.ApplicationManager;
import org.ogema.core.model.Resource;
import org.ogema.core.model.simple.FloatResource;
import org.ogema.core.model.simple.IntegerResource;
import org.ogema.tools.resource.util.ResourceUtils;
import org.smartrplace.apps.alarmingconfig.mgmt.AlarmingManager;
import org.smartrplace.util.directobjectgui.ObjectGUITablePage;
import org.smartrplace.util.directobjectgui.ObjectResourceGUIHelper;

import de.iwes.util.resource.ResourceHelper;
import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.html.StaticTable;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.complextable.RowTemplate.Row;
import de.iwes.widgets.html.form.button.Button;
import de.iwes.widgets.html.form.button.ButtonData;
import de.iwes.widgets.html.form.button.WindowCloseButton;
import de.iwes.widgets.html.form.label.Header;
import extensionmodel.smarteff.monitoring.AlarmConfigBase;


/**
 * An HTML page, generated from the Java code.
 */
public class MainPage extends ObjectGUITablePage<AlarmConfigBase, AlarmConfigBase>  {

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
	

	public MainPage(WidgetPage<?> page, ApplicationManager appMan, Resource baseResource) {
		super(page, appMan, ResourceHelper.getSampleResource(AlarmConfigBase.class));
		this.baseResource = baseResource;
		appMan.getLogger().info("Alarming Config page created at {}", page.getFullUrl());
	}


	@Override
	public void addWidgets(AlarmConfigBase sr, ObjectResourceGUIHelper<AlarmConfigBase, AlarmConfigBase> vh,
			String id, OgemaHttpRequest req, Row row, ApplicationManager appMan) {

		if(req == null)
			vh.registerHeaderEntry("Name");
		else {
			vh.stringLabel( "Name", id, ResourceUtils.getHumanReadableShortName(sr), row);
		}
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
				id, sr.maxIntervalBetweenNewValues(), row, alert,
				Float.MIN_VALUE, Float.MAX_VALUE, "");
		vh.booleanEdit("Monitoring Switch active", id, sr.performAdditinalOperations(), row);
		if(req == null)
			vh.registerHeaderEntry("Status");
		else {
			FloatResource res = (FloatResource) sr.supervisedSensor().reading().getLocationResource();
			IntegerResource statusRes = AlarmingManager.getAlarmStatus(res);
			if(statusRes == null)
				return;
			vh.intLabel("Status", id, statusRes, row, 0);
		}
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
		return arg0;
	}
}