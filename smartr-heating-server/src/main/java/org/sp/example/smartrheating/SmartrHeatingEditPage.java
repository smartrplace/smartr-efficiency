package org.sp.example.smartrheating;

import java.util.HashMap;
import java.util.Map;

import org.smartrplace.commontypes.RadiatorTypeRegistration;
import org.smartrplace.smarteff.util.editgeneric.EditPageGeneric;

import de.iwes.widgets.api.widgets.localisation.OgemaLocale;
import extensionmodel.smarteff.smartrheating.SmartrHeatingData;

public class SmartrHeatingEditPage extends EditPageGeneric<SmartrHeatingData> {
	@Override
	public String label(OgemaLocale locale) {
		return "SmartrHeating Edit Page";
	}
	
	/*public boolean checkResource(SmartrHeatingData data) {
		if(!checkResourceBase(data, false)) return false;
		String newName = CapabilityHelper.getnewDecoratorName("SmartrHeatingProject", data.getParent());
		ValueResourceHelper.setIfNew(data.name(), newName);
		if(data.numberOfRadiators().getValue() <= 0) return false;
		if(data.numberOfRooms().getValue() <= 0) return false;
		return true;
	}*/

	@Override
	protected Class<SmartrHeatingData> primaryEntryTypeClass() {
		return SmartrHeatingData.class;
	}

	public static final Map<String, String> FUNGUSMAP_EN = new HashMap<>();
	public static final Map<String, String> FUNGUSMAP_DE = new HashMap<>();

	static {
		FUNGUSMAP_EN.put("1", "None");
		FUNGUSMAP_EN.put("2", "Relevant");
		FUNGUSMAP_EN.put("3", "Important Issue");
		FUNGUSMAP_DE.put("1", "Keine");
		FUNGUSMAP_DE.put("2", "Relevant");
		FUNGUSMAP_DE.put("3", "Ernsthaftes Problem");
	}
	
	@Override
	public void setData(SmartrHeatingData sr) {
		setLabel(sr.name(), EN, "name", DE, "Name");
		setLabel(sr.numberOfRadiators(), EN, "Number of thermostats", DE, "Zahl Termostate");
		setLabel(sr.numberOfRooms(), EN, "Number of rooms", DE, "Zahl Räume");
		setLabel(sr.gasMeterHasPulseOutput(), EN, "Does gas meter provide magnetic pulse output?", DE, "Besitzt der Gaszähler einen magnetischen Impulsausgang?");
		setLabel(sr.problemsWithFungusOrMould(), EN, "Does the building have problems with fungus or mould?", DE, "Bestehen Probleme mit Schimmelbildung?");
		setDisplayOptions(sr.problemsWithFungusOrMould(), EN, FUNGUSMAP_EN);
		setDisplayOptions(sr.problemsWithFungusOrMould(), DE, FUNGUSMAP_DE);
		setLabel(sr.typeOfThermostats(), EN, "Type of thermostats", DE, "Art Heizkörperthermostate");
		setDisplayOptions(sr.typeOfThermostats(), EN, RadiatorTypeRegistration.THTMAP_EN);
	}

	/*@Override
	protected void getEditTableLines(EditPageBase<SmartrHeatingData>.EditTableBuilder etb) {
		etb.addEditLine("Name", mh.stringEdit("name", alert));
		etb.addEditLine("Zahl Termostate", mh.integerEdit("numberOfRadiators", alert, 1, 999999, "Value outside range!"));
		etb.addEditLine("Zahl Räume", mh.integerEdit("numberOfRooms", alert, 1, 999999, "Value outside range!"));
	}*/
}
