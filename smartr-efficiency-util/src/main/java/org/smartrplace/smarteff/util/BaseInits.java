package org.smartrplace.smarteff.util;

import org.smartrplace.extensionservice.ApplicationManagerSPExt;
import org.smartrplace.smarteff.util.CapabilityHelper;
import org.smartrplace.smarteff.util.LogicProviderBase;

import de.iwes.util.resource.ValueResourceHelper;
import extensionmodel.smarteff.api.base.SmartEffPriceData;

public class BaseInits {
	public static void initSmartrEffPriceData(ApplicationManagerSPExt appManExt, String moduleClassName) {
		SmartEffPriceData params = CapabilityHelper.getSubResourceSingle(appManExt.globalData(),
				SmartEffPriceData.class, appManExt);
		params.create();
		if(ValueResourceHelper.setIfNew(params.gasPriceBase(), 143) |
				ValueResourceHelper.setIfNew(params.gasPricePerkWh(), 0.0532f) |
				ValueResourceHelper.setIfNew(params.electrictiyPriceBase(), 184) |
				ValueResourceHelper.setIfNew(params.electrictiyPricePerkWh(), 0.249f) |
				ValueResourceHelper.setIfNew(params.oilPriceBase(), 70) |
				ValueResourceHelper.setIfNew(params.oilPricePerkWh(), 0.06814f) |
				ValueResourceHelper.setIfNew(params.standardRoomNum(), 8) |
				ValueResourceHelper.setIfNew(params.yearlyInterestRate(), 0.01f)) {
			params.activate(true);
		}
		LogicProviderBase.addAccessControlResource(params, moduleClassName);
	}
}
