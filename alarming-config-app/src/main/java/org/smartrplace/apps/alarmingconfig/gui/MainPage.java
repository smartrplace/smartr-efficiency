package org.smartrplace.apps.alarmingconfig.gui;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.ogema.core.application.ApplicationManager;
import org.ogema.core.model.Resource;
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


/**
 * An HTML page, generated from the Java code.
 */
public class MainPage extends ObjectGUITablePage<AlarmConfigBase, AlarmConfigBase>  {
	/*public final long UPDATE_RATE = 5*1000;
	
	protected final AlarmingConfigAppController controller;
	
	public MainPage(final WidgetPage<?> page, final AlarmingConfigAppController controller) {

		this.controller = controller;
		
		Header header = new Header(page, "header", "User and room access administration");
		header.addDefaultStyle(HeaderData.TEXT_ALIGNMENT_CENTERED);
		page.append(header).linebreak();
		
		Alert alert = new Alert(page, "alert", "");
		alert.setDefaultVisibility(false);
		page.append(alert).linebreak();
		
	}*/


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
	

	public MainPage(WidgetPage<?> page, ApplicationManager
	appMan, AlarmConfigBase initSampleObject, Resource baseResource) {
		super(page, appMan, initSampleObject);
		this.baseResource = baseResource;
	}


	@Override
	public void addWidgets(AlarmConfigBase alarm, ObjectResourceGUIHelper<AlarmConfigBase, AlarmConfigBase> vh,
			String id, OgemaHttpRequest req, Row row, ApplicationManager appMan) {
		if (null == alarm) return;
		// TODO add relevant widgets
		vh.stringLabel(id, alarm.getPath("_"), "Alarm Config for " + alarm.supervisedSensor().getPath(), row);
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

	
	/*
	@Override
	public String label(OgemaLocale locale) {
		return "Alarm limit configuration";
	}
	
	@Override
	public Class<AlarmConfigBase> primaryEntryTypeClass() {
		return AlarmConfigBase.class;
	}
	
	@Override
	public void setData(AlarmConfigBase sr) {
		setLabel("#humanreadableshort", EN, "Name");
		if(System.getProperty("org.smartrplace.smarteff.util.editgeneric.fixedlanguage", "german").equals("english")) {
			setLabel(sr.sendAlarm(), EN, "Alarm active");
			setTableHeader(sr.sendAlarm(), EN, "active", FORMAT, "edit");
			setLabel(sr.lowerLimit(), EN, "Lower limit operation range");
			setTableHeader(sr.lowerLimit(), EN, "Lower limit", FORMAT, "edit");
			setLabel(sr.upperLimit(), EN, "Upper limit operation range");
			setTableHeader(sr.upperLimit(), EN, "Upper limit", FORMAT, "edit");
			setLabel(sr.maxViolationTimeWithoutAlarm(), EN, "Delay until the alarm is triggered (minutes)");
			setTableHeader(sr.maxViolationTimeWithoutAlarm(), EN, "Delay (min)", FORMAT, "edit");
			setLabel(sr.alarmLevel(), EN, "Priority");
			setTableHeader(sr.alarmLevel(), EN, "Expert Level", FORMAT, "edit");
			setDisplayOptions(sr.alarmLevel(), EN, ALARM_LEVEL_EN);
			setLabel(sr.alarmRepetitionTime(), EN, "Duration Blocking Sending the same alarm (minutes)");
			setLabel(sr.maxIntervalBetweenNewValues(), EN, "Maximale Dauer bis neuer Wert empfangen wird");
			setTableHeader(sr.maxIntervalBetweenNewValues(), EN, "Maximum duration until new value is received (min)", FORMAT, "edit");
			setLabel(sr.performAdditinalOperations(), EN, "Monitoring Switch active");
			setTableHeader("#TableHeader", EN, "Alarms for ");
			return;
		}
		setLabel(sr.sendAlarm(), EN, "Alarm aktiv");
		setTableHeader(sr.sendAlarm(), EN, "aktiv", FORMAT, "edit");
		setLabel(sr.lowerLimit(), EN, "Untere Grenze Normalbereich");
		setTableHeader(sr.lowerLimit(), EN, "Grenze Unten", FORMAT, "edit");
		setLabel(sr.upperLimit(), EN, "Obere Grenze Normalbereich");
		setTableHeader(sr.upperLimit(), EN, "Grenze Oben", FORMAT, "edit");
		setLabel(sr.maxViolationTimeWithoutAlarm(), EN, "Verzögerung bis zum Auslösen des Alarms (Minuten)");
		setTableHeader(sr.maxViolationTimeWithoutAlarm(), EN, "Verzögerung (min)", FORMAT, "edit");
		setLabel(sr.alarmLevel(), EN, "Priorität");
		setTableHeader(sr.alarmLevel(), EN, "Level", FORMAT, "edit");
		setDisplayOptions(sr.alarmLevel(), EN, ALARM_LEVEL_EN);
		setLabel(sr.alarmRepetitionTime(), EN, "Dauer Blockierung Senden des gleichen Alarms (Minuten)");
		setLabel(sr.maxIntervalBetweenNewValues(), EN, "Maximale Dauer bis neuer Wert empfangen wird");
		setTableHeader(sr.maxIntervalBetweenNewValues(), EN, "Max-Intervall (min) negativ:inaktiv", FORMAT, "edit");
		setLabel(sr.performAdditinalOperations(), EN, "Überwachung Schalter aktiv");
		setTableHeader("#TableHeader", EN, "Alarme für ");
	}
	
	@Override
	protected void defaultValues(AlarmConfigBase data, DefaultSetModes mode) {
		setDefaultValuesStatic(data, mode);
	}
	
	public static void setDefaultValuesStatic(AlarmConfigBase data, DefaultSetModes mode) {
		setDefault(data.alarmLevel(), 1, mode);
		setDefault(data.alarmRepetitionTime(), 60, mode);
		setDefault(data.maxViolationTimeWithoutAlarm(), 10, mode);
		setDefault(data.lowerLimit(), 0, mode);
		setDefault(data.upperLimit(), 100, mode);
		setDefault(data.maxIntervalBetweenNewValues(), -1, mode);
		setDefault(data.sendAlarm(), false, mode);
		if(data.supervisedSensor().exists() &&
				AlarmingUtil.getSwitchFromSensor(data.supervisedSensor()) != null) {
			setDefault(data.performAdditinalOperations(), true, mode);
		} else
			setDefault(data.performAdditinalOperations(), false, mode);
	}
	

	@Override
	public boolean offerDeleteInTable() {
		return false;
	}
	
	public static interface AlarmingUpdater {
		void updateAlarming();
	}
	
	//Set this to enable alarming updates
	public static AlarmingUpdater alarmingUpdater = null;
	
	@Override
	public void changeMenuConfig(MenuConfiguration mc) {
		mc.setLanguageSelectionVisible(false);
		mc.setNavigationVisible(false);
	}
	*/
}
