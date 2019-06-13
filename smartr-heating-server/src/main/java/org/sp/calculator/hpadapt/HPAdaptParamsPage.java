package org.sp.calculator.hpadapt;

import org.smartrplace.smarteff.util.editgeneric.EditPageGenericParams;

import extensionmodel.smarteff.hpadapt.HPAdaptParams;

public class HPAdaptParamsPage extends EditPageGenericParams<HPAdaptParams> {
	@Override
	public void setData(HPAdaptParams params) {
		
		setLabel(params.electrictiyPriceCO2neutralPerkWh(), true,
				EN, "Price of CO2-neutral electricity (EUR/kWh)",
				DE, "");

		setLabel(params.electrictiyPrice100EEPerkWh(), true,
				EN, "Price of 100EE neutral electricity (EUR/kWh)",
				DE, "");

		setLabel(params.electrictiyPriceHeatBase(), true,
				EN, "Base price for heat pump electricity",
				DE, "");

		setLabel(params.electrictiyPriceHeatPerkWh(), true,
				EN, "Price for heat pump electricity (EUR/kWh)",
				DE, "");

		setLabel(params.electrictiyPriceHeatCO2neutralPerkWh(), true,
				EN, "Price for CO2-neutral heat pump electricity (EUR/kWh)",
				DE, "");

		setLabel(params.electrictiyPriceHeat100EEPerkWh(), true,
				EN, "Price for 100EE heat pump electricity (EUR/kWh)",
				DE, "");

		setLabel(params.gasPriceCO2neutralPerkWh(), true,
				EN, "Price for CO2-neutral gas (EUR/kWh)",
				DE, "");

		setLabel(params.gasPrice100EEPerkWh(), true,
				EN, "Price for 100EE gas (EUR/kWh)",
				DE, "");

		setLabel(params.boilerChangeCDtoCD(), true,
				EN, "Condensing Boiler → Condensing Boiler (CD→CD), Base (EUR)",
				DE, "");

		setLabel(params.boilerChangeLTtoCD(), true,
				EN, "Low-Temperature Boiler → Condensing Boiler (LT→CD), Base (EUR)",
				DE, "");

		setLabel(params.boilerChangeCDtoCDAdditionalPerkW(), true,
				EN, "Additional CD→CD (per kW) (EUR)",
				DE, "");

		setLabel(params.boilerChangeLTtoCDAdditionalPerkW(), true,
				EN, "Additional LT→CD (per kW) (EUR)",
				DE, "");

		setLabel(params.additionalBivalentHPBase(), true,
				EN, "Additional Bivalent Heat Pump Base (EUR)",
				DE, "");

		setLabel(params.additionalBivalentHPPerkW(), true,
				EN, "Additional Bivalent Heat Pump per kW (EUR)",
				DE, "");

		setLabel(params.boilerPowerReductionLTtoCD(), true,
				EN, "Boiler Power Reduction switching from LT→CD (EUR)",
				DE, "");

		setLabel(params.wwSupplyTemp(), true,
				EN, "Warm water supply temperature (°C)",
				DE, "");

	
		
	}
	@Override
	protected Class<HPAdaptParams> primaryEntryTypeClass() {
		return HPAdaptParams.class;
	}
}
