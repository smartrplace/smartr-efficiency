package org.smartrplace.smarteff.admin.protect;

import java.util.List;

import org.ogema.core.model.Resource;
import org.smartrplace.extensionservice.proposal.LogicProviderPublicData;
import org.smartrplace.extensionservice.resourcecreate.ExtensionResourceAccessInitData;
import org.smartrplace.smarteff.admin.object.LogicProviderData;

public class ProposalPublicDataImpl extends ProviderPublicDataForCreateImpl implements LogicProviderPublicData {
	private final LogicProviderData internalData;
	
	public ProposalPublicDataImpl(LogicProviderData internalData) {
		super(internalData.provider);
		this.internalData = internalData;
	}

	@Override
	public PagePriority getPriority() {
		return internalData.provider.getPriority();
	}

	@Override
	public List<Resource> calculate(ExtensionResourceAccessInitData data) {
		return internalData.provider.calculate(data);
	}

	@Override
	public List<CalculationResultType> resultTypes() {
		return internalData.provider.resultTypes();
	}

	@Override
	public List<EvaluationResultTypes> getEvaluationResultTypes() {
		return internalData.provider.getEvaluationResultTypes();
	}
	
	//@Override
	//public String userName() {
	//	return internalData.provider.userName();
	//}
	
	@Override
	public String getProviderId() {
		return internalData.provider.getProviderId();
	}

	@Override
	public Class<? extends Resource> getParamType() {
		return internalData.provider.getParamType();
	}
}
