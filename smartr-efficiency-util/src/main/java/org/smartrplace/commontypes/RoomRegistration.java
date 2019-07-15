package org.smartrplace.commontypes;

import java.util.LinkedHashMap;
import java.util.Map;

import org.smartrplace.efficiency.api.base.SmartEffResource;
import org.smartrplace.extensionservice.ExtensionResourceTypeDeclaration;
import org.smartrplace.smarteff.util.editgeneric.EditPageGenericWithTable;

import de.iwes.util.resource.ValueResourceHelper;
import de.iwes.widgets.api.widgets.localisation.OgemaLocale;
import extensionmodel.smarteff.api.common.BuildingData;
import extensionmodel.smarteff.api.common.BuildingUnit;

public class RoomRegistration {
	public static final Class<? extends SmartEffResource> TYPE_CLASS = BuildingUnit.class; //RoomData.class;
	
	public static final Map<String, String> TEMPFB_MAP_EN = new LinkedHashMap<>();
	public static final Map<String, String> TEMPFB_MAP_DE = new LinkedHashMap<>();
	static {
		TEMPFB_MAP_EN.put("1", "OK, no complaints");
		TEMPFB_MAP_EN.put("2", "Feels improved compared to earlier situations");
		TEMPFB_MAP_EN.put("10", "Too cold");
		TEMPFB_MAP_EN.put("11", "Too warm");
		TEMPFB_MAP_EN.put("12", "Too cold / warm mixed");
		TEMPFB_MAP_EN.put("100", "Heating does not seem to work at all");
		TEMPFB_MAP_EN.put("101", "Too hot: Heating working full power");
		
		TEMPFB_MAP_DE.put("1", "OK, keine Beschwerden");
		TEMPFB_MAP_DE.put("2", "Gegenüber vorherigem Zustand verbessert");
		TEMPFB_MAP_DE.put("10", "Zu kalt");
		TEMPFB_MAP_DE.put("11", "Zu warm");
		TEMPFB_MAP_DE.put("12", "Wechselnd zu kalt/zu warm");
		TEMPFB_MAP_DE.put("100", "Kalt - Heizung funktioniert gar nicht");
		TEMPFB_MAP_DE.put("101", "Zu heiß - Heizung läuft immer voll");

	}
	
	public static class TypeDeclaration implements ExtensionResourceTypeDeclaration<SmartEffResource> {

		@Override
		public Class<? extends SmartEffResource> dataType() {
			return TYPE_CLASS;
		}

		@Override
		public String label(OgemaLocale locale) {
			return "Room Data";
		}

		@Override
		public Class<? extends SmartEffResource> parentType() {
			return BuildingData.class;
		}

		@Override
		public Cardinality cardinality() {
			return Cardinality.MULTIPLE_OPTIONAL;
		}
	}
	
	public static class EditPage extends EditPageGenericWithTable<BuildingUnit> {
		@Override
		public void setData(BuildingUnit sr) {
			setLabel(sr.name(), EN, "Name",
					DE, "Voller Name");
			setTableHeader(sr.name(), EN, "Name");
			setLabel(sr.groundArea(), EN, "Ground Area",
					DE, "Nutzfläche");
			setTableHeader(sr.groundArea(), EN, "Ground Area (m2)", DE, "Nutzfläche (m2)");
			setLabel(sr.totalOutsideWallArea(), EN, "Total area of outside walls (m2)", DE ,"Gesamtfläche Außenwände (m2)");
			setLabel(sr.outsideWindowArea(), EN, "Total area of windows in outside walls (m2)", DE, "Gesamt-Fensterfläche (nur Außenwände) in m2");
			setLabel(sr.roomHeight(),
					EN, "Room specific height. Set to 0 to use buidling default.",
					DE, "Raumhöhe falls abweichend von der Standardhöhe im Gebäude");
			setLabelWithUnit(sr.comfortTemperature(),
					EN, "Room specific comfort temperature. Set to 0 to use buidling default.",
					DE, "Komfort-Temperature falls abweichend von der Standardtemperature im Gebäude");
			setLabelWithUnit(sr.basementShare(),
					EN, "Share of room floor that connects to the lower building envelope",
					DE, "Anteil des Bodens am Keller");
			setLabelWithUnit(sr.ceilingShare(),
					EN, "Share of room ceiling that connects to the upper building envelope",
					DE, "Anteil der Decke am Dach");
			
			
			setLabel(sr.manualTemperatureReading(), EN, "Manual temperature reading (°C)");
			setLabel(sr.manualHumidityReading(), EN, "Manual humidity reading (%)");
			setLabel(sr.roomTemperatureQualityRating(), EN, "Room temperature comfort level user feedback",
					DE, "Nutzer-Feedback zur Raumtemperatur");
			setDisplayOptions(sr.roomTemperatureQualityRating(), EN, TEMPFB_MAP_EN);
			setDisplayOptions(sr.roomTemperatureQualityRating(), DE, TEMPFB_MAP_DE);
			
			setLabel(sr.heatRadiator(), EN, "Radiators in the room", DE, "Heizköper im Raum verwalten");
			setLabel(sr.window(), EN, "Windows in the room", DE, "Fenster im Raum verwalten");
		}

		@SuppressWarnings("unchecked")
		@Override
		protected Class<BuildingUnit> primaryEntryTypeClass() {
			return (Class<BuildingUnit>) TYPE_CLASS;
		}
		
		@Override
		protected String getMaintainer() {
			return "test1";
		}
		
		@Override
		public boolean checkResource(BuildingUnit res) {
			return (ValueResourceHelper.setIfNew(res.roomHeight(), 2.8f))
				| super.checkResource(res);
		}
	}
}
