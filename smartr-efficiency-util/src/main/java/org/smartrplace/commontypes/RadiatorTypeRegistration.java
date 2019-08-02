package org.smartrplace.commontypes;

import java.util.HashMap;
import java.util.Map;

import org.smartrplace.efficiency.api.base.SmartEffResource;
import org.smartrplace.extensionservice.ExtensionResourceTypeDeclaration;
import org.smartrplace.smarteff.util.editgeneric.EditPageGenericWithTable;

import de.iwes.widgets.api.widgets.localisation.OgemaLocale;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import extensionmodel.smarteff.api.common.BuildingData;
import extensionmodel.smarteff.api.common.HeatRadiatorType;

public class RadiatorTypeRegistration {
	public static final Class<? extends SmartEffResource> TYPE_CLASS = HeatRadiatorType.class;
	
	public static final Map<String, String> THTMAP_EN = new HashMap<>();
	public static final Map<String, String> RADTYPEMAP_EN = new HashMap<>();
	static {
		THTMAP_EN.put("1", "Standard on radiators");
		THTMAP_EN.put("2", "Control knob connected via pressure cable");
		THTMAP_EN.put("3", "Room control device");
		THTMAP_EN.put("4", "Part of building automation system");

		RADTYPEMAP_EN.put("0", "hot-water powered radiator for walls (default)");
		RADTYPEMAP_EN.put("1", "under-floor heating (powered by hot water)");
		RADTYPEMAP_EN.put("2", "ceiling heating (powered by hot water)");
		RADTYPEMAP_EN.put("10", "electricity-powered radiator");
	}
	
	public static class TypeDeclaration implements ExtensionResourceTypeDeclaration<SmartEffResource> {

		@Override
		public Class<? extends SmartEffResource> dataType() {
			return TYPE_CLASS;
		}

		@Override
		public String label(OgemaLocale locale) {
			return "Radiator type data";
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
	
	public static class EditPage extends EditPageGenericWithTable<HeatRadiatorType> {
		@Override
		public void setData(HeatRadiatorType sr) {
			setHeaderLabel(EN, "Radiator Type Data", DE, "Daten Thermostattype im Gebäude");
			setLabel(sr.name(), EN, "Name of radiator type", DE, "Name des Fenstertyps");
			setTableHeader(sr.name(), EN, "Name");
			setLabel(sr.numberOfRadiators(), EN, "Number of radiators of this type in building NOT modeled in rooms",
					DE, "Zahl der Thermostate des Typs im Gebäude, die NICHT in Räumen angelegt wurden");
			setTableHeader(sr.numberOfRadiators(), EN, "# in building",
					DE, "# im Gebäude");
			setLabel(sr.radiatorDescription(), EN, "Radiator description");
			setTableHeader(sr.radiatorDescription(), EN, "Description of the type (optional)", DE, "Beschreibung des Typs (optional)");
			
			setLabel(sr.typeOfThermostat(), EN, "Type of thermostats", DE, "Art Heizkörperthermostate");
			setDisplayOptions(sr.typeOfThermostat(), EN, THTMAP_EN);
			setLabel(sr.radiatorPictureURLs(), EN, "Picture URLs");
			
			setLabel(sr.radiatorHight(), EN, "Height of radiators of the type (m)", DE, "Höhe der Radiatoren des Typs bei vertikalem Einbau (m)");
			setLabel(sr.radiatorDepth(), EN, "Depth of radiators of the type (m)", DE, "Tiefe der Radiatoren des Typs (m)");
			setLabel(sr.nominalTemperatureDifference(), EN, "Nominal DeltaT between room temperature and flow temperature", DE, "Nenn-DeltaT zwischen Raumtemperatur und Vorlauftemperatur");
			setLabel(sr.powerPerLength(), EN, "Estimated thermal power per meter of radiator lenght at nominal DeltaT",
						DE, "Geschätzte thermische Leistung pro Meter Heizkörperlänge bei Nenn-DeltaT");
			setLabel(sr.radiatorType(), EN, "Radiator Type", DE, "Art des Radiators");
			setDisplayOptions(sr.radiatorType(), EN, RADTYPEMAP_EN);
		}
		
		@SuppressWarnings("unchecked")
		@Override
		public Class<HeatRadiatorType> primaryEntryTypeClass() {
			return (Class<HeatRadiatorType>) TYPE_CLASS;
		}
		
		@Override
		protected String getMaintainer() {
			return "test1";
		}
		
		@Override
		protected String getHeader(OgemaHttpRequest req) {
			return getReqData(req).getParent().getParent().getName();
		}
	}
}
