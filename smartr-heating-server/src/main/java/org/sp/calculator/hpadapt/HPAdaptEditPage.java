package org.sp.calculator.hpadapt;

import java.util.HashMap;
import java.util.Map;

import org.smartrplace.smarteff.util.editgeneric.EditPageGeneric;

import de.iwes.widgets.api.widgets.localisation.OgemaLocale;
import extensionmodel.smarteff.hpadapt.HPAdaptData;
import extensionmodel.smarteff.smartrheating.SmartrHeatingData;

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
				DE, "");

		setLabel(data.wwConsumption(), true,
				EN, "Known or estimated warm drinking water consumption",
				DE, "");

		setLabel(data.wwLossHeatedAreas(), true,
				EN, "Estimated warm water energy loss from storage, circulation at current temperature in heated areas",
				DE, "");

		setLabel(data.wwLossUnheatedAreas(), true,
				EN, "Warm water energy loss in unheated areas",
				DE, "");

		setLabel(data.wwTemp(), true,
				EN, "Warm water temperature",
				DE, "");

		setLabel(data.wwTempMin(), true,
				EN, "Warm water temperature can be lowered to",
				DE, "");

		setLabel(data.heatingLimitTemp(), true,
				EN, "Heating limit temperature",
				DE, "");

		setLabel(data.outsideDesignTemp(), true,
				EN, "Outside design temperature",
				DE, "");

		setLabel(data.savingsFromCDBoiler(), true,
				EN, "Estimated savings from condensing boiler",
				DE, "");

		setLabel(data.designedForPriceType(), true,
				EN, "Designed for price type",
				DE, "");

		setLabel(data.uValueBasementFacade(), true,
				EN, "U-Value basement → facade",
				DE, "");

		setLabel(data.uValueRoofFacade(), true,
				EN, "U-Value roof → facade",
				DE, "");

		setLabel(data.innerWallThickness(), true,
				EN, "Thickness of inner walls",
				DE, "");

		setLabel(data.basementTempHeatingSeason(), true,
				EN, "Basement temperature during heating season",
				DE, "");


	}
}
