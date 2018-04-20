package extensionmodel.smarteff.defaultproposal;

import org.smartrplace.efficiency.api.base.SmartEffResource;
import org.smartrplace.extenservice.proposal.ProjectProposal;
import org.smartrplace.extenservice.resourcecreate.ExtensionResourceAccessInitData;
import org.smartrplace.extensionservice.ApplicationManagerSPExt;
import org.smartrplace.smarteff.util.CapabilityHelper;
import org.smartrplace.smarteff.util.MyParam;
import org.smartrplace.smarteff.util.ProjectProviderBase;

import de.iwes.util.resource.ValueResourceHelper;
import de.iwes.widgets.api.widgets.localisation.OgemaLocale;
import extensionmodel.smarteff.api.common.BuildingData;

public class BuildingExampleAnalysis extends ProjectProviderBase<BuildingData> {
	//private Resource generalData;	
	
	@Override
	public String label(OgemaLocale locale) {
		return "Energy consumption analysis and consulting of users";
	}
	@Override
	protected void calculateProposal(BuildingData input, ProjectProposal result, ExtensionResourceAccessInitData data) {
		//DefaultProviderParams params = CapabilityHelper.getSubResourceSingle(appManExt.globalData(), DefaultProviderParams.class);
		MyParam<DefaultProviderParams> paramHelper = CapabilityHelper.getMyParams(DefaultProviderParams.class, data.userData(), appManExt);
		DefaultProviderParams myPar = paramHelper.get();
		float baseCost = myPar.basePriceBuildingAnalysis().getValue();
		float varCost = myPar.pricePerSQMBuildingAnalysis().getValue();
		float customerHourCost = myPar.costOfCustomerHour().getValue();
		float kwHpSQM = myPar.defaultKwhPerSQM().getValue();
		/*float baseCost = CapabilityHelper.floatParam(params.basePriceBuildingAnalysis(), data);
		float varCost = CapabilityHelper.floatParam(params.pricePerSQMBuildingAnalysis(), data);
		float customerHourCost = CapabilityHelper.floatParam(params.costOfCustomerHour(), data);
		float kwHpSQM = CapabilityHelper.floatParam(params.defaultKwhPerSQM(), data);
		*/
		
		float yearlykWh;
		float yearlyCost;
		if(input.heatCostBillingInfo().isActive()) {
			//Calculate yearly energy consumption based on billing info
			throw new UnsupportedOperationException("BillingInfo usage not implemented yet");
		} else {
			yearlykWh = input.heatedLivingSpace().getValue() * kwHpSQM;
			yearlyCost = yearlykWh * 0.06f;
			//TODO: use price data and calculate CO2
			//input.heatSource();
		}
		result.yearlySavings().create();
		result.yearlySavings().setValue(0.05f*yearlyCost);
		
		float customerHours = input.heatedLivingSpace().getValue() * 0.04f;
		
		float cost = baseCost +
				input.heatedLivingSpace().getValue() * varCost;
		result.costOfProject().create();
		result.costOfProject().setValue(cost);
		
		result.costOfProjectIncludingInternal().create();
		result.costOfProjectIncludingInternal().setValue(cost + customerHours*customerHourCost);
		paramHelper.close();
	}
	
	@Override
	protected Class<? extends ProjectProposal> getResultType() {
		return BuildingExampleAnalysisResult.class;
	}
	@Override
	protected Class<BuildingData> typeClass() {
		return BuildingData.class;
	}
	@Override
	protected Class<? extends SmartEffResource> getParamType() {
		return DefaultProviderParams.class;
	}
	
	@Override
	protected boolean initParams(SmartEffResource paramsIn) {
		DefaultProviderParams params = (DefaultProviderParams)paramsIn;
		if(ValueResourceHelper.setIfNew(params.basePriceBuildingAnalysis(), 400) |
				ValueResourceHelper.setIfNew(params.pricePerSQMBuildingAnalysis(), 1.5f) |
				ValueResourceHelper.setIfNew(params.defaultKwhPerSQM(), 140f) |
				ValueResourceHelper.setIfNew(params.costOfCustomerHour(), 40f)) {
			return true;
		}
		return false;
	}

	public BuildingExampleAnalysis(ApplicationManagerSPExt appManExt) {
		super(appManExt);
	}
}
