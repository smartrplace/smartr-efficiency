package org.sp.calculator.hpadapt;


import org.smartrplace.smarteff.util.editgeneric.EditPageGeneric;

import de.iwes.widgets.api.widgets.localisation.OgemaLocale;
import extensionmodel.smarteff.hpadapt.HPAdaptData;

public class HPAdaptEditPage extends EditPageGeneric<HPAdaptData> {
	@Override
	public String label(OgemaLocale locale) {
		return "Bivalent heat pumpt calculator edit page";
	}

	@Override
	protected Class<HPAdaptData> primaryEntryTypeClass() {
		return HPAdaptData.class;
	}

	@Override
	public void setData(HPAdaptData data) {
		setLabel(data.name(), EN, "name", DE, "Name");

		setLabelWithUnit(data.savingsAfterBasicRenovation(),
				EN, "Estimated savings after basic renovation",
				DE, "Geschätzte Einsparung durch einfache Sanierung");

		setLabelWithUnit(data.wwConsumption(),
				EN, "Known or estimated warm drinking water consumption",
				DE, "Bekannter oder geschätzter Trinkwarmwasserverbrauch");

		setLabelWithUnit(data.wwLossHeatedAreas(),
				EN, "Estimated warm water energy loss from storage, circulation at current temperature in heated areas",
				DE, "Geschätzter Warmwasser-Energieverlost duch Speicher, Zirkulation"
						+ "bei aktueller Temperatur in beheizten Bereichen");

		setLabelWithUnit(data.wwLossUnheatedAreas(),
				EN, "Warm water energy loss in unheated areas",
				DE, "Warmwasser-Energieverlust in unbeheizten Bereichen");

		setLabelWithUnit(data.wwTemp(),
				EN, "Warm water temperature (°C)",
				DE, "Warmwassertemperatur (°C)");

		setLabelWithUnit(data.wwTempMin(),
				EN, "Warm water temperature can be lowered to (°C)",
				DE, "Warmwassertemperatur kann gesenkt werden auf (°C)");

		setLabelWithUnit(data.heatingLimitTemp(),
				EN, "Heating limit temperature",
				DE, "Heizgrenztemperatur");

		setLabelWithUnit(data.outsideDesignTemp(),
				EN, "Outside design temperature",
				DE, "Außentemperatur-Auslegung");

		setLabelWithUnit(data.savingsFromCDBoiler(),
				EN, "Estimated savings from condensing boiler",
				DE, "Geschätzte Einsparung durch Brennwertkessel");

		setLabelWithUnit(data.designedForPriceType(),
				EN, "Designed for price type",
				DE, "Auslegung auf Preistyp");

		setLabelWithUnit(data.uValueBasementFacade(),
				EN, "U-Value basement → facade",
				DE, "U-Wert Keller → Fassade");

		setLabelWithUnit(data.uValueRoofFacade(),
				EN, "U-Value roof → facade",
				DE, "U-Wert Dach → Fassade");

		setLabelWithUnit(data.innerWallThickness(),
				EN, "Thickness of inner walls",
				DE, "Dicke der Innenwände");

		setLabelWithUnit(data.basementTempHeatingSeason(),
				EN, "Basement temperature during heating season",
				DE, "Kellertemperatur in der Heizperiode");
		
		setLabelWithUnit(data.boilerPowerBoilerOnly(),
				EN, "Designed boiler power (boiler only)",
				DE, "Auslegung Kessel alleine");
		
		setLabelWithUnit(data.hpPowerBivalentHP(),
				EN, "Designed heat pump power (bivalent heat pump)",
				DE, "Auslegung Wärmepumpe bivalent");


	}
}
