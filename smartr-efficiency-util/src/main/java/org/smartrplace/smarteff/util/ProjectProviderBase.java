package org.smartrplace.smarteff.util;

import org.smartrplace.efficiency.api.base.SmartEffResource;
import org.smartrplace.extensionservice.ApplicationManagerSPExt;
import org.smartrplace.extensionservice.proposal.CalculatedData;
import org.smartrplace.extensionservice.proposal.ProjectProposal;
import org.smartrplace.extensionservice.resourcecreate.ExtensionResourceAccessInitData;

public abstract class ProjectProviderBase<T extends SmartEffResource>  extends ProposalProviderBase<T> {
	protected abstract void calculateProposal(T input, ProjectProposal result, ExtensionResourceAccessInitData data);
	@Override
	protected void calculateProposal(T input, CalculatedData result, ExtensionResourceAccessInitData data) {
		calculateProposal(input, (ProjectProposal)result, data);
	}
	@Override
	protected abstract Class<? extends ProjectProposal> getResultType();
	
	public ProjectProviderBase(ApplicationManagerSPExt appManExt) {
		super(appManExt);
	}
}
