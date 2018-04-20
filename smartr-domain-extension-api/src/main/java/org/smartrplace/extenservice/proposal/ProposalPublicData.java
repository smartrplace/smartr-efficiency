package org.smartrplace.extenservice.proposal;

import java.util.List;

import org.ogema.core.model.Resource;
import org.smartrplace.extenservice.proposal.ProposalProvider.CalculationResultType;
import org.smartrplace.extenservice.resourcecreate.ExtensionResourceAccessInitData;
import org.smartrplace.extenservice.resourcecreate.ProviderPublicDataForCreate;

public interface ProposalPublicData extends ProviderPublicDataForCreate {

	/** see {@link ProposalProvider#calculate(ExtensionResourceAccessInitData)}*/
	List<Resource> calculate(ExtensionResourceAccessInitData data);
	
	/** see {@link ProposalProvider#resultTypes()}*/
	List<CalculationResultType> resultTypes();
}
