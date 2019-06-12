package org.sp.calculator.hpadapt;

import org.smartrplace.smarteff.util.editgeneric.EditPageGenericParams;

import extensionmodel.smarteff.hpadapt.HPAdaptParams;

public class HPAdaptParamsPage extends EditPageGenericParams<HPAdaptParams> {
	@Override
	public void setData(HPAdaptParams sr) {
		setLabel(sr.gasPriceCO2neutralPerkWh(), EN, "Price gas in EUR/kWh for todays CO2-neutral supply",
				DE, "Gaspreis in EUR/kWh für heutige CO2-neutrale Versorgung");
		//TODO: Other fields
	}
	@Override
	protected Class<HPAdaptParams> primaryEntryTypeClass() {
		return HPAdaptParams.class;
	}
}
