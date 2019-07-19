package org.sp.calculator.hpadapt;


import org.smartrplace.smarteff.util.editgeneric.EditPageGeneric;

import de.iwes.widgets.api.widgets.localisation.OgemaLocale;
import extensionmodel.smarteff.hpadapt.HPAdaptResult;


public class HPAdaptResultPage extends EditPageGeneric<HPAdaptResult> {
	@Override
	public String label(OgemaLocale locale) {
		return "Bivalent heat pumpt calculator result page";
	}

	@Override
	protected Class<HPAdaptResult> primaryEntryTypeClass() {
		return HPAdaptResult.class;
	}

	@Override
	public void setData(HPAdaptResult result) {
		setLabel(result.name(), EN, "name", DE, "Name");

		setLabelWithUnit(result.boilerPowerBoilerOnly(),
				EN, "Boiler power (boiler only)",
				DE, "Leistung Kessel alleine");
		
		setLabelWithUnit(result.boilerPowerBivalentHP(),
				EN, "Boiler power (bivalent heat pump)",
				DE, "Leistung Kessel (bivalente Wärmepumpe)");
		
		setLabelWithUnit(result.hpPowerBivalentHP(),
				EN, "Heat pump power (bivalent heat pump)",
				DE, "Leistung Wärmepumpe bivalent");

		setLabel(result.wwEnergyPreRenovation(),
				EN, "Warm water energy (pre-renovation) (kWh) "
						+ "(losses during heating season in heated areas are attributed to heater)",
				DE, "TWW-Energie (vor Sanierung) (kWh) "
						+ "(während Heizperiode werden Verluste TWW im beheizten Bereich der Heizung zugerechnet)");

		setLabel(result.heatingEnergyPreRenovation(),
				EN, "Heating energy (pre-renovation) (kWh)",
				DE, "Heizenergie (vor Sanierung) (kWh)");

		setLabel(result.wwEnergyPostRenovation(),
				EN, "Warm water energy (post-renovation) (kWh)",
				DE, "TWW-Energie (nach Sanierung) (kWh)");

		setLabel(result.heatingEnergyPostRenovation(),
				EN, "Heating energy (post-renovation) (kWh)",
				DE, "Heizenergie (nach Sanierung) (kWh)");

		setLabel(result.totalEnergyPostRenovation(),
				EN, "Total energy (post-renovation) (kWh)",
				DE, "Gesamtenergie (nach Sanierung) (kWh)");

		setLabelWithUnit(result.heatingDegreeDays(),
				EN, "Heating degree days",
				DE, "Heizgradtage");

		setLabelWithUnit(result.numberOfHeatingDays(),
				EN, "Number of heating days",
				DE, "Zahl der Heiztage");

		/* setLabelWithUnit(result.heatingDegreeDaysHourly(),
				EN, "Heating degree days (hourly basis)",
				DE, "Heizgradtage (Stundenbasis)"); */

		/* setLabelWithUnit(result.numberOfHeatingDaysHourly(),
				EN, "Number of heating days (hourly basis)",
				DE, "Zahl der Heiztage (Stundenbasis)"); */

		setLabelWithUnit(result.fullLoadHoursExclWW(),
				EN, "Full load hours excl. warm water (h/a)",
				DE, "Vollaststunden ohne TWW (h/a)");

		setLabelWithUnit(result.fullLoadHoursInclWW(),
				EN, "Full load hours incl. warm water (h/a)",
				DE, "Vollaststrunden mit TWW (h/a)");

		setLabelWithUnit(result.meanHeatingOutsideTemp(),
				EN, "Mean heating outside temperature",
				DE, "Mittlere Heit-Außentemperatur");

		setLabelWithUnit(result.maxPowerHPfromBadRoom(),
				EN, "Maximum power of heat pump from BadRoom",
				DE, "Maximale Leistung Wärmepumpe aus BadRoom");

		setLabelWithUnit(result.windowArea(),
				EN, "Window area",
				DE, "Fensterfläche");

		setLabelWithUnit(result.pLossWindow(),
				EN, "Window power loss (W/K)",
				DE, "Verlust durch Fenster (W/K)");

		setLabelWithUnit(result.numberOfRoomsFacingOutside(),
				EN, "Number of rooms facing outside",
				DE, "Anzahl der Räume nach Außen");

		setLabelWithUnit(result.facadeWallArea(),
				EN, "Facade wall area",
				DE, "Fassadenfläche");

		setLabelWithUnit(result.basementArea(),
				EN, "Basement area",
				DE, "Kellerfläche");

		setLabelWithUnit(result.roofArea(),
				EN, "Roof area",
				DE, "Dachfläche");

		setLabelWithUnit(result.weightedExtSurfaceAreaExclWindows(),
				EN, "Weighted exterior surface area excl. windows",
				DE, "Gewichtete Außenfläche ohne Fenster");

		setLabelWithUnit(result.activePowerWhileHeating(),
				EN, "Active power while heating",
				DE, "Wirkleistung beim Heizen");

		setLabelWithUnit(result.totalPowerLoss(),
				EN, "Total power loss (W/K)",
				DE, "Gesamtverlust (W/K)");

		setLabelWithUnit(result.uValueFacade(),
				EN, "U-Value of facade",
				DE, "U-Wert der Fassade");

		setLabelWithUnit(result.powerLossBasementHeating(),
				EN, "Basement heating power loss",
				DE, "Heizenergieverlust Keller");

		setLabelWithUnit(result.otherPowerLoss(),
				EN, "Other power loss (W/K)",
				DE, "Sonstige verluste (W/K)");

		setLabelWithUnit(result.powerLossAtFreezing(),
				EN, "Power loss at 0°C",
				DE, "Verlust bei 0°C");

		setLabelWithUnit(result.powerLossAtOutsideDesignTemp(),
				EN, "Power loss at outside design temperature",
				DE, "Verlust bei Ausgelegter Außentemperatur");
		

		/* Documentation Links */
		setHeaderLink(EN, HPAdaptEval.WIKI_LINK + "#results");
		//setLink(result.boilerPowerBoilerOnly(), EN, HPAdaptEval.WIKI_LINK +  "#boiler-power-(boiler-only)");
		//setLink(result.boilerPowerBivalentHP(), EN, HPAdaptEval.WIKI_LINK +  "#boiler-power-(bivalent-heat-pump)");
		//setLink(result.hpPowerBivalentHP(), EN, HPAdaptEval.WIKI_LINK +  "#heat-pump-power-(bivalent-heat-pump)");
		//setLink(result.wwEnergyPreRenovation(), EN, HPAdaptEval.WIKI_LINK +  "#warm-water-energy-(pre-renovation)-(kwh)-");
		//setLink(result.heatingEnergyPreRenovation(), EN, HPAdaptEval.WIKI_LINK +  "#heating-energy-(pre-renovation)-(kwh)");
		//setLink(result.wwEnergyPostRenovation(), EN, HPAdaptEval.WIKI_LINK +  "#warm-water-energy-(post-renovation)-(kwh)");
		//setLink(result.heatingEnergyPostRenovation(), EN, HPAdaptEval.WIKI_LINK +  "#heating-energy-(post-renovation)-(kwh)");
		//setLink(result.totalEnergyPostRenovation(), EN, HPAdaptEval.WIKI_LINK +  "#total-energy-(post-renovation)-(kwh)");
		//setLink(result.heatingDegreeDays(), EN, HPAdaptEval.WIKI_LINK +  "#heating-degree-days");
		//setLink(result.numberOfHeatingDays(), EN, HPAdaptEval.WIKI_LINK +  "#number-of-heating-days");
		//setLink(result.heatingDegreeDaysHourly(), EN, HPAdaptEval.WIKI_LINK +  "#heating-degree-days-(hourly-basis)");
		//setLink(result.numberOfHeatingDaysHourly(), EN, HPAdaptEval.WIKI_LINK +  "#number-of-heating-days-(hourly-basis)");
		//setLink(result.fullLoadHoursExclWW(), EN, HPAdaptEval.WIKI_LINK +  "#full-load-hours-excl.-warm-water-(h/a)");
		//setLink(result.fullLoadHoursInclWW(), EN, HPAdaptEval.WIKI_LINK +  "#full-load-hours-incl.-warm-water-(h/a)");
		//setLink(result.meanHeatingOutsideTemp(), EN, HPAdaptEval.WIKI_LINK +  "#mean-heating-outside-temperature");
		//setLink(result.maxPowerHPfromBadRoom(), EN, HPAdaptEval.WIKI_LINK +  "#maximum-power-of-heat-pump-from-badroom");
		//setLink(result.windowArea(), EN, HPAdaptEval.WIKI_LINK +  "#window-area");
		//setLink(result.pLossWindow(), EN, HPAdaptEval.WIKI_LINK +  "#window-power-loss-(w/k)");
		//setLink(result.numberOfRoomsFacingOutside(), EN, HPAdaptEval.WIKI_LINK +  "#number-of-rooms-facing-outside");
		//setLink(result.facadeWallArea(), EN, HPAdaptEval.WIKI_LINK +  "#facade-wall-area");
		//setLink(result.basementArea(), EN, HPAdaptEval.WIKI_LINK +  "#basement-area");
		//setLink(result.roofArea(), EN, HPAdaptEval.WIKI_LINK +  "#roof-area");
		//setLink(result.weightedExtSurfaceAreaExclWindows(), EN, HPAdaptEval.WIKI_LINK +  "#weighted-exterior-surface-area-excl.-windows");
		//setLink(result.activePowerWhileHeating(), EN, HPAdaptEval.WIKI_LINK +  "#active-power-while-heating");
		//setLink(result.totalPowerLoss(), EN, HPAdaptEval.WIKI_LINK +  "#total-power-loss-(w/k)");
		//setLink(result.uValueFacade(), EN, HPAdaptEval.WIKI_LINK +  "#u-value-of-facade");
		//setLink(result.powerLossBasementHeating(), EN, HPAdaptEval.WIKI_LINK +  "#basement-heating-power-loss");
		//setLink(result.otherPowerLoss(), EN, HPAdaptEval.WIKI_LINK +  "#other-power-loss-(w/k)");
		//setLink(result.powerLossAtFreezing(), EN, HPAdaptEval.WIKI_LINK +  "#power-loss-at-0°c");
		
	}
}
