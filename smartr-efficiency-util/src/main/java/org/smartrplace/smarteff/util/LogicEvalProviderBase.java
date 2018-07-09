package org.smartrplace.smarteff.util;

import java.util.Arrays;
import java.util.List;

import org.ogema.core.model.Resource;
import org.ogema.core.model.simple.TimeResource;
import org.smartrplace.efficiency.api.base.SmartEffResource;
import org.smartrplace.extensionservice.ApplicationManagerSPExt;
import org.smartrplace.extensionservice.proposal.CalculatedData;
import org.smartrplace.extensionservice.proposal.CalculatedEvalResult;
import org.smartrplace.extensionservice.proposal.LogicProvider;
import org.smartrplace.extensionservice.resourcecreate.ExtensionPageSystemAccessForEvaluation;
import org.smartrplace.extensionservice.resourcecreate.ExtensionResourceAccessInitData;

import de.iwes.timeseries.eval.garo.api.base.GaRoDataTypeI;
import de.iwes.timeseries.eval.garo.multibase.GaRoSingleEvalProvider;

/** Template for LogicProviders that use time series as input. Usually such providers calculate KPIs
 * for the provider. These KPIs may be used to provide certain result values in the result resource.<br>
 * Note that currently {@link ExtensionPageSystemAccessForEvaluation} only supports starting
 * OGEMA evaluations for KPI calculation, so under standard permissions only KPI-based evaluation is
 * supported. If an evaluation would need to evaluate exactly startTime to endTime then another
 * mechanism is required. Whether this is required may have to be discussed.<br>
 * In most cases {@link ExtensionPageSystemAccessForEvaluation#calculateKPIs(GaRoSingleEvalProvider, Resource, Resource, List, boolean, int)}
 * is used, so check this method for more details.
 * @param <T> Type of resource that is required as input. See {@link LogicProviderBase} for details.
 * If the input has sub resources named startTime and endTime of type TimeResource these resources
 * are used to determine the respective input values of {@link #calculateProposal(SmartEffResource, Long, Long, CalculatedData, ExtensionResourceAccessInitData)}.
 * All results of the EvaluationProvider are added to the result types of {@link LogicProvider#resultTypes()}.
 * @param <E> OGEMA EvaluationProvider that is used to calculate the results.
 */
public abstract class LogicEvalProviderBase<T extends SmartEffResource, E extends GaRoSingleEvalProvider>  extends LogicProviderBase<T> {
	protected void calculateProposal(T input, CalculatedData result, ExtensionResourceAccessInitData data) {
		throw new IllegalStateException("Only use calculateProposal with eval signature here!");
	};
	protected abstract void calculateProposal(T input,
			Long startTime, Long endTime,
			CalculatedData result, ExtensionResourceAccessInitData data);
	@Override
	protected abstract Class<? extends CalculatedEvalResult> getResultType();
	protected abstract Class<T> typeClass();
	
	/** This method is called once on startup and the implementation needs to provide an instance
	 * of the EvaluationProvider here that will be used over the entire runtime of the module
	 */
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
		return LogicEvalProviderBase.this.getClass().getName();
	}

	/*@Override
	public void init(ApplicationManagerSPExt appManExt) {
		//ProjectProviderBase.this.appManExt = appManExt;
		//add param and init
		if(getParamType() != null) {
			SmartEffResource params = CapabilityHelper.getSubResourceSingle(appManExt.globalData(), getParamType(), appManExt);
			params.create();
			if(initParams(params)) params.activate(true);
		}
		if(getInternalParamType() != null) {
			SmartEffResource params = CapabilityHelper.getSubResourceSingle(userData.editableData(), getInternalParamType(), appManExt);
			params.create();
			if(initInternalParams(params)) params.activate(true);
		}
	}*/
	
	public LogicEvalProviderBase(ApplicationManagerSPExt appManExt) {
		super(appManExt);
		evalProvider = evalProvider();
	}
	
	@Override
	public List<EntryType> getEntryTypes() {
		List<EntryType> result = CapabilityHelper.getStandardEntryTypeList(typeClass());
		for(GaRoDataTypeI inp: evalProvider.getGaRoInputTypes()) {
			result.add(CapabilityHelper.getEntryType(inp));
		}
		return result;
	}
	
	@Override
	public String getProviderId() {
		return evalProvider.id();
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
