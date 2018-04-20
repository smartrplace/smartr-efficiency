package org.smartrplace.extensionservice.proposal;

import java.util.List;

import org.ogema.core.model.Resource;
import org.smartrplace.extensionservice.proposal.ProposalProvider.CalculationResultType;
import org.smartrplace.extensionservice.resourcecreate.ExtensionResourceAccessInitData;
import org.smartrplace.extensionservice.resourcecreate.ProviderPublicDataForCreate;

public interface ProposalPublicData extends ProviderPublicDataForCreate {

	/** see {@link ProposalProvider#calculate(ExtensionResourceAccessInitData)}*/
	List<Resource> calculate(ExtensionResourceAccessInitData data);
	
	/** see {@link ProposalProvider#resultTypes()}*/
	List<CalculationResultType> resultTypes();
}
