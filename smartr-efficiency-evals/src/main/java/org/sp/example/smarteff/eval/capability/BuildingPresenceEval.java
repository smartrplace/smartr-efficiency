package org.sp.example.smarteff.eval.capability;

import org.smartrplace.efficiency.api.base.SmartEffResource;
import org.smartrplace.extensionservice.ApplicationManagerSPExt;
import org.smartrplace.extensionservice.proposal.CalculatedData;
import org.smartrplace.extensionservice.resourcecreate.ExtensionResourceAccessInitData;
import org.smartrplace.smarteff.util.CapabilityHelper;
import org.smartrplace.smarteff.util.MyParam;
import org.smartrplace.smarteff.util.ProposalEvalProviderBase;
import org.sp.example.smarteff.eval.provider.BuildingPresenceEvalProvider;

import de.iwes.util.resource.ValueResourceHelper;
import de.iwes.widgets.api.widgets.localisation.OgemaLocale;
import extensionmodel.smarteff.api.common.BuildingData;
import extensionmodel.smarteff.basic.evals.BuildingEvalData;
import extensionmodel.smarteff.defaultproposal.CalculatedEvalResult;

public class BuildingPresenceEval extends ProposalEvalProviderBase<BuildingData, BuildingPresenceEvalProvider> {
	public static final int DEFAULT_INTERVALS_TO_CALCULATE = 3;
	//private Resource generalData;	
	
	@Override
	public String label(OgemaLocale locale) {
		if(locale == OgemaLocale.GERMAN) return "Anwesenheitsanalyse für ein Gebäude";
		return "User Presence time analysis for building";
	}
	@Override
	protected void calculateProposal(BuildingData input, Long startTime, Long endTime,
			CalculatedData result, ExtensionResourceAccessInitData data) {
		//DefaultProviderParams params = CapabilityHelper.getSubResourceSingle(appManExt.globalData(), DefaultProviderParams.class);
		MyParam<BuildingEvalData> paramHelper = CapabilityHelper.getMyParams(BuildingEvalData.class, data.userData(), appManExt);
		BuildingEvalData myPar = paramHelper.get();
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
		return BuildingEvalData.class;
	}
	
	@Override
	protected boolean initParams(SmartEffResource paramsIn) {
		BuildingEvalData params = (BuildingEvalData)paramsIn;
		if(ValueResourceHelper.setIfNew(params.minimumAbsenceTime(), BuildingPresenceEvalProvider.DEFAULT_MINIMUM_ABSENCE)) {
			return true;
		}
		return false;
	}

	public BuildingPresenceEval(ApplicationManagerSPExt appManExt) {
		super(appManExt);
	}

	@Override
	protected BuildingPresenceEvalProvider evalProvider() {
		return new BuildingPresenceEvalProvider();
	}
}
