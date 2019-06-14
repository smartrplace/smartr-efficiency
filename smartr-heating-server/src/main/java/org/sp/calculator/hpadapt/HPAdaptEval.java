package org.sp.calculator.hpadapt;

import org.smartrplace.efficiency.api.base.SmartEffResource;
import org.smartrplace.extensionservice.ApplicationManagerSPExt;
import org.smartrplace.extensionservice.proposal.ProjectProposal;
import org.smartrplace.extensionservice.resourcecreate.ExtensionResourceAccessInitData;
import org.smartrplace.smarteff.util.CapabilityHelper;
import org.smartrplace.smarteff.util.MyParam;
import org.smartrplace.smarteff.util.ProjectProviderBase;
import org.sp.example.smartrheating.util.BaseInits;
import org.sp.example.smartrheating.util.BasicCalculations;
import org.sp.example.smartrheating.util.BasicCalculations.YearlyConsumption;

import de.iwes.util.resource.ValueResourceHelper;
import de.iwes.widgets.api.widgets.localisation.OgemaLocale;
import extensionmodel.smarteff.api.base.SmartEffPriceData;
import extensionmodel.smarteff.api.common.BuildingData;
import extensionmodel.smarteff.defaultproposal.DefaultProviderParams;
import extensionmodel.smarteff.hpadapt.HPAdaptData;
import extensionmodel.smarteff.hpadapt.HPAdaptParams;
import extensionmodel.smarteff.hpadapt.HPAdaptResult;

public class HPAdaptEval extends ProjectProviderBase<HPAdaptData> {
	
	@Override
	public String label(OgemaLocale locale) {
		return "Bivalent heat pump refurbishment";
	}

	@Override
	protected void calculateProposal(HPAdaptData input, ProjectProposal resultProposal, ExtensionResourceAccessInitData data) {
		
		if(!(resultProposal instanceof HPAdaptResult)) {
			// TODO Log error and throw exception
			System.out.println("Wrong Result type!");
			return;
		}
		else {
			HPAdaptResult result = (HPAdaptResult) resultProposal;
			calculateHPAdapt(input, result, data);
		}
	}
	
	protected void calculateHPAdapt(HPAdaptData input, HPAdaptResult result, ExtensionResourceAccessInitData data) {
		
		MyParam<HPAdaptParams> paramHelper = CapabilityHelper.getMyParams(HPAdaptParams.class, data.userData(), appManExt);
		HPAdaptParams myPar = paramHelper.get();
		
		MyParam<DefaultProviderParams> paramHelperB = CapabilityHelper.getMyParams(DefaultProviderParams.class, data.userData(), appManExt);
		DefaultProviderParams myParB = paramHelperB.get();
		float kwHpSQM = myParB.defaultKwhPerSQM().getValue();
		
		float yearlykWh;
		float yearlyCost;
		BuildingData building = input.getParent();
		if(building.heatCostBillingInfo().isActive()) {
			//Calculate yearly energy consumption based on billing info
			//throw new UnsupportedOperationException("BillingInfo usage not implemented yet");
			YearlyConsumption avData = BasicCalculations.getYearlyConsumption(
					building.heatCostBillingInfo(), 3);
			yearlykWh = avData.avKWh;
			yearlyCost = avData.avCostTotal;
		} else {
			yearlykWh = building.heatedLivingSpace().getValue() * kwHpSQM;
			yearlyCost = yearlykWh * 0.06f;
			//TODO: use price data and calculate CO2
			building.heatSource();
		}
		result.yearlySavings().create();
		result.yearlySavings().setValue(0.05f*yearlyCost);
		
		Integer roomNum = BasicCalculations.getNumberOfRooms(building);
		if(roomNum == 0) {
			MyParam<SmartEffPriceData> paramHelperPrice = CapabilityHelper.getMyParams(SmartEffPriceData.class, data.userData(), appManExt);
			SmartEffPriceData myParPrice = paramHelperPrice.get();
			roomNum = myParPrice.standardRoomNum().getValue(); 
		}
		
		result.costOfProject().create();
		result.costOfProject().setValue(999);
		
		result.costOfProjectIncludingInternal().create();
		result.costOfProjectIncludingInternal().setValue(9999);
		
		result.wwEnergyPreRenovation().create();
		result.wwEnergyPreRenovation().setValue(473.5f);
		
		paramHelper.close();
	}
	
	@Override
	protected Class<? extends ProjectProposal> getResultType() {
		return HPAdaptResult.class;
	}
	@Override
	protected Class<HPAdaptData> typeClass() {
		return HPAdaptData.class;
	}
	@Override
	public Class<? extends SmartEffResource> getParamType() {
		return HPAdaptParams.class;
	}
	
	@Override
	protected boolean initParams(SmartEffResource paramsIn) {
		BaseInits.initSmartrEffPriceData(appManExt, this.getClass().getName());
		HPAdaptParams params = (HPAdaptParams)paramsIn;

		// Perhaps move to properties?
		if(
				ValueResourceHelper.setIfNew(params.electrictiyPriceCO2neutralPerkWh(), 0.259f) |
				ValueResourceHelper.setIfNew(params.electrictiyPrice100EEPerkWh(), 0.249f) |
				ValueResourceHelper.setIfNew(params.electrictiyPriceHeatBase(), 112) |
				ValueResourceHelper.setIfNew(params.electrictiyPriceHeatPerkWh(), 0.191f) |
				ValueResourceHelper.setIfNew(params.electrictiyPriceHeatCO2neutralPerkWh(), 0.201f) |
				ValueResourceHelper.setIfNew(params.electrictiyPriceHeat100EEPerkWh(), 0.201f) |
				ValueResourceHelper.setIfNew(params.gasPriceCO2neutralPerkWh(), 0.099f) |
				ValueResourceHelper.setIfNew(params.gasPrice100EEPerkWh(), 0.15f) |
				ValueResourceHelper.setIfNew(params.boilerChangeCDtoCD(), 4000) |
				ValueResourceHelper.setIfNew(params.boilerChangeLTtoCD(), 7000) |
				ValueResourceHelper.setIfNew(params.boilerChangeCDtoCDAdditionalPerkW(), 100) |
				ValueResourceHelper.setIfNew(params.boilerChangeLTtoCDAdditionalPerkW(), 200) |
				ValueResourceHelper.setIfNew(params.additionalBivalentHPBase(), 5000) |
				ValueResourceHelper.setIfNew(params.additionalBivalentHPPerkW(), 100) |
				ValueResourceHelper.setIfNew(params.boilerPowerReductionLTtoCD(), 10) |
				ValueResourceHelper.setIfNew(params.wwSupplyTemp(), (273.15f + 8.0f))
		) {
			return true;
		}
		return false;
	}
	
	public HPAdaptEval(ApplicationManagerSPExt appManExt) {
		super(appManExt);
	}
	
	@Override
	public String userName() {
		return "master";
	}
}
