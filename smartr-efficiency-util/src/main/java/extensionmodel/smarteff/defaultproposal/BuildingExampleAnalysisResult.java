package extensionmodel.smarteff.defaultproposal;

import org.ogema.core.model.simple.FloatResource;
import org.smartrplace.extensionservice.proposal.ProjectProposalEfficiency;

public interface BuildingExampleAnalysisResult extends ProjectProposalEfficiency {
	FloatResource consultantHours();
}
