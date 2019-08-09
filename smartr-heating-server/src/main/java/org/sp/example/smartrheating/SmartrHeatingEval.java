package org.sp.example.smartrheating;

import org.smartrplace.efficiency.api.base.SmartEffResource;
import org.smartrplace.extensionservice.ApplicationManagerSPExt;
import org.smartrplace.extensionservice.proposal.ProjectProposal;
import org.smartrplace.extensionservice.proposal.ProjectProposalEfficiency;
import org.smartrplace.extensionservice.resourcecreate.ExtensionResourceAccessInitData;
import org.smartrplace.smarteff.util.CapabilityHelper;
import org.smartrplace.smarteff.util.MyParam;
import org.smartrplace.smarteff.util.ProjectProviderBase;
import org.sp.example.smartrheating.util.BaseInits;
import org.sp.example.smartrheating.util.BasicCalculations;

import de.iwes.util.resource.ValueResourceHelper;
import de.iwes.widgets.api.widgets.localisation.OgemaLocale;
import extensionmodel.smarteff.api.base.SmartEffPriceData;
import extensionmodel.smarteff.api.common.BuildingData;
import extensionmodel.smarteff.defaultproposal.DefaultProviderParams;
import extensionmodel.smarteff.multibuild.MultiBuildData;
import extensionmodel.smarteff.multibuild.MultiBuildParams;
import extensionmodel.smarteff.smartrheating.SmartrHeatingData;
import extensionmodel.smarteff.smartrheating.SmartrHeatingParams;
import extensionmodel.smarteff.smartrheating.SmartrHeatingResult;
import extensionmodel.smarteff.smartrheating.intern.SmartrHeatingInternalParams;

public class SmartrHeatingEval extends ProjectProviderBase<SmartrHeatingData> {
	
	@Override
	public String label(OgemaLocale locale) {
		return "SmartrHeating Recommendations";
	}

	@Override
	protected void calculateProposal(SmartrHeatingData input, ProjectProposal resultIn, ExtensionResourceAccessInitData data) {
		MyParam<SmartrHeatingParams> paramHelper = CapabilityHelper.getMyParams(SmartrHeatingParams.class, data.userData(), appManExt);
		SmartrHeatingParams myPar = paramHelper.get();
		SmartrHeatingResult result = (SmartrHeatingResult) resultIn;
		
		String internalUser = myPar.internalParamProvider().getValue();
		SmartrHeatingInternalParams internal = data.getCrossuserAccess().getAccess("smartrHeatingInternalParams", internalUser,
				SmartrHeatingInternalParams.class, this);
		
		MyParam<DefaultProviderParams> paramHelperB = CapabilityHelper.getMyParams(DefaultProviderParams.class, data.userData(), appManExt);
		DefaultProviderParams myParB = paramHelperB.get();
		BuildingData building = input.getParent();
		
		MyParam<MultiBuildParams> paramHelperMB = CapabilityHelper.getMyParams(MultiBuildParams.class, data.userData(), appManExt);
		MultiBuildParams myParMB = paramHelperMB.get();
		MultiBuildData multiBuild = CapabilityHelper.addMultiTypeToList(building, null, MultiBuildData.class);
		
		float kwHpSQM = myParB.defaultKwhPerSQM().getValue();
		
		float yearlykWh = BasicCalculations.getYearlyConsumption(building, 3, myParB).avKWh;

		
		result.yearlySavings().create();
		//result.yearlySavings().setValue(0.05f*yearlyCost);
		
		Integer roomNum = BasicCalculations.getNumberOfRooms(building);
		if(roomNum == 0) {
			MyParam<SmartEffPriceData> paramHelperPrice = CapabilityHelper.getMyParams(SmartEffPriceData.class, data.userData(), appManExt);
			SmartEffPriceData myParPrice = paramHelperPrice.get();
			roomNum = myParPrice.standardRoomNum().getValue(); 
		}
		
		//result.costOfProjectIncludingInternal().create();
		//result.costOfProjectIncludingInternal().setValue(vendorCost + customerCost);
		paramHelper.close();
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
	
	@Override
	protected boolean initInternalParams(SmartEffResource paramsIn) {
		SmartrHeatingInternalParams params = (SmartrHeatingInternalParams)paramsIn;
		if(ValueResourceHelper.setIfNew(params.baseCost(), 2000) |
				ValueResourceHelper.setIfNew(params.costPerThermostat(), 70) |
				ValueResourceHelper.setIfNew(params.costPerRoom(), 30)) {
			return true;
		}
		return false;
	}

	public SmartrHeatingEval(ApplicationManagerSPExt appManExt) {
		super(appManExt);
	}
	
	@Override
	public Class<? extends SmartEffResource> getInternalParamType() {
		return SmartrHeatingInternalParams.class;
	}
	
	@Override
	public String userName() {
		return "master";
	}
}
