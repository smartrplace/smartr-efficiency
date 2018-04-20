package extensionmodel.smarteff.defaultproposal;

import org.ogema.core.model.simple.FloatResource;
import org.smartrplace.extensionservice.proposal.ProjectProposal;

public interface BuildingExampleAnalysisResult extends ProjectProposal {
	FloatResource consultantHours();
}
