package extensionmodel.smarteff.multibuild;

import org.ogema.core.model.ResourceList;
import org.ogema.core.model.simple.FloatResource;
import org.smartrplace.efficiency.api.base.SmartEffResource;
import org.smartrplace.extensionservice.proposal.ProjectProposal;
/**
 * Results of MultiBuilding
 * See {@link (https://github.com/smartrplace/smartr-efficiency/blob/master/MultiBuilding.md#results}
 */
public interface MultiBuildResult extends ProjectProposal, SmartEffResource {
	FloatResource costPerBuilding();
	
	/** Initial investment list*/
	ResourceList<OfferLineInit> offerLineInit();
	
	/** Monthly or yearly cost*/
	ResourceList<OfferLineRecurrent> offerLineRecurrent();
	
	/** Hardware cost excluding VAT including delivery cost and custom charges*/
	FloatResource hardwareCost();
	
	/** Other non-personal cost such as subcontractors*/
	FloatResource otherNonPersonalCost();
}
