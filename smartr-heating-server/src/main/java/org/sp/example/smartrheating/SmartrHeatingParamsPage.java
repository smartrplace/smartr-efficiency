package org.sp.example.smartrheating;

import org.smartrplace.smarteff.util.editgeneric.EditPageGenericParams;

import extensionmodel.smarteff.smartrheating.SmartrHeatingParams;

public class SmartrHeatingParamsPage extends EditPageGenericParams<SmartrHeatingParams> {
	@Override
	public void setData(SmartrHeatingParams sr) {
		setLabelWithUnit(sr.wwSupplyTemp(), EN, "Supply temperature of drinking water into the building");
		
		setLabel(sr.costOfCustomerPerRoom(), EN, "Own cost of customer per room per for project installation",
				DE, "Eigene Kosten des Kunden pro Raum bei Unterstützung von Installation und Inbetriebname");
		
		setLabelWithUnit(sr.co2factorGas(), EN, "CO2 emissions of burning natural gas (kg/kWh)");
		
		setLabel(sr.internalParamProvider(), EN, "Internal Parameter user provider");
	}
	@Override
	public Class<SmartrHeatingParams> primaryEntryTypeClass() {
		return SmartrHeatingParams.class;
	}
}
