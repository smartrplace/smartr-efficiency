package org.sp.calculator.hpadapt;

import org.smartrplace.smarteff.util.editgeneric.EditPageGenericParams;

import extensionmodel.smarteff.hpadapt.HPAdaptParams;

public class HPAdaptParamsPage extends EditPageGenericParams<HPAdaptParams> {
	@Override
	public void setData(HPAdaptParams params) {
		
		setLabel(params.electrictiyPriceCO2neutralPerkWh(), true,
				EN, "Price of CO2-neutral electricity (EUR/kWh)",
				DE, "Preis für CO2-neutralem Strom (EUR/kWh)");

		setLabel(params.electrictiyPrice100EEPerkWh(), true,
				EN, "Price of 100EE electricity (EUR/kWh)",
				DE, "Preis für 100EE-Strom (EUR/kWh)");

		setLabel(params.electrictiyPriceHeatBase(), true,
				EN, "Base price of heat pump electricity",
				DE, "Grundpreis von Wärmepumpenstrom");

		setLabel(params.electrictiyPriceHeatPerkWh(), true,
				EN, "Price of heat pump electricity (EUR/kWh)",
				DE, "Preis für Wärmepumpenstrom (EUR/kWh)");

		setLabel(params.electrictiyPriceHeatCO2neutralPerkWh(), true,
				EN, "Price of CO2-neutral heat pump electricity (EUR/kWh)",
				DE, "Preis für CO2-neutralen Wärmepumpenstrom (EUR/kWh)");

		setLabel(params.electrictiyPriceHeat100EEPerkWh(), true,
				EN, "Price of 100EE heat pump electricity (EUR/kWh)",
				DE, "Preis für 100EE-Wärmepumpenstrom (EUR/kWh)");

		setLabel(params.gasPriceCO2neutralPerkWh(), true,
				EN, "Price of CO2-neutral gas (EUR/kWh)",
				DE, "Preis für CO2-neutrales Gas (EUR/kWh)");

		setLabel(params.gasPrice100EEPerkWh(), true,
				EN, "Price of 100EE gas (EUR/kWh)",
				DE, "Preis für 100EE-Gas (EUR/kWh)");

		setLabel(params.boilerChangeCDtoCD(), true,
				EN, "Condensing Boiler → Condensing Boiler (CD→CD), base price (EUR)",
				DE, "Brennwertkessel → Brennwertkessel (BW→BW), Basispreis (EUR)");

		setLabel(params.boilerChangeLTtoCD(), true,
				EN, "Low-Temperature Boiler → Condensing Boiler (LT→CD), base price (EUR)",
				DE, "Niedertemperaturkessel → Brennwertkessel (NT→BW), Basispreis (EUR)");

		setLabel(params.boilerChangeCDtoCDAdditionalPerkW(), true,
				EN, "Additional CD→CD (EUR/kW)",
				DE, "Zusätzlich (BW→BW) (EUR/kW)");

		setLabel(params.boilerChangeLTtoCDAdditionalPerkW(), true,
				EN, "Additional LT→CD (EUR/kW)",
				DE, "Zusätzlich NT→BW (EUR/kW)");

		setLabel(params.additionalBivalentHPBase(), true,
				EN, "Additional Base Cost of Bivalent Heat Pump (EUR)",
				DE, "Zusätzlicher Basispreis für bivalente Wärmepumpe (EUR)");

		setLabel(params.additionalBivalentHPPerkW(), true,
				EN, "Additional Base Cost of Bivalent Heat Pump (EUR/kW)",
				DE, "Zusätzlicher Basispreis für bivalente Wärmepumpe (EUR/kW)");

		setLabel(params.boilerPowerReductionLTtoCD(), true,
				EN, "Boiler Power Reduction switching from LT→CD",
				DE, "Energieeinsparung bei Umstellung NT→BW");

		setLabel(params.wwSupplyTemp(), true,
				EN, "Warm water supply temperature (°C)",
				DE, "Trinkwarmwasser Vorlauftemperatur (°C)");

	
		
	}
	@Override
	protected Class<HPAdaptParams> primaryEntryTypeClass() {
		return HPAdaptParams.class;
	}
}
