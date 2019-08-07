package extensionmodel.smarteff.smartrheating;

import org.ogema.core.model.simple.FloatResource;
import org.smartrplace.extensionservice.proposal.ProjectProposalEfficiency;

public interface SmartrHeatingResult extends ProjectProposalEfficiency {
	FloatResource hardwareCost();
}
