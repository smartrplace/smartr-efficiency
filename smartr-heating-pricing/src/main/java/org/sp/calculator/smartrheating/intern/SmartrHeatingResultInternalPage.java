package org.sp.calculator.smartrheating.intern;


import org.sp.example.smartrheating.SmartrHeatingResultPage;

import extensionmodel.smarteff.smartrheating.SmartrHeatingResult;
import extensionmodel.smarteff.smartrheating.intern.SmartrHeatingResultPricing;


public class SmartrHeatingResultInternalPage extends SmartrHeatingResultPage {
	
	@Override
	public Class<SmartrHeatingResultPricing> primaryEntryTypeClass() {
		return SmartrHeatingResultPricing.class;
	}

	@Override
	public void setData(SmartrHeatingResult result) {
		super.setData(result);
		SmartrHeatingResultPricing resultP = (SmartrHeatingResultPricing) result;
		setLabel(resultP.multiBuildResult(), EN, "base pricing results");
	}
}
