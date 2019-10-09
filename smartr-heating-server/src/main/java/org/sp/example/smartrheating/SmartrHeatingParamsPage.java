package org.sp.example.smartrheating;

import org.smartrplace.smarteff.util.editgeneric.EditPageGenericParams;

import extensionmodel.smarteff.smartrheating.SmartrHeatingParams;

public class SmartrHeatingParamsPage extends EditPageGenericParams<SmartrHeatingParams> {
	@Override
	public void setData(SmartrHeatingParams sr) {
		setLabelWithUnit(sr.wwSupplyTemp(), EN, "Supply temperature of cold drinking water into the building");
		setLink(sr.wwSupplyTemp(), EN, SmartrHeatingEval.WIKI_LINK +  "#cold-water-supply-temperature");
		
		setLabel(sr.hoursOfCustomerBase(), EN, "Own working hours of customer for project implementation "
				+ "(base value, total value is plus working hours per room)");
		//		DE, "Eigene Arbeitsstunden des Kunden für die Projektdurchführung (Basis ohne Stunden pro Raum)");
		setLink(sr.hoursOfCustomerBase(), EN, SmartrHeatingEval.WIKI_LINK +  "#working-hours-of-customer--building-owner");
		setLabel(sr.hoursOfCustomerPerRoom(), EN, "Own working hours of customer per room per for project installation");
		//		DE, "Eigene Arbeitsstunden des Kunden pro Raum bei Unterstützung von Installation und Inbetriebname");
		setLabel(sr.hoursOfCustomerWinSensBase(),
				EN, "Own working hours of customer for window sensor installation training etc.");
		setLabel(sr.hoursOfCustomerPerWindowSensor(),
				EN, "Woen working hours of customer for each window sensor installation");
		
		setLabelWithUnit(sr.co2factorGas(), EN, "CO2 emissions of burning natural gas (kg/kWh)");
		
		//setLabel(sr.internalParamProvider(), EN, "Internal Parameter user provider");
		
		setLabel("#exportCSV", EN, "Export data");
		
		setHeaderLink(EN, SmartrHeatingEval.WIKI_LINK + "#parameters");
	}
	@Override
	public Class<SmartrHeatingParams> primaryEntryTypeClass() {
		return SmartrHeatingParams.class;
	}
}
