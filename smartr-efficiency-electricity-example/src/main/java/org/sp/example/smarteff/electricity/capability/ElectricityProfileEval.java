package org.sp.example.smarteff.electricity.capability;

import org.ogema.core.channelmanager.measurements.SampledValue;
import org.ogema.core.timeseries.ReadOnlyTimeSeries;
import org.smartrplace.efficiency.api.base.SmartEffResource;
import org.smartrplace.extensionservice.ApplicationManagerSPExt;
import org.smartrplace.extensionservice.SmartEffTimeSeries;
import org.smartrplace.extensionservice.proposal.CalculatedData;
import org.smartrplace.extensionservice.proposal.CalculatedEvalResult;
import org.smartrplace.extensionservice.resourcecreate.ExtensionResourceAccessInitData;
import org.smartrplace.smarteff.util.CapabilityHelper;
import org.smartrplace.smarteff.util.LogicEvalProviderBase;
import org.smartrplace.smarteff.util.MyParam;
import org.sp.example.smarteff.electricity.provider.ElectricityProfileEvalProvider;

import de.iwes.util.resource.ValueResourceHelper;
import de.iwes.widgets.api.widgets.localisation.OgemaLocale;
import extensionmodel.smarteff.api.common.BuildingData;
import extensionmodel.smarteff.electricity.example.ElectricityProfileEvalConfig;

public class ElectricityProfileEval extends LogicEvalProviderBase<BuildingData, ElectricityProfileEvalProvider> {
	public static final int DEFAULT_INTERVALS_TO_CALCULATE = 3;
	//private Resource generalData;	
	
	@Override
	public String label(OgemaLocale locale) {
		if(locale == OgemaLocale.GERMAN) return "Anwesenheitsanalyse für ein Gebäude";
		return "Electricity profile analysis for building";
	}
	@Override
	protected void calculateProposal(BuildingData input, Long startTime, Long endTime,
			CalculatedData result, ExtensionResourceAccessInitData data) {
		//DefaultProviderParams params = CapabilityHelper.getSubResourceSingle(appManExt.globalData(), DefaultProviderParams.class);
		MyParam<ElectricityProfileEvalConfig> paramHelper = CapabilityHelper.getMyParams(ElectricityProfileEvalConfig.class, data.userData(), appManExt);
		ElectricityProfileEvalConfig myPar = paramHelper.get();
		
		if(startTime == null) {
			SmartEffTimeSeries tsRes = input.electricityMainProfile();
			ReadOnlyTimeSeries ts = data.getTimeseriesManagement().getTimeSeries(tsRes);
			if(ts != null) {
				SampledValue val = ts.getNextValue(0);
				SampledValue val2 = ts.getPreviousValue(Long.MAX_VALUE);
				if(val != null && val2 != null) {
					startTime = val.getTimestamp();
					endTime = val2.getTimestamp();			
				}
			}
		}
		
		long[] res;
		if(startTime != null)
			res = data.getEvaluationManagement().calculateKPIs(evalProvider, input, myPar, null, true, startTime, endTime, null);
		else		
			res = data.getEvaluationManagement().calculateKPIs(evalProvider, input, myPar, null, true, DEFAULT_INTERVALS_TO_CALCULATE, null);
		//res == null is also returned when no gaps found
		//if(res == null) throw new IllegalStateException("Evaluation was not possible for some reason!");
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
	public Class<? extends SmartEffResource> getParamType() {
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

	public ElectricityProfileEval(ApplicationManagerSPExt appManExt) {
		super(appManExt);
	}

	@Override
	protected ElectricityProfileEvalProvider evalProvider() {
		return new ElectricityProfileEvalProvider();
	}
}
