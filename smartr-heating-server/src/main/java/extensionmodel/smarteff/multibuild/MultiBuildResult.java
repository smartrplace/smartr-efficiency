package extensionmodel.smarteff.multibuild;

import org.ogema.core.model.simple.FloatResource;
import org.smartrplace.efficiency.api.base.SmartEffResource;
import org.smartrplace.extensionservice.proposal.ProjectProposal;
/**
 * Results of MultiBuilding
 * See {@link (https://github.com/smartrplace/smartr-efficiency/blob/master/MultiBuilding.md#results}
 */
public interface MultiBuildResult extends ProjectProposal, SmartEffResource {
	FloatResource costPerBuilding();
}
