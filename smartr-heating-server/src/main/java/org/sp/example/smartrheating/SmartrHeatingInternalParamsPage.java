package org.sp.example.smartrheating;

import org.smartrplace.smarteff.util.editgeneric.EditPageGenericParams;

import extensionmodel.smarteff.smartrheating.SmartrHeatingInternalParams;

public class SmartrHeatingInternalParamsPage extends EditPageGenericParams<SmartrHeatingInternalParams> {
	@Override
	public void setData(SmartrHeatingInternalParams sr) {
		setLabel(sr.baseCost(), EN, "Base project cost (EUR)");
		setLabel(sr.costPerRoom(), EN, "Additional cost per room (EUR)");
		setLabel(sr.costPerThermostat(), EN, "Additional cost per thermostat (EUR)");
	}
	@Override
	protected Class<SmartrHeatingInternalParams> primaryEntryTypeClass() {
		return SmartrHeatingInternalParams.class;
	}
}
