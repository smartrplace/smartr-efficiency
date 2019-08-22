package org.sp.calculator.smartrheating.intern;

import org.ogema.core.model.ResourceList;
import org.smartrplace.efficiency.api.base.SmartEffResource;
import org.smartrplace.extensionservice.ApplicationManagerSPExt;
import org.smartrplace.extensionservice.proposal.ProjectProposalEfficiency;
import org.smartrplace.extensionservice.resourcecreate.ExtensionResourceAccessInitData;
import org.smartrplace.smarteff.util.CapabilityHelper;
import org.sp.calculator.multibuild.MultiBuildEval;
import org.sp.example.smartrheating.SmartrHeatingEval;

import de.iwes.util.resource.ValueResourceHelper;
import de.iwes.widgets.api.widgets.localisation.OgemaLocale;
import extensionmodel.smarteff.api.common.BuildingData;
import extensionmodel.smarteff.defaultproposal.DefaultProviderParams;
import extensionmodel.smarteff.multibuild.BuildingComponentUsage;
import extensionmodel.smarteff.multibuild.MultiBuildData;
import extensionmodel.smarteff.smartrheating.SmartrHeatingData;
import extensionmodel.smarteff.smartrheating.SmartrHeatingParams;
import extensionmodel.smarteff.smartrheating.SmartrHeatingResult;
import extensionmodel.smarteff.smartrheating.intern.SmartrHeatingInternalParams;
import extensionmodel.smarteff.smartrheating.intern.SmartrHeatingResultPricing;


public class SmartrHeatingEvalInternal extends SmartrHeatingEval {
	
	@Override
	public String label(OgemaLocale locale) {
		return "Single room heating optimization by Smartrplace";
	}

	
	@Override
	protected void calculateInternal(SmartrHeatingData data, SmartrHeatingResult resultIn,
			ExtensionResourceAccessInitData dataExt, BuildingData building, SmartrHeatingParams param,
			DefaultProviderParams myParB) {
		super.calculateInternal(data, resultIn, dataExt, building, param, myParB);
		SmartrHeatingResultPricing result = (SmartrHeatingResultPricing) resultIn;
		String internalUser = userName();
		SmartrHeatingInternalParams internal = dataExt.getCrossuserAccess().getAccess("smartrHeatingInternalParams", internalUser,
				SmartrHeatingInternalParams.class, this);
		MultiBuildData multiBuild = CapabilityHelper.addMultiTypeToList(building, null, MultiBuildData.class);

		//First perform sub calculation MultiBuild
		MultiBuildEval mbe = new MultiBuildEval(appManExt);
		ValueResourceHelper.setCreate(multiBuild.buildingNum(), 1);
		ValueResourceHelper.setCreate(multiBuild.operationalCost(), 20);
		ValueResourceHelper.setCreate(multiBuild.otherInitialCost(),
				internal.costPerRoom().getValue() * result.roomNumWithThermostats().getValue());
		ResourceList<BuildingComponentUsage> hw = multiBuild.buildingComponentUsage().create();
		BuildingComponentUsage thermo = hw.add();
		ValueResourceHelper.setCreate(thermo.name(), "Homematic Thermostat");
		ValueResourceHelper.setCreate(thermo.number(), result.thermostatNum().getValue());		
		ValueResourceHelper.setCreate(thermo.additionalCostPerItem(), internal.costPerThermostat().getValue());		
		mbe.calculateMultiBuild(multiBuild, result.multiBuildResult(), dataExt);

		//final result in sheet data
		float costOfProject = result.multiBuildResult().costOfProject().getValue();
		float yearlyCost = result.multiBuildResult().yearlyOperatingCosts().getValue();
		float yearlySavings = result.yearlySavings().getValue();
		ValueResourceHelper.setCreate(result.costOfProject(), costOfProject);
		ValueResourceHelper.setCreate(result.yearlyOperatingCosts(), yearlyCost);
		float amortization = costOfProject / (yearlyCost - yearlySavings);
		ValueResourceHelper.setCreate(result.amortization(), amortization);
	}

	/* * * * * * * * * * * * * * * * * * * * * * *
	 *   PROJECT PROVIDER FUNCTIONS              *
	 * * * * * * * * * * * * * * * * * * * * * * */

	@Override
	public Class<? extends SmartEffResource> getInternalParamType() {
		return SmartrHeatingInternalParams.class;
	}
	
	@Override
	protected Class<? extends ProjectProposalEfficiency> getResultType() {
		return SmartrHeatingResultPricing.class;
	}
	
	@Override
	protected boolean initInternalParams(SmartEffResource paramsIn) {
		SmartrHeatingInternalParams params = (SmartrHeatingInternalParams)paramsIn;
		if(//ValueResourceHelper.setIfNew(params.baseCost(), 2000) |
				ValueResourceHelper.setIfNew(params.costPerThermostat(), 70) |
				ValueResourceHelper.setIfNew(params.costPerRoom(), 30)) {
			return true;
		}
		return false;
	}
	
	public SmartrHeatingEvalInternal(ApplicationManagerSPExt appManExt) {
		super(appManExt);
	}
}
