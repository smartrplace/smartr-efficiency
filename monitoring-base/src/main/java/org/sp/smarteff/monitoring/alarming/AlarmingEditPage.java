package org.sp.smarteff.monitoring.alarming;

import java.util.HashMap;
import java.util.Map;

import org.ogema.core.model.Resource;
import org.smartrplace.smarteff.util.editgeneric.EditPageGenericWithTable;
import org.smartrplace.smarteff.util.editgeneric.GenericResourceByTypeTablePage;

import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.html.StaticTable;
import de.iwes.widgets.api.widgets.localisation.OgemaLocale;
import de.iwes.widgets.api.widgets.navigation.MenuConfiguration;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.form.button.Button;
import de.iwes.widgets.html.form.button.ButtonData;
import de.iwes.widgets.html.form.button.WindowCloseButton;
import extensionmodel.smarteff.monitoring.AlarmConfigBase;

public class AlarmingEditPage extends EditPageGenericWithTable<AlarmConfigBase> {
	public static final Map<String, String> ALARM_LEVEL_EN = new HashMap<>();
	static {
		ALARM_LEVEL_EN.put("0", "Keine Alarme");
		ALARM_LEVEL_EN.put("1", "Normal");
		ALARM_LEVEL_EN.put("2", "Hoch");
	}

	
	@Override
	public String label(OgemaLocale locale) {
		return "Alarm limit configuration";
	}
	
	/*public boolean checkResource(SmartrHeatingData data) {
		if(!checkResourceBase(data, false)) return false;
		String newName = CapabilityHelper.getnewDecoratorName("SmartrHeatingProject", data.getParent());
		ValueResourceHelper.setIfNew(data.name(), newName);
		return true;
	}*/

	@Override
	public Class<AlarmConfigBase> primaryEntryTypeClass() {
		return AlarmConfigBase.class;
	}
	
	@Override
	public void setData(AlarmConfigBase sr) {
		setLabel("#humanreadableshort", EN, "Name");
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
	public boolean addWidgetsAboveTable(Class<? extends Resource> resourceType, WidgetPage<?> page,
			GenericResourceByTypeTablePage<AlarmConfigBase> genericResourceByTypeTablePage) {
		WindowCloseButton closeTabButton = new WindowCloseButton(page, "closeTabButtonBuilding", "Fertig") {
			private static final long serialVersionUID = 1L;
			@Override
			public void onPrePOST(String data, OgemaHttpRequest req) {
				if(alarmingUpdater != null) {
					alarmingUpdater.updateAlarming();
				}
			}
		};
		closeTabButton.addDefaultStyle(ButtonData.BOOTSTRAP_RED);

		Button saveButton = new Button(page, "saveButton", "Speichern") {
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
		return true;
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
}
