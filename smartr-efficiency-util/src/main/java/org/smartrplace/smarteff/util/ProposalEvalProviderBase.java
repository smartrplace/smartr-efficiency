package org.smartrplace.smarteff.util;

import java.util.Arrays;
import java.util.List;

import org.ogema.core.model.Resource;
import org.ogema.core.model.simple.TimeResource;
import org.smartrplace.efficiency.api.base.SmartEffResource;
import org.smartrplace.extensionservice.ApplicationManagerSPExt;
import org.smartrplace.extensionservice.proposal.CalculatedData;
import org.smartrplace.extensionservice.resourcecreate.ExtensionResourceAccessInitData;

import de.iwes.timeseries.eval.garo.api.base.GaRoDataType;
import de.iwes.timeseries.eval.garo.multibase.GaRoSingleEvalProvider;
import extensionmodel.smarteff.defaultproposal.CalculatedEvalResult;

public abstract class ProposalEvalProviderBase<T extends SmartEffResource, E extends GaRoSingleEvalProvider>  extends ProposalProviderBase<T> {
	protected void calculateProposal(T input, CalculatedData result, ExtensionResourceAccessInitData data) {
		throw new IllegalStateException("Only use calculateProposal with eval signature here!");
	};
	protected abstract void calculateProposal(T input,
			Long startTime, Long endTime,
			CalculatedData result, ExtensionResourceAccessInitData data);
	@Override
	protected abstract Class<? extends CalculatedEvalResult> getResultType();
	protected abstract Class<T> typeClass();
	protected abstract E evalProvider();
	
	protected final E evalProvider;
	
	/** Override these methods if more than one result shall be supported*/
	@Override
	public List<Resource> calculate(ExtensionResourceAccessInitData data) {
		T input = getReqData(data);
		CalculatedData result = input.addDecorator(CapabilityHelper.getSingleResourceName(getResultType()), getResultType());
		
		TimeResource start = input.getSubResource("startTime", TimeResource.class);
		TimeResource end = input.getSubResource("endTime", TimeResource.class);
		
		if(start.isActive() && end.isActive())
			calculateProposal(input, start.getValue(), end.getValue(), result, data);
		else
			calculateProposal(input, null, null, result, data);
		
		//calculateProposal(input, result, data);
		result.activate(true);
		return Arrays.asList(new CalculatedData[] {result});
	}
	@Override
	public List<CalculationResultType> resultTypes() {
		return Arrays.asList(new CalculationResultType[] {
				new CalculationResultType(getResultType())});
	}

	@Override
	public String id() {
		return ProposalEvalProviderBase.this.getClass().getName();
	}

	@Override
	public void init(ApplicationManagerSPExt appManExt) {
		//ProjectProviderBase.this.appManExt = appManExt;
		//add param and init
		if(getParamType() != null) {
			SmartEffResource params = CapabilityHelper.getSubResourceSingle(appManExt.globalData(), getParamType(), appManExt);
			params.create();
			if(initParams(params)) params.activate(true);
		}
	}
	
	public ProposalEvalProviderBase(ApplicationManagerSPExt appManExt) {
		super(appManExt);
		evalProvider = evalProvider();
	}
	
	@Override
	public List<EntryType> getEntryTypes() {
		List<EntryType> result = CapabilityHelper.getStandardEntryTypeList(typeClass());
		for(GaRoDataType inp: evalProvider.getGaRoInputTypes()) {
			result.add(CapabilityHelper.getEntryType(inp));
		}
		return result;
	}
	
	/*public List<GaRoMultiEvalDataProvider<?>> getDataProvidersToUse() {
		StringArrayResource dataProvidersToUse;
		if(!dataProvidersToUse.isActive() || dataProvidersToUse.getValues().length == 0)
			return null;
		List<GaRoMultiEvalDataProvider<?>> result = new ArrayList<>();
		for(String s: dataProvidersToUse.getValues()) {
			GaRoMultiEvalDataProvider<?> dp = getDataProvider(s);
			if(dp != null) result.add(dp);
		}
		return result ;
	}*/
}
