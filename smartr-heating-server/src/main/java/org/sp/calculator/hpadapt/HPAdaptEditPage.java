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
						+ "bei aktueller Temperatur im beheizten Bereichen");

		setLabelWithUnit(data.wwLossUnheatedAreas(),
				EN, "Warm water energy loss in unheated areas",
				DE, "Warmwasser-Energieverlust im unbeheizten Bereichen");

		setLabelWithUnit(data.wwTemp(),
				EN, "Warm water temperature",
				DE, "Warmwassertemperatur");

		setLabelWithUnit(data.wwTempMin(),
				EN, "Warm water temperature can be lowered to",
				DE, "Warmwassertemperatur kann gesenkt werden auf");

		setLabelWithUnit(data.heatingLimitTemp(),
				EN, "Heating limit temperature",
				DE, "Heizgrenztemperatur");

		setLabelWithUnit(data.outsideDesignTemp(),
				EN, "Outside design temperature",
				DE, "Außentemperatur-Auslegung",
				-40, 50);

		setLabelWithUnit(data.savingsFromCDBoiler(),
				EN, "Estimated savings from condensing boiler",
				DE, "Geschätzte Einsparung durch Brennwertkessel");

		setLabelWithUnit(data.dimensioningForPriceType(),
				EN, "Dimensioning for price type",
				DE, "Auslegung auf Preistyp");

		setLabelWithUnit(data.uValueBasementFacade(),
				EN, "U-Value basement in relation to U-Value facade (equal = 1.0)",
				DE, "U-Wert Keller im Verhältnis zum U-Wert Fassade (gleich = 1.0)");

		setLabelWithUnit(data.uValueRoofFacade(),
				EN, "U-Value roof in relation to U-Value facade (equal = 1.0)",
				DE, "U-Wert Dach im Verhältnis zum U-Wert Fassade (gleich = 1.0)");

		setLabelWithUnit(data.innerWallThickness(),
				EN, "Thickness of inner walls",
				DE, "Dicke der Innenwände");

		setLabelWithUnit(data.basementTempHeatingSeason(),
				EN, "Basement temperature during heating season",
				DE, "Kellertemperatur in der Heizperiode");
		
		setLabel(data.temperatureHistory(),
				EN, "Historical Temperature Data to be imported via CSV",
				DE, "Temperaturewerte, die per CSV importiert werden können");

	}
}
