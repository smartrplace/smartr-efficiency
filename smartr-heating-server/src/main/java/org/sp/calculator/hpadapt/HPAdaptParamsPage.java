package org.sp.calculator.hpadapt;

import org.smartrplace.smarteff.util.editgeneric.EditPageGenericParams;

import extensionmodel.smarteff.hpadapt.HPAdaptParams;

public class HPAdaptParamsPage extends EditPageGenericParams<HPAdaptParams> {
	@Override
	public void setData(HPAdaptParams params) {
		
		setLabelWithUnit(params.electrictiyPriceCO2neutralPerkWh(),
				EN, "Price of CO2-neutral electricity (EUR/kWh)",
				DE, "Preis für CO2-neutralem Strom (EUR/kWh)");

		setLabelWithUnit(params.electrictiyPrice100EEPerkWh(),
				EN, "Price of 100EE electricity (EUR/kWh)",
				DE, "Preis für 100EE-Strom (EUR/kWh)");

		setLabelWithUnit(params.electrictiyPriceHeatBase(),
				EN, "Base price of heat pump electricity",
				DE, "Grundpreis von Wärmepumpenstrom");

		setLabelWithUnit(params.electrictiyPriceHeatPerkWh(),
				EN, "Price of heat pump electricity (EUR/kWh)",
				DE, "Preis für Wärmepumpenstrom (EUR/kWh)");

		setLabelWithUnit(params.electrictiyPriceHeatCO2neutralPerkWh(),
				EN, "Price of CO2-neutral heat pump electricity (EUR/kWh)",
				DE, "Preis für CO2-neutralen Wärmepumpenstrom (EUR/kWh)");

		setLabelWithUnit(params.electrictiyPriceHeat100EEPerkWh(),
				EN, "Price of 100EE heat pump electricity (EUR/kWh)",
				DE, "Preis für 100EE-Wärmepumpenstrom (EUR/kWh)");

		setLabelWithUnit(params.gasPriceCO2neutralPerkWh(),
				EN, "Price of CO2-neutral gas (EUR/kWh)",
				DE, "Preis für CO2-neutrales Gas (EUR/kWh)");

		setLabelWithUnit(params.gasPrice100EEPerkWh(),
				EN, "Price of 100EE gas (EUR/kWh)",
				DE, "Preis für 100EE-Gas (EUR/kWh)");

		setLabelWithUnit(params.boilerChangeCDtoCD(),
				EN, "Condensing Boiler → Condensing Boiler (CD→CD), base price (EUR)",
				DE, "Brennwertkessel → Brennwertkessel (BW→BW), Basispreis (EUR)");

		setLabelWithUnit(params.boilerChangeLTtoCD(),
				EN, "Low-Temperature Boiler → Condensing Boiler (LT→CD), base price (EUR)",
				DE, "Niedertemperaturkessel → Brennwertkessel (NT→BW), Basispreis (EUR)");

		setLabelWithUnit(params.boilerChangeCDtoCDAdditionalPerkW(),
				EN, "Additional CD→CD (EUR/kW)",
				DE, "Zusätzlich (BW→BW) (EUR/kW)");

		setLabelWithUnit(params.boilerChangeLTtoCDAdditionalPerkW(),
				EN, "Additional LT→CD (EUR/kW)",
				DE, "Zusätzlich NT→BW (EUR/kW)");

		setLabelWithUnit(params.additionalBivalentHPBase(),
				EN, "Additional Base Cost of Bivalent Heat Pump (EUR)",
				DE, "Zusätzlicher Basispreis für bivalente Wärmepumpe (EUR)");

		setLabelWithUnit(params.additionalBivalentHPPerkW(),
				EN, "Additional Base Cost of Bivalent Heat Pump (EUR/kW)",
				DE, "Zusätzlicher Basispreis für bivalente Wärmepumpe (EUR/kW)");

		setLabelWithUnit(params.boilerPowerReductionLTtoCD(),
				EN, "Boiler Power Reduction switching from LT→CD",
				DE, "Energieeinsparung bei Umstellung NT→BW");

		setLabelWithUnit(params.wwSupplyTemp(),
				EN, "Warm water supply temperature",
				DE, "Trinkwarmwasser Vorlauftemperatur");

	
		
	}
	@Override
	protected Class<HPAdaptParams> primaryEntryTypeClass() {
		return HPAdaptParams.class;
	}
}
