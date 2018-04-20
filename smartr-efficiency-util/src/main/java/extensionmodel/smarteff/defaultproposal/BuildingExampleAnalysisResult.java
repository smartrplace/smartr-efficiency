package extensionmodel.smarteff.defaultproposal;

import org.ogema.core.model.simple.FloatResource;
import org.smartrplace.extenservice.proposal.ProjectProposal;

public interface BuildingExampleAnalysisResult extends ProjectProposal {
	FloatResource consultantHours();
}
