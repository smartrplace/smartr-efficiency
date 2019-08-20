package org.sp.example.smartrheating;

import org.smartrplace.efficiency.api.base.SmartEffResource;
import org.smartrplace.extensionservice.ApplicationManagerSPExt;
import org.smartrplace.extensionservice.proposal.ProjectProposal;
import org.smartrplace.extensionservice.proposal.ProjectProposalEfficiency;
import org.smartrplace.extensionservice.resourcecreate.ExtensionResourceAccessInitData;
import org.smartrplace.smarteff.util.BaseInits;
import org.smartrplace.smarteff.util.CapabilityHelper;
import org.smartrplace.smarteff.util.MyParam;
import org.smartrplace.smarteff.util.ProjectProviderBase;
import org.sp.example.smartrheating.util.BasicCalculations;

import de.iwes.util.resource.ValueResourceHelper;
import de.iwes.widgets.api.widgets.localisation.OgemaLocale;
import extensionmodel.smarteff.api.common.BuildingData;
import extensionmodel.smarteff.defaultproposal.DefaultProviderParams;
import extensionmodel.smarteff.smartrheating.SmartrHeatingData;
import extensionmodel.smarteff.smartrheating.SmartrHeatingParams;
import extensionmodel.smarteff.smartrheating.SmartrHeatingResult;

public class SmartrHeatingEval extends ProjectProviderBase<SmartrHeatingData> {
	
	public static final String WIKI_LINK =
			"https://github.com/smartrplace/smartr-efficiency/blob/master/SmartrHeating.md";

	@Override
	public String label(OgemaLocale locale) {
		return "Single room heating optimization calculator";
	}

	@Override
	protected void calculateProposal(SmartrHeatingData data, ProjectProposal resultIn, ExtensionResourceAccessInitData dataExt) {
		MyParam<SmartrHeatingParams> paramHelper = CapabilityHelper.getMyParams(SmartrHeatingParams.class, dataExt.userData(), appManExt);
		SmartrHeatingParams param = paramHelper.get();
		SmartrHeatingResult result = (SmartrHeatingResult) resultIn;
		
		MyParam<DefaultProviderParams> paramHelperB = CapabilityHelper.getMyParams(DefaultProviderParams.class, dataExt.userData(), appManExt);
		DefaultProviderParams myParB = paramHelperB.get();
		BuildingData building = data.getParent();
		
		calculateInternal(data, result, dataExt, building, param, myParB);
		paramHelper.close();
	}
	protected void calculateInternal(SmartrHeatingData data, SmartrHeatingResult result,
			ExtensionResourceAccessInitData dataExt,
			BuildingData building, SmartrHeatingParams param, DefaultProviderParams myParB) {

		////////////
		// LastCalc
		////////////
		
		//Heat energy consumption
		float wwEnergyPre;
		if(data.wwIsContained().getValue()) {
			float wwBase = data.wwConsumption().getValue()*(4.19f/3.6f)*
					(data.wwTemp().getValue() - param.wwSupplyTemp().getValue());
			float wwWinter = wwBase * data.heatingDaysManual().getValue()/365/
					(1-data.wwLossUnheatedAreas().getValue());
			float wwSummer = wwBase * (365 - data.heatingDaysManual().getValue())/365/
					(1-data.wwLossUnheatedAreas().getValue()-data.wwLossHeatedAreas().getValue());
			wwEnergyPre = wwWinter + wwSummer;
		} else
			wwEnergyPre = 0;
		float yearlykWh = BasicCalculations.getYearlyConsumption(building, 3, myParB).avKWh;
		float heatEnergyPre = yearlykWh - wwEnergyPre;
		ValueResourceHelper.setCreate(result.wwEnergyPreRenovation(), wwEnergyPre);
		ValueResourceHelper.setCreate(result.heatingEnergyPreRenovation(), heatEnergyPre);
		
		//Radiators
		int[] thermNum = BasicCalculations.getNumberOfRadiators(building);
		ValueResourceHelper.setCreate(result.thermostatNum(), thermNum[0]);
		ValueResourceHelper.setCreate(result.roomNumInBuilding(), BasicCalculations.getNumberOfRooms(building));
		ValueResourceHelper.setCreate(result.roomNumWithThermostats(), thermNum[1]);
		
		float heatDownUpPerBlock = (data.coolingDownHours().getValue() + data.heatingUpHours().getValue())*0.5f;
		float hrWithout = data.usageTimePerWeek().getValue() +
				data.usageBlocksPerWeek().getValue() * heatDownUpPerBlock;
		float hrBefore = data.heatingReductionHoursBefore().getValue() -
				data.heatingReductionBlocksBefore().getValue() * heatDownUpPerBlock;
		//Savings
		ValueResourceHelper.setCreate(result.hoursWithoutLowering(), hrWithout);
		ValueResourceHelper.setCreate(result.hoursLoweringEffectiveBefore(), hrBefore);
		ValueResourceHelper.setCreate(result.savingsRelative(), (168-hrWithout-hrBefore)/(168-hrBefore));
		ValueResourceHelper.setCreate(result.savingsAbsolute(), heatEnergyPre * result.savingsRelative().getValue());
		
		/////////////////////////////
		//final result in sheet data
		/////////////////////////////
		float yearlySavings = result.savingsAbsolute().getValue() * data.gasPricePerkWh().getValue();
		float yearlyCO2Savings = result.savingsAbsolute().getValue() * param.co2factorGas().getValue();
		ValueResourceHelper.setCreate(result.yearlySavings(), yearlySavings);
		ValueResourceHelper.setCreate(result.yearlyCO2savings(), yearlyCO2Savings);
	}
	
	@Override
	protected Class<? extends ProjectProposalEfficiency> getResultType() {
		return SmartrHeatingResult.class;
	}
	@Override
	protected Class<SmartrHeatingData> typeClass() {
		return SmartrHeatingData.class;
	}
	@Override
	public Class<? extends SmartEffResource> getParamType() {
		return SmartrHeatingParams.class;
	}
	
	@Override
	protected boolean initParams(SmartEffResource paramsIn) {
		BaseInits.initSmartrEffPriceData(appManExt, this.getClass().getName());
		SmartrHeatingParams params = (SmartrHeatingParams)paramsIn;
		if(ValueResourceHelper.setIfNew(params.wwSupplyTemp(), 8+273.15f) |
				ValueResourceHelper.setIfNew(params.costOfCustomerPerRoom(), 20) |
				ValueResourceHelper.setIfNew(params.internalParamProvider(), "master") |
				ValueResourceHelper.setIfNew(params.co2factorGas(), 0.22f)) {
			return true;
		}
		return false;
	}
	

	public SmartrHeatingEval(ApplicationManagerSPExt appManExt) {
		super(appManExt);
	}
	
	@Override
	public String userName() {
		return "master";
	}
}
