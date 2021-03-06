package org.sp.example.smartrheating;

import java.util.HashMap;
import java.util.Map;

import org.ogema.tools.resource.util.ResourceUtils;
import org.smartrplace.smarteff.util.editgeneric.EditPageGeneric;

import de.iwes.util.resource.ResourceHelper;
import de.iwes.widgets.api.widgets.localisation.OgemaLocale;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import extensionmodel.smarteff.api.common.BuildingData;
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
		return true;
	}*/

	@Override
	public Class<SmartrHeatingData> primaryEntryTypeClass() {
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
		//setLabel(sr.name(), EN, "name", DE, "Name");
		setLabel(sr.wwIsContained(), EN, "Is gas consumption for drinking warm water contained in yearly energy consumption?");
		setLink(sr.wwIsContained(), EN,SmartrHeatingEval.WIKI_LINK +  "#warm-water-supply-included-boolean");
		setLabelWithUnit(sr.wwConsumption(),
				EN, "Known or estimated warm drinking water consumption"); //,
		//		DE, "Bekannter oder geschätzter Trinkwarmwasserverbrauch");

		setLabelWithUnit(sr.wwLossHeatedAreas(),
				EN, "Estimated warm water energy loss from storage, circulation at current temperature in heated areas"); //,
		//		DE, "Geschätzter Warmwasser-Energieverlost duch Speicher, Zirkulation"
		//				+ "bei aktueller Temperatur im beheizten Bereichen");
		setLink(sr.wwLossHeatedAreas(), EN, SmartrHeatingEval.WIKI_LINK +  "#estimated-warm-water-energy-loss-from-storage,-circulation-at-current-temperature-in-heated-areas");

		setLabelWithUnit(sr.wwLossUnheatedAreas(),
				EN, "Warm water energy loss in unheated areas"); //,
		//		DE, "Warmwasser-Energieverlust im unbeheizten Bereichen");
		setLink(sr.wwLossUnheatedAreas(), EN, SmartrHeatingEval.WIKI_LINK +  "#warm-water-energy-loss-in-unheated-areas");

		setLabelWithUnit(sr.wwTemp(),
				EN, "Warm water temperature"); //,
		//		DE, "Warmwassertemperatur");
		setLink(sr.wwTemp(), EN, SmartrHeatingEval.WIKI_LINK +  "#warm-water-temperature");

		//setLabel(sr.heatingDegreeDaysManual(), EN, "Heating degree days per year");
		setLabel(sr.heatingDaysManual(), EN, "Average number of heating days per year");
		setLink(sr.heatingDaysManual(), EN, SmartrHeatingEval.WIKI_LINK +  "#number-of-heating-days");

		setLabel(sr.gasPricePerkWh(), EN, "Price for heating energy source per kWh set for building without fixed base fee (EUR/kWh)");
		setLabel(sr.usageTimePerWeek(), EN, "Average usage time of building hours per week");
		setLink(sr.usageTimePerWeek(), EN, SmartrHeatingEval.WIKI_LINK +  "#usage-time-per-week");
		setLabel(sr.usageBlocksPerWeek(), EN, "Average number of blocks of usage time per week");
		setLink(sr.usageBlocksPerWeek(), EN, SmartrHeatingEval.WIKI_LINK +  "#usage-blocks-per-week");
		setLabel(sr.coolingDownHours(), EN, "Average duration for cooling down of the building to temperature with low loss (hours)");
		setLink(sr.coolingDownHours(), EN, SmartrHeatingEval.WIKI_LINK +  "#time-to-cool-down-the-building");
		setLabel(sr.heatingUpHours(), EN, "Average duration for heating up of the building from temperature with low loss (hours)");
		setLink(sr.heatingUpHours(), EN, SmartrHeatingEval.WIKI_LINK +  "#time-to-heat-up-the-building");
		setLabel(sr.heatingReductionHoursBefore(), EN, "Number of hours the building was cooled per week down before introduction of SmartrHeating");
		setLink(sr.heatingReductionBlocksBefore(), EN, SmartrHeatingEval.WIKI_LINK +  "#number-of-hours-and-blocks-the-building-was-cooled-per-week-down-before-introduction-of-smartrheating");
		setLabel(sr.heatingReductionBlocksBefore(), EN, "Number of heating reduction blocks per week performed before introduction of SmartrHeating");
		//setLabel(sr.gasMeterHasPulseOutput(), EN, "Does gas meter provide magnetic pulse output?", DE, "Besitzt der Gaszähler einen magnetischen Impulsausgang?");
		setLabel(sr.problemsWithFungusOrMould(), EN, "Does the building have problems with fungus or mould?", DE, "Bestehen Probleme mit Schimmelbildung?");
		setDisplayOptions(sr.problemsWithFungusOrMould(), EN, FUNGUSMAP_EN);
		setDisplayOptions(sr.problemsWithFungusOrMould(), DE, FUNGUSMAP_DE);
		setLink(sr.problemsWithFungusOrMould(), EN, SmartrHeatingEval.WIKI_LINK +  "#problems-with-fungus-or-mould");
		
		/* Additional Documentation Links */
		setHeaderLink(EN, SmartrHeatingEval.WIKI_LINK + "#data");
	}
	
	@Override
	protected void defaultValues(SmartrHeatingData data, DefaultSetModes mode) {
		setDefault(data.wwIsContained(), true, mode);
		setDefault(data.wwLossHeatedAreas(), 0.35f, mode);
		setDefault(data.wwLossUnheatedAreas(), 0.25f, mode);
		setDefault(data.wwTemp(), 50+273.15f, mode);
		//setDefault(data.heatingDegreeDaysManual(), 2050, mode);
		setDefault(data.heatingDaysManual(), 250, mode);
		setDefault(data.gasPricePerkWh(), 0.06f, mode);
		setDefault(data.problemsWithFungusOrMould(), 1, mode);
		setDefault(data.gasMeterHasPulseOutput(), false, mode);
		setDefault(data.coolingDownHours(), 3, mode);
		setDefault(data.heatingUpHours(), 1, mode);
	}
	
	@Override
	protected String getHeader(OgemaHttpRequest req) {
		SmartrHeatingData res = getReqData(req);
		BuildingData bd = ResourceHelper.getFirstParentOfType(res, BuildingData.class);
		if(bd != null)
			return "Data for "+SmartrHeatingEval.label+" in "+ResourceUtils.getHumanReadableShortName(bd);
		return "Data for "+SmartrHeatingEval.label+" ("+ResourceUtils.getHumanReadableShortName(res)+")";
	}
}
