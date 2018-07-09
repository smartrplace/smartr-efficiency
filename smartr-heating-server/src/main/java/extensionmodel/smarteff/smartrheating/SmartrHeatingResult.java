package extensionmodel.smarteff.smartrheating;

import org.ogema.core.model.simple.FloatResource;
import org.smartrplace.extensionservice.proposal.ProjectProposal;

public interface SmartrHeatingResult extends ProjectProposal {
	FloatResource hardwareCost();
}
