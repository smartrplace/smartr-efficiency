package org.sp.calculator.smartrheating.intern;

import org.smartrplace.efficiency.api.base.SmartEffResource;
import org.smartrplace.extensionservice.ApplicationManagerSPExt;
import org.smartrplace.extensionservice.proposal.ProjectProposal100EE;
import org.smartrplace.extensionservice.resourcecreate.ExtensionResourceAccessInitData;
import org.sp.calculator.hpadapt.HPAdaptEval;
import org.sp.example.smartrheating.util.BaseInits;

import de.iwes.util.resource.ValueResourceHelper;
import de.iwes.widgets.api.widgets.localisation.OgemaLocale;
import extensionmodel.smarteff.api.common.BuildingData;
import extensionmodel.smarteff.hpadapt.HPAdaptData;
import extensionmodel.smarteff.hpadapt.HPAdaptParams;
import extensionmodel.smarteff.hpadapt.HPAdaptResult;
import extensionmodel.smarteff.smartrheating.intern.HPAdaptParamInternal;


public class HPAdaptEvalInternal extends HPAdaptEval {
	
	@Override
	public String label(OgemaLocale locale) {
		return "Bivalent heat pump refurbishment by Smartrplace";
	}

	@Override
	protected void calculateHPAdapt(HPAdaptData hpData, HPAdaptResult result, ExtensionResourceAccessInitData data,
			BuildingData building, HPAdaptParams hpParams) {
		super.calculateHPAdapt(hpData, result, data, building, hpParams);
		String internalUser = hpParams.internalParamProvider().getValue();
		HPAdaptParamInternal hpParamInternal = data.getCrossuserAccess().getAccess("hPAdaptParamInternal", internalUser,
				HPAdaptParamInternal.class, this);
		float boilerPowerBoilerOnly = result.boilerPowerBoilerOnly().getValue() / 1000;

		float hpPowerBivalentHP = result.hpPowerBivalentHP().getValue() /  1000;

		float  boilerPowerBivalentHP = result.boilerPowerBivalentHP().getValue() / 1000;

		//private void calcRemaining(HPAdaptResult result, HPAdaptData hpData, HPAdaptParams hpParams,
	//		HPAdaptParamInternal hpParamInternal, float boilerPowerBivalentHP, float hpPowerBivalentHP, float boilerPowerBoilerOnly) {

		/* GET VALUES */
		boolean isCondensingBurner = building.condensingBurner().getValue();
		
		float boilerChangeCDtoCD = hpParamInternal.boilerChangeCDtoCD().getValue();
		float boilerChangeLTtoCD = hpParamInternal.boilerChangeLTtoCD().getValue();
		float boilerChangeCDtoCDAdditionalPerkW = hpParamInternal.boilerChangeCDtoCDAdditionalPerkW().getValue();
		float boilerChangeLTtoCDAdditionalPerkW = hpParamInternal.boilerChangeLTtoCDAdditionalPerkW().getValue();
		float additionalBivalentHPBase = hpParamInternal.additionalBivalentHPBase().getValue();
		float additionalBivalentHPPerkW = hpParamInternal.additionalBivalentHPPerkW().getValue();
		
		float boilerChangeCostHP;
		if (isCondensingBurner)
			boilerChangeCostHP = boilerChangeCDtoCD + boilerChangeCDtoCDAdditionalPerkW * boilerPowerBivalentHP;
		else
			boilerChangeCostHP = boilerChangeLTtoCD + boilerChangeLTtoCDAdditionalPerkW * boilerPowerBivalentHP;
		float costOfInstallingBivalentSystem = boilerChangeCostHP + additionalBivalentHPBase
				+ additionalBivalentHPPerkW * hpPowerBivalentHP;
		
		float costOfInstallingCondensingBoiler;
		if(isCondensingBurner) {
			costOfInstallingCondensingBoiler =
					boilerChangeCDtoCD + boilerChangeCDtoCDAdditionalPerkW * boilerPowerBoilerOnly;
		} else {
			costOfInstallingCondensingBoiler =
					boilerChangeLTtoCD + boilerChangeLTtoCDAdditionalPerkW * boilerPowerBoilerOnly;
		}
		
		ValueResourceHelper.setCreate(result.bivalent().costOfProject(), costOfInstallingBivalentSystem);
		ValueResourceHelper.setCreate(result.condensing().costOfProject(), costOfInstallingCondensingBoiler);
				

		float amortization = (costOfInstallingBivalentSystem - costOfInstallingCondensingBoiler)
				/ (yearlyCostCondensing - yearlyCostBivalent);
		ValueResourceHelper.setCreate(result.bivalent().amortization(), amortization);

	}

	/* * * * * * * * * * * * * * * * * * * * * * *
	 *   PROJECT PROVIDER FUNCTIONS              *
	 * * * * * * * * * * * * * * * * * * * * * * */

	@Override
	protected Class<? extends ProjectProposal100EE> getResultType() {
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
	public Class<? extends SmartEffResource> getInternalParamType() {
		return HPAdaptParamInternal.class;
	}
	
	@Override
	protected boolean initInternalParams(SmartEffResource paramsIn) {
		BaseInits.initSmartrEffPriceData(appManExt, this.getClass().getName());

		HPAdaptParamInternal params = (HPAdaptParamInternal) paramsIn;
		
		if(
				ValueResourceHelper.setIfNew(params.boilerChangeCDtoCD(), 4000) |
				ValueResourceHelper.setIfNew(params.boilerChangeLTtoCD(), 7000) |
				ValueResourceHelper.setIfNew(params.boilerChangeCDtoCDAdditionalPerkW(), 100) |
				ValueResourceHelper.setIfNew(params.boilerChangeLTtoCDAdditionalPerkW(), 200) |
				ValueResourceHelper.setIfNew(params.additionalBivalentHPBase(), 5000) |
				ValueResourceHelper.setIfNew(params.additionalBivalentHPPerkW(), 100)
		) {
			return true;
		}
		return false;
		
	}
	
	public HPAdaptEvalInternal(ApplicationManagerSPExt appManExt) {
		super(appManExt);
	}
	
	@Override
	public String userName() {
		return "master";
	}

}
