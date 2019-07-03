package org.smartrplace.smarteff.util;

import org.smartrplace.efficiency.api.base.SmartEffResource;
import org.smartrplace.extensionservice.ApplicationManagerSPExt;
import org.smartrplace.extensionservice.proposal.CalculatedData;
import org.smartrplace.extensionservice.proposal.ProjectProposal100EE;
import org.smartrplace.extensionservice.resourcecreate.ExtensionResourceAccessInitData;

/**
 * Base provider for {@link ProjectProposal100EE} providers.
 * @param <T> Type of resource that is required as input. See {@link LogicProviderBase} for details.
 */
public abstract class ProjectProviderBase100EE<T extends SmartEffResource>  extends LogicProviderBase<T> {
	protected abstract void calculateProposal(T input, ProjectProposal100EE result, ExtensionResourceAccessInitData data);
	@Override
	protected void calculateProposal(T input, CalculatedData result, ExtensionResourceAccessInitData data) {
		if(!typeClass().isAssignableFrom(input.getClass())) {
			T subRes = CapabilityHelper.getSubResourceOfTypeSingleIfExisting(input, typeClass());
			if(subRes.isActive()) calculateProposal(subRes, (ProjectProposal100EE)result, data);
			else throw new IllegalStateException("Resource "+input.getLocation()+" is not suitable as input for "+this.getClass().getSimpleName());
			return;
		}
		calculateProposal(input, (ProjectProposal100EE)result, data);
	}
	@Override
	protected abstract Class<? extends ProjectProposal100EE> getResultType();
	
	public ProjectProviderBase100EE(ApplicationManagerSPExt appManExt) {
		super(appManExt);
	}
}
