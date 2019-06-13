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

		setLabel(data.savingsAfterBasicRenovation(), true,
				EN, "Estimated savings after basic renovation",
				DE, "Geschätzte Einsparung durch einfache Sanierung");

		setLabel(data.wwConsumption(), true,
				EN, "Known or estimated warm drinking water consumption",
				DE, "Bekannter oder geschätzter Trinkwarmwasserverbrauch");

		setLabel(data.wwLossHeatedAreas(), true,
				EN, "Estimated warm water energy loss from storage, circulation at current temperature in heated areas",
				DE, "Geschätzter Warmwasser-Energieverlost duch Speicher, Zirkulation"
						+ "bei aktueller Temperatur in beheizten Bereichen");

		setLabel(data.wwLossUnheatedAreas(), true,
				EN, "Warm water energy loss in unheated areas",
				DE, "Warmwasser-Energieverlust in unbeheizten Bereichen");

		setLabel(data.wwTemp(), true,
				EN, "Warm water temperature (°C)",
				DE, "Warmwassertemperatur (°C)");

		setLabel(data.wwTempMin(), true,
				EN, "Warm water temperature can be lowered to (°C)",
				DE, "Warmwassertemperatur kann gesenkt werden auf (°C)");

		setLabel(data.heatingLimitTemp(), true,
				EN, "Heating limit temperature",
				DE, "Heizgrenztemperatur");

		setLabel(data.outsideDesignTemp(), true,
				EN, "Outside design temperature",
				DE, "Außentemperatur-Auslegung");

		setLabel(data.savingsFromCDBoiler(), true,
				EN, "Estimated savings from condensing boiler",
				DE, "Geschätzte Einsparung durch Brennwertkessel");

		setLabel(data.designedForPriceType(), true,
				EN, "Designed for price type",
				DE, "Auslegung auf Preistyp");

		setLabel(data.uValueBasementFacade(), true,
				EN, "U-Value basement → facade",
				DE, "U-Wert Keller → Fassade");

		setLabel(data.uValueRoofFacade(), true,
				EN, "U-Value roof → facade",
				DE, "U-Wert Dach → Fassade");

		setLabel(data.innerWallThickness(), true,
				EN, "Thickness of inner walls",
				DE, "Dicke der Innenwände");

		setLabel(data.basementTempHeatingSeason(), true,
				EN, "Basement temperature during heating season",
				DE, "Kellertemperatur in der Heizperiode");
		
		setLabel(data.boilerPowerBoilerOnly(), true,
				EN, "Designed boiler power (boiler only)",
				DE, "Auslegung Kessel alleine");
		
		setLabel(data.hpPowerBivalentHP(), true,
				EN, "Designed heat pump power (bivalent heat pump)",
				DE, "Auslegung Wärmepumpe bivalent");


	}
}
