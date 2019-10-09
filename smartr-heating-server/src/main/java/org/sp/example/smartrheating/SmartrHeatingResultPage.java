package org.sp.example.smartrheating;


import org.smartrplace.smarteff.util.editgeneric.EditPageGeneric;
import org.smartrplace.smarteff.util.editgeneric.EditPageGenericUtil;

import de.iwes.widgets.api.widgets.localisation.OgemaLocale;
import extensionmodel.smarteff.smartrheating.SmartrHeatingResult;


public class SmartrHeatingResultPage extends EditPageGeneric<SmartrHeatingResult> {
	@Override
	public String label(OgemaLocale locale) {
		return "SmartrHeating calculator result page";
	}

	@Override
	public Class<? extends SmartrHeatingResult> primaryEntryTypeClass() {
		return SmartrHeatingResult.class;
	}

	@Override
	public void setData(SmartrHeatingResult result) {
		//setLabel(result.name(), EN, "name", DE, "Name");

		setLabel(result.costOfProject(), EN, "Cost of project");
		setLink(result.costOfProject(), EN, SmartrHeatingEval.WIKI_LINK +  "#initial-investment-cost");
		setLabel(result.yearlyOperatingCosts(), EN, "Yearly operating costs for the system");
		setLink(result.yearlyOperatingCosts(), EN, SmartrHeatingEval.WIKI_LINK +  "#yearly-benefits-and-costs");
		setLabel(result.yearlySavings(), EN, "Yearly savings in energy cost and estimated valuable benefits");
		setLink(result.yearlySavings(), EN, SmartrHeatingEval.WIKI_LINK +  "#yearly-benefits-and-costs");
		//setLabel(result.yearlySavings(), EN, "Pay-back period in years");

		setLabel(result.wwEnergyPreRenovation(),
				EN, "Warm water energy (pre-renovation) (kWh) "
						+ "(losses during heating season in heated areas are attributed to heater)",
				DE, "TWW-Energie (vor Sanierung) (kWh) "
						+ "(während Heizperiode werden Verluste TWW im beheizten Bereich der Heizung zugerechnet)");
		setLink(result.wwEnergyPreRenovation(), EN, SmartrHeatingEval.WIKI_LINK +  "#warm-water-energy-pre-renovation-kWh");

		setLabel(result.heatingEnergyPreRenovation(),
				EN, "Heating energy (pre-renovation) (kWh)",
				DE, "Heizenergie (vor Sanierung) (kWh)");
		setLink(result.heatingEnergyPreRenovation(), EN, SmartrHeatingEval.WIKI_LINK +  "#h");

		setLabel(result.thermostatNum(), EN, "Number of thermostats in building to be replaced");
		setLink(result.thermostatNum(), EN, SmartrHeatingEval.WIKI_LINK +  "#h");
		setLabel(result.roomNumInBuilding(), EN, "Number of rooms in building total");
		setLabel(result.roomNumWithThermostats(), EN, "Number of rooms with heating and thermostats"
				+ " to be replaced");

		setLabel(result.savingsAbsolute(), EN, "Absolute savings per year expected (kWh)");
		setLink(result.savingsAbsolute(), EN, SmartrHeatingEval.WIKI_LINK +  "#h");
		setLabel(result.savingsRelative(), EN, "Relative saving of heating energy input");
		setLabel(result.hoursWithoutLowering(), EN, "Hours per week with effectively not lowered temperature");
		setLink(result.hoursWithoutLowering(), EN, SmartrHeatingEval.WIKI_LINK +  "#h");
		setLabel(result.hoursLoweringEffectiveBefore(), EN, "Hours per week at which temperature was lowered before");
		
		EditPageGenericUtil.setDataProjectEff(result, this, true);
		
		/* Documentation Links */
		setHeaderLink(EN, SmartrHeatingEval.WIKI_LINK + "#results");
		//setLink(result.wwEnergyPreRenovation(), EN, HPAdaptEval.WIKI_LINK +  "#warm-water-energy-(pre-renovation)-(kwh)-");
		//setLink(result.heatingEnergyPreRenovation(), EN, HPAdaptEval.WIKI_LINK +  "#heating-energy-(pre-renovation)-(kwh)");
	}
}
