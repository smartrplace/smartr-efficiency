package org.sp.example.smarteff.eval.capability;

import org.smartrplace.efficiency.api.base.SmartEffResource;
import org.smartrplace.extensionservice.ApplicationManagerSPExt;
import org.smartrplace.extensionservice.proposal.CalculatedData;
import org.smartrplace.extensionservice.resourcecreate.ExtensionResourceAccessInitData;
import org.smartrplace.smarteff.util.CapabilityHelper;
import org.smartrplace.smarteff.util.MyParam;
import org.smartrplace.smarteff.util.ProposalEvalProviderBase;
import org.sp.example.smarteff.eval.provider.ElectricityProfileEvalProvider;

import de.iwes.util.resource.ValueResourceHelper;
import de.iwes.widgets.api.widgets.localisation.OgemaLocale;
import extensionmodel.smarteff.api.common.BuildingData;
import extensionmodel.smarteff.defaultproposal.CalculatedEvalResult;
import extensionmodel.smarteff.electricity.example.ElectricityProfileEvalConfig;

public class BuildingPresenceEval extends ProposalEvalProviderBase<BuildingData, ElectricityProfileEvalProvider> {
	public static final int DEFAULT_INTERVALS_TO_CALCULATE = 3;
	//private Resource generalData;	
	
	@Override
	public String label(OgemaLocale locale) {
		if(locale == OgemaLocale.GERMAN) return "Anwesenheitsanalyse für ein Gebäude";
		return "User Presence time analysis for building";
	}
	@Override
	protected void calculateProposal(BuildingData input,long startTime, long endTime,
			CalculatedData result, ExtensionResourceAccessInitData data) {
		//DefaultProviderParams params = CapabilityHelper.getSubResourceSingle(appManExt.globalData(), DefaultProviderParams.class);
		MyParam<ElectricityProfileEvalConfig> paramHelper = CapabilityHelper.getMyParams(ElectricityProfileEvalConfig.class, data.userData(), appManExt);
		ElectricityProfileEvalConfig myPar = paramHelper.get();
		data.getEvaluationManagement().calculateKPIs(evalProvider, input, myPar, null, true, DEFAULT_INTERVALS_TO_CALCULATE);
		
		CalculatedEvalResult bRes = (CalculatedEvalResult) result;
		bRes.startTimes().create();
		paramHelper.close();
	}
	
	@Override
	protected Class<? extends CalculatedEvalResult> getResultType() {
		return CalculatedEvalResult.class;
	}
	@Override
	protected Class<BuildingData> typeClass() {
		return BuildingData.class;
	}
	@Override
	protected Class<? extends SmartEffResource> getParamType() {
		return ElectricityProfileEvalConfig.class;
	}
	
	@Override
	protected boolean initParams(SmartEffResource paramsIn) {
		ElectricityProfileEvalConfig params = (ElectricityProfileEvalConfig)paramsIn;
		if(ValueResourceHelper.setIfNew(params.peakPrice(), ElectricityProfileEvalProvider.DEFAULT_PEAK_PRICE) |
				ValueResourceHelper.setIfNew(params.offpeakPrice(), ElectricityProfileEvalProvider.DEFAULT_OFFPEAK_PRICE) |
				ValueResourceHelper.setIfNew(params.addPower(), 0)) {
			return true;
		}
		return false;
	}

	public BuildingPresenceEval(ApplicationManagerSPExt appManExt) {
		super(appManExt);
	}

	@Override
	protected ElectricityProfileEvalProvider evalProvider() {
		return new ElectricityProfileEvalProvider();
	}
}
