package org.sp.calculator.smartrheating.intern;

import org.smartrplace.smarteff.util.editgeneric.EditPageGenericParams;
import org.sp.calculator.hpadapt.HPAdaptEval;

import extensionmodel.smarteff.smartrheating.intern.HPAdaptParamInternal;

public class HPAdaptParamsInternalPage extends EditPageGenericParams<HPAdaptParamInternal> {
	@Override
	public void setData(HPAdaptParamInternal params) {
		
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

		/* Documentation Links */
		setHeaderLink(EN, HPAdaptEval.WIKI_LINK + "#parameters");
	}
	@Override
	public Class<HPAdaptParamInternal> primaryEntryTypeClass() {
		return HPAdaptParamInternal.class;
	}
}
