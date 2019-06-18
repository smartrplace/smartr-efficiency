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

		setLabelWithUnit(result.wwEnergyPreRenovation(),
				EN, "Warm water energy (pre-renovation) "
						+ "(losses during heating season in heated areas are attributed to heater)",
				DE, "TWW-Energie (vor Sanierung)"
						+ "(während Heizperiode werden Verluste TWW im beheizten Bereich der Heizung zugerechnet)");

		setLabelWithUnit(result.heatingEnergPreRenovation(),
				EN, "Heating energy (pre-renovation)",
				DE, "Heizenergie (vor Sanierung)");

		setLabelWithUnit(result.wwEnergyPostRenovation(),
				EN, "Warm water energy (post-renovation)",
				DE, "TWW-Energie (nach Sanierung)");

		setLabelWithUnit(result.heatingEnergyPostRenovation(),
				EN, "Heating energy (post-renovation)",
				DE, "Heizenergie (nach Sanierung)");

		setLabelWithUnit(result.totalEnergyPostRenovation(),
				EN, "Total energy (post-renovation)",
				DE, "Gesamtenergie (nach Sanierung)");

		setLabelWithUnit(result.heatingDegreeDays(),
				EN, "Heating degree days",
				DE, "Heizgradtage");

		setLabelWithUnit(result.numberOfHeatingDays(),
				EN, "Number of heating days",
				DE, "Zahl der Heiztage");

		setLabelWithUnit(result.heatingDegreeDaysHourly(),
				EN, "Heating degree days (hourly basis)",
				DE, "Heizgradtage (Stundenbasis)");

		setLabelWithUnit(result.numberOfHeatingDaysHourly(),
				EN, "Number of heating days (hourly basis)",
				DE, "Zahl der Heiztage (Stundenbasis)");

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

	}
}