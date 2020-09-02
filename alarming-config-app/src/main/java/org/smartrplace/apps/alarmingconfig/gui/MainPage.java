package org.smartrplace.apps.alarmingconfig.gui;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.ogema.accessadmin.api.ApplicationManagerPlus;
import org.ogema.core.application.ApplicationManager;
import org.ogema.core.model.ValueResource;
import org.ogema.core.model.simple.FloatResource;
import org.ogema.core.model.simple.IntegerResource;
import org.ogema.devicefinder.api.AlarmingExtension;
import org.ogema.devicefinder.api.Datapoint;
import org.ogema.devicefinder.api.DatapointService;
import org.ogema.model.extended.alarming.AlarmConfiguration;
import org.ogema.tools.resource.util.ValueResourceUtils;
import org.smartrplace.apps.alarmingconfig.mgmt.AlarmingManager;
import org.smartrplace.gui.tablepages.PerMultiselectConfigPage;
import org.smartrplace.util.directobjectgui.ObjectResourceGUIHelper;

import de.iwes.util.resource.ResourceHelper;
import de.iwes.util.resource.ValueResourceHelper;
import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.html.StaticTable;
import de.iwes.widgets.api.widgets.localisation.OgemaLocale;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.complextable.RowTemplate.Row;
import de.iwes.widgets.html.form.button.Button;
import de.iwes.widgets.html.form.button.ButtonData;
import de.iwes.widgets.html.form.button.WindowCloseButton;


/**
 * An HTML page, generated from the Java code.
 */
public class MainPage extends PerMultiselectConfigPage<AlarmConfiguration, AlarmingExtension, AlarmConfiguration>  {

	public static final Map<String, String> ALARM_LEVEL_EN = new HashMap<>();
	static {
		ALARM_LEVEL_EN.put("0", "No Alarms");
		ALARM_LEVEL_EN.put("1", "Low");
		ALARM_LEVEL_EN.put("2", "Normal");
		ALARM_LEVEL_EN.put("3", "High");
	}
	public static interface AlarmingUpdater {
		void updateAlarming();
	}
	
	//Set this to enable alarming updates
	public static AlarmingUpdater alarmingUpdater = null;
	
	//private final Resource baseResource;
	protected final ApplicationManagerPlus appManPlus;
	protected final DatapointService dpService;
	

	public MainPage(WidgetPage<?> page, ApplicationManagerPlus appManPlus) { //, Resource baseResource) {
		super(page, appManPlus.appMan(), ResourceHelper.getSampleResource(AlarmConfiguration.class));
		//this.baseResource = baseResource;
		this.appManPlus = appManPlus;
		this.dpService = appManPlus.dpService();
		appMan.getLogger().info("Alarming Config page created at {}", page.getFullUrl());
		triggerPageBuild();
	}


	@Override
	protected void addWidgetsBeforeMultiSelect(AlarmConfiguration sr,
			ObjectResourceGUIHelper<AlarmConfiguration, AlarmConfiguration> vh, String id, OgemaHttpRequest req,
			Row row, ApplicationManager appMan) {
	//@Override
	//public void addWidgets(AlarmConfiguration sr, ObjectResourceGUIHelper<AlarmConfiguration, AlarmConfiguration> vh,
	//		String id, OgemaHttpRequest req, Row row, ApplicationManager appMan) {

		//if(req == null)
		//	vh.registerHeaderEntry("Name");
		//else {
		//	vh.stringLabel( "Name", id, ResourceUtils.getHumanReadableShortName(sr), row);
		//}
		if(sr.sensorVal() instanceof FloatResource) 
			vh.floatLabel("Measured", id, (FloatResource) sr.sensorVal(), row, "%.1f");
		else {
			String text;
			if(sr.sensorVal().isActive()) { // && sr.supervisedSensor().reading() instanceof SingleValueResource) {
				try {
					text = String.format("%.1f",
						ValueResourceUtils.getFloatValue(sr.sensorVal()));
				} catch(Exception e) {
					text = "Ex!";
				}
			} else
				text = "--";
			vh.stringLabel("Measured", id, text, row);
		}
		vh.booleanEdit("Alarm active", id, sr.sendAlarm(), row);
		vh.floatEdit("Lower Limit",
				id, sr.lowerLimit(), row, alert,
				-Float.MAX_VALUE, Float.MAX_VALUE, "");
		vh.floatEdit("Upper Limit",
				id, sr.upperLimit(), row, alert,
				-Float.MAX_VALUE, Float.MAX_VALUE, "");
		vh.floatEdit("Delay until the alarm is triggered (minutes)",
				id, sr.maxViolationTimeWithoutAlarm(), row, alert,
				-Float.MAX_VALUE, Float.MAX_VALUE, "");
		vh.dropdown("Priority", id, sr.alarmLevel(), row, ALARM_LEVEL_EN);
		vh.floatEdit("Duration Blocking Sending the same alarm (minutes)",
				id, sr.alarmRepetitionTime(), row, alert,
				-Float.MAX_VALUE, Float.MAX_VALUE, "");
		vh.floatEdit("Maximum duration until new value is received (min)",
				id, sr.maxIntervalBetweenNewValues(), row, alert,
				-Float.MAX_VALUE, Float.MAX_VALUE, "");
		vh.booleanEdit("Monitoring Switch active", id, sr.performAdditinalOperations(), row);
		if(req == null)
			vh.registerHeaderEntry("Status");
		else {
			ValueResource res = sr.sensorVal().getLocationResource();
			IntegerResource statusRes = AlarmingManager.getAlarmStatus(res);
			if(statusRes == null)
				return;
			vh.intLabel("Status", id, statusRes, row, 0);
		}
	}


	@Override
	protected String getHeader(OgemaLocale locale) {
		return "1. Alarming Configuration";
	}
	
	@Override
	public void addWidgetsAboveTable() {
		super.addWidgetsAboveTable();
		//Header h = new Header(page, "header", "Alarming Configuration");
		//page.append(h);

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
				System.getProperty("org.ogema.app.navigation.alarmchangesactivate", "Neustart Alarming (Änderungen werden aktiv)")) {
			private static final long serialVersionUID = 1L;
			@Override
			public void onPrePOST(String data, OgemaHttpRequest req) {
				if(alarmingUpdater != null) {
					alarmingUpdater.updateAlarming();
					alert.showAlert("Restarted alarming", true, req);
				} else
					alert.showAlert("Could not find alarmingManagement for update", false, req);
			}
		};
		closeTabButton.addDefaultStyle(ButtonData.BOOTSTRAP_GREEN);
		
		StaticTable topTable = new StaticTable(1, 2);
		topTable.setContent(0, 0, closeTabButton).setContent(0, 1, saveButton);
		
		page.append(topTable);
	}


	@Override
	public Collection<AlarmConfiguration> getObjectsInTable(OgemaHttpRequest arg0) {
		return appMan.getResourceAccess().getResources(AlarmConfiguration.class);
		//return baseResource.getSubResources(AlarmConfiguration.class, true);
	}


	@Override
	public AlarmConfiguration getResource(AlarmConfiguration arg0, OgemaHttpRequest arg1) {
		return arg0;
	}


	@Override
	protected String getGroupColumnLabel() {
		return "Extensions";
	}


	@Override
	protected Collection<AlarmingExtension> getAllGroups(OgemaHttpRequest req) {
		return appManPlus.dpService().alarming().getAlarmingExtensions();
	}


	@Override
	protected List<AlarmingExtension> getGroups(AlarmConfiguration object, OgemaHttpRequest req) {
		List<AlarmingExtension> result = new ArrayList<>();
		for(String id: object.alarmingExtensions().getValues()) {
			AlarmingExtension ext = appManPlus.dpService().alarming().getAlarmingExtension(id);
			if(ext != null)
				result.add(ext);
		}
		return result;
	}


	@Override
	protected void setGroups(AlarmConfiguration object, List<AlarmingExtension> groups, OgemaHttpRequest req) {
		String[] result = new String[groups.size()];
		for(int i=0; i<groups.size(); i++)
			result[i] = groups.get(i).id();
		ValueResourceHelper.setCreate(object.alarmingExtensions(), result);
	}


	@Override
	protected String getGroupLabel(AlarmingExtension object, OgemaLocale locale) {
		return object.label(locale);
	}


	@Override
	protected String getTypeName(OgemaLocale locale) {
		return "Datapoint";
	}


	@Override
	protected String getLabel(AlarmConfiguration obj) {
		//InstallAppDevice dev = ResourceHelper.getFirstParentOfType(obj, InstallAppDevice.class);
		//if(dev == null)
		//	return ResourceUtils.getHumanReadableShortName(obj);
		Datapoint dp = getDatapoint(obj, dpService);
		return dp.label(null);
	}
	
	public static Datapoint getDatapoint(AlarmConfiguration ac, DatapointService dpService) {
		return dpService.getDataPointStandard(ac.sensorVal().getLocation());
	}
}