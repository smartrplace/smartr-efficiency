package org.sp.example.smartrheating;

import org.smartrplace.efficiency.api.base.SmartEffResource;
import org.smartrplace.extensionservice.ApplicationManagerSPExt;
import org.smartrplace.extensionservice.proposal.ProjectProposal;
import org.smartrplace.extensionservice.resourcecreate.ExtensionResourceAccessInitData;
import org.smartrplace.smarteff.util.CapabilityHelper;
import org.smartrplace.smarteff.util.MyParam;
import org.smartrplace.smarteff.util.ProjectProviderBase;

import de.iwes.util.resource.ValueResourceHelper;
import de.iwes.widgets.api.widgets.localisation.OgemaLocale;
import extensionmodel.smarteff.api.common.BuildingData;
import extensionmodel.smarteff.defaultproposal.DefaultProviderParams;
import extensionmodel.smarteff.smartrheating.SmartrHeatingData;
import extensionmodel.smarteff.smartrheating.SmartrHeatingInternalParams;
import extensionmodel.smarteff.smartrheating.SmartrHeatingParams;
import extensionmodel.smarteff.smartrheating.SmartrHeatingResult;

public class SmartrHeatingEval extends ProjectProviderBase<SmartrHeatingData> {
	
	@Override
	public String label(OgemaLocale locale) {
		return "SmartrHeating Recommendations";
	}

	@Override
	protected void calculateProposal(SmartrHeatingData input, ProjectProposal result, ExtensionResourceAccessInitData data) {
		MyParam<SmartrHeatingParams> paramHelper = CapabilityHelper.getMyParams(SmartrHeatingParams.class, data.userData(), appManExt);
		SmartrHeatingParams myPar = paramHelper.get();
		
		//TODO: Put vendor data also in resource
		SmartrHeatingInternalParams internal = data.getCrossuserAccess().getAccess("smartrHeatingInternalParams", "master",
				SmartrHeatingInternalParams.class, this);
		float baseCost = internal.baseCost().getValue(); //2000;
		float costPerRad = internal.costPerThermostat().getValue(); //70;
		float costPerRoom = internal.costPerRoom().getValue(); //30;
		float customerCostPerRoom = myPar.costOfCustomerPerRoom().getValue();
		
		/** TODO: We perform same savings calculation here as in BuildingExampleAnalysis. We should find a way to avoid
		 * such doubling of code.
		 * TODO: We do not declare the usage of these parameters yet. Currently this information would not be used,
		 * but if the availability of all parameters would be checked then missing parameters would not be detected
		 */ 
		MyParam<DefaultProviderParams> paramHelperB = CapabilityHelper.getMyParams(DefaultProviderParams.class, data.userData(), appManExt);
		DefaultProviderParams myParB = paramHelperB.get();
		float kwHpSQM = myParB.defaultKwhPerSQM().getValue();
	
		float yearlykWh;
		float yearlyCost;
		BuildingData building = input.getParent();
		if(building.heatCostBillingInfo().isActive()) {
			//Calculate yearly energy consumption based on billing info
			throw new UnsupportedOperationException("BillingInfo usage not implemented yet");
		} else {
			yearlykWh = building.heatedLivingSpace().getValue() * kwHpSQM;
			yearlyCost = yearlykWh * 0.06f;
			//TODO: use price data and calculate CO2
			building.heatSource();
		}
		result.yearlySavings().create();
		result.yearlySavings().setValue(0.05f*yearlyCost);
		
		float customerCost = input.numberOfRooms().getValue() * customerCostPerRoom;
		float vendorCost = input.numberOfRadiators().getValue() * costPerRad +
				input.numberOfRooms().getValue() * costPerRoom + baseCost;
		/*float customerCost = building.roomNum().getValue() * customerCostPerRoom;
		float vendorCost = input.numberOfRadiators().getValue() * costPerRad +
				building.roomNum().getValue() * costPerRoom + baseCost;*/
		
		result.costOfProject().create();
		result.costOfProject().setValue(vendorCost);
		
		result.costOfProjectIncludingInternal().create();
		result.costOfProjectIncludingInternal().setValue(vendorCost + customerCost);
		paramHelper.close();
	}
	
	@Override
	protected Class<? extends ProjectProposal> getResultType() {
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
		SmartrHeatingParams params = (SmartrHeatingParams)paramsIn;
		if(ValueResourceHelper.setIfNew(params.costOfCustomerPerRoom(), 20)) {
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