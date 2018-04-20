package org.smartrplace.smarteff.admin.protect;

import java.util.List;

import org.ogema.core.model.Resource;
import org.smartrplace.extenservice.proposal.ProposalProvider.CalculationResultType;
import org.smartrplace.extenservice.proposal.ProposalPublicData;
import org.smartrplace.extenservice.resourcecreate.ExtensionResourceAccessInitData;
import org.smartrplace.smarteff.admin.object.ProposalProviderData;

public class ProposalPublicDataImpl extends ProviderPublicDataForCreateImpl implements ProposalPublicData {
	private final ProposalProviderData internalData;
	
	public ProposalPublicDataImpl(ProposalProviderData internalData) {
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

}
