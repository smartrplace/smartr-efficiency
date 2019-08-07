package org.sp.calculator.hpadapt;

import org.smartrplace.smarteff.util.editgeneric.EditPageGenericParams;

import extensionmodel.smarteff.hpadapt.HPAdaptParams;

public class HPAdaptParamsPage extends EditPageGenericParams<HPAdaptParams> {
	@Override
	public void setData(HPAdaptParams params) {
		
		setLabelWithUnit(params.pricesCO2neutral().electrictiyPricePerkWh(),
				EN, "Price of CO₂-neutral electricity (EUR/kWh)",
				DE, "Preis für CO₂-neutralem Strom (EUR/kWh)");

		setLabelWithUnit(params.prices100EE().electrictiyPricePerkWh(),
				EN, "Price of 100EE electricity (EUR/kWh)",
				DE, "Preis für 100EE-Strom (EUR/kWh)");

		//setLabelWithUnit(params.electrictiyPriceHeatBase(),
		//		EN, "Base price of heat pump electricity",
		//		DE, "Grundpreis von Wärmepumpenstrom");

		setLabelWithUnit(params.pricesConventional().electrictiyPriceHeatPerkWh(),
				EN, "Price of heat pump electricity (EUR/kWh)",
				DE, "Preis für Wärmepumpenstrom (EUR/kWh)");

		setLabelWithUnit(params.pricesCO2neutral().electrictiyPriceHeatPerkWh(),
				EN, "Price of CO₂-neutral heat pump electricity (EUR/kWh)",
				DE, "Preis für CO₂-neutralen Wärmepumpenstrom (EUR/kWh)");

		setLabelWithUnit(params.prices100EE().electrictiyPriceHeatPerkWh(),
				EN, "Price of 100EE heat pump electricity (EUR/kWh)",
				DE, "Preis für 100EE-Wärmepumpenstrom (EUR/kWh)");

		setLabelWithUnit(params.pricesConventional().gasPricePerkWh(),
				EN, "Price of conventional natural gas (EUR/kWh)",
				DE, "Preis für konventionelles Erdgas (EUR/kWh)");

		setLabelWithUnit(params.pricesCO2neutral().gasPricePerkWh(),
				EN, "Price of CO₂-neutral gas (EUR/kWh)",
				DE, "Preis für CO₂-neutrales Gas (EUR/kWh)");

		setLabelWithUnit(params.prices100EE().gasPricePerkWh(),
				EN, "Price of 100EE gas (EUR/kWh)",
				DE, "Preis für 100EE-Gas (EUR/kWh)");

		setLabelWithUnit(params.boilerPowerReductionLTtoCD(),
				EN, "Boiler Power Reduction switching from LT→CD",
				DE, "Energieeinsparung bei Umstellung NT→BW");

		setLabelWithUnit(params.wwSupplyTemp(),
				EN, "Warm water supply temperature",
				DE, "Trinkwarmwasser Vorlauftemperatur");
		
		setLabel(params.temperatureHistory(),
				EN, "Historical Temperature Data to be imported via CSV",
				DE, "Temperaturewerte, die per CSV importiert werden können");
		setLabel(params.copCharacteristics(),
				EN, "Heat pump COP characteristics",
				DE, "COP-Kennfeld der Wärmepumpe");
		
		
		/* Documentation Links */
		setHeaderLink(EN, HPAdaptEval.WIKI_LINK + "#parameters");
		//setLink(params.pricesCO2neutral().electrictiyPricePerkWh(), EN, HPAdaptEval.WIKI_LINK +  "#price-of-co₂-neutral-electricity-(eur/kwh)");
		//setLink(params.prices100EE().electrictiyPricePerkWh(), EN, HPAdaptEval.WIKI_LINK +  "#price-of-100ee-electricity-(eur/kwh)");
		//setLink(params.pricesConventional().electrictiyPriceHeatPerkWh(), EN, HPAdaptEval.WIKI_LINK +  "#price-of-heat-pump-electricity-(eur/kwh)");
		//setLink(params.pricesCO2neutral().electrictiyPriceHeatPerkWh(), EN, HPAdaptEval.WIKI_LINK +  "#price-of-co₂-neutral-heat-pump-electricity-(eur/kwh)");
		//setLink(params.prices100EE().electrictiyPriceHeatPerkWh(), EN, HPAdaptEval.WIKI_LINK +  "#price-of-100ee-heat-pump-electricity-(eur/kwh)");
		//setLink(params.pricesConventional().gasPricePerkWh(), EN, HPAdaptEval.WIKI_LINK +  "#price-of-conventional-natural-gas-(eur/kwh)");
		//setLink(params.pricesCO2neutral().gasPricePerkWh(), EN, HPAdaptEval.WIKI_LINK +  "#price-of-co₂-neutral-gas-(eur/kwh)");
		//setLink(params.prices100EE().gasPricePerkWh(), EN, HPAdaptEval.WIKI_LINK +  "#price-of-100ee-gas-(eur/kwh)");
		//setLink(params.boilerChangeCDtoCD(), EN, HPAdaptEval.WIKI_LINK +  "#condensing-boiler-→-condensing-boiler-(cd→cd),-base-price-(eur)");
		//setLink(params.boilerChangeLTtoCD(), EN, HPAdaptEval.WIKI_LINK +  "#low-temperature-boiler-→-condensing-boiler-(lt→cd),-base-price-(eur)");
		//setLink(params.boilerChangeCDtoCDAdditionalPerkW(), EN, HPAdaptEval.WIKI_LINK +  "#additional-cd→cd-(eur/kw)");
		//setLink(params.boilerChangeLTtoCDAdditionalPerkW(), EN, HPAdaptEval.WIKI_LINK +  "#additional-lt→cd-(eur/kw)");
		//setLink(params.additionalBivalentHPBase(), EN, HPAdaptEval.WIKI_LINK +  "#additional-base-cost-of-bivalent-heat-pump-(eur)");
		//setLink(params.additionalBivalentHPPerkW(), EN, HPAdaptEval.WIKI_LINK +  "#additional-base-cost-of-bivalent-heat-pump-(eur/kw)");
		//setLink(params.boilerPowerReductionLTtoCD(), EN, HPAdaptEval.WIKI_LINK +  "#boiler-power-reduction-switching-from-lt→cd");
		//setLink(params.wwSupplyTemp(), EN, HPAdaptEval.WIKI_LINK +  "#warm-water-supply-temperature");
		setLink(params.temperatureHistory(), EN, HPAdaptEval.WIKI_LINK +  "#historical-temperature-data-to-be-imported-via-csv");

	}
	@Override
	public Class<HPAdaptParams> primaryEntryTypeClass() {
		return HPAdaptParams.class;
	}
}
