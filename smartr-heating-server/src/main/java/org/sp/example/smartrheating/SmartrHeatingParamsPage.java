package org.sp.example.smartrheating;

import org.smartrplace.smarteff.util.editgeneric.EditPageGenericParams;

import extensionmodel.smarteff.smartrheating.SmartrHeatingParams;

public class SmartrHeatingParamsPage extends EditPageGenericParams<SmartrHeatingParams> {
	@Override
	public void setData(SmartrHeatingParams sr) {
		setLabel(sr.costOfCustomerPerRoom(), EN, "Own cost of customer per room per for project installation",
				DE, "Eigene Kosten des Kunden pro Raum bei Unterstützung von Installation und Inbetriebname");
	}
	@Override
	protected Class<SmartrHeatingParams> primaryEntryTypeClass() {
		return SmartrHeatingParams.class;
	}
}
