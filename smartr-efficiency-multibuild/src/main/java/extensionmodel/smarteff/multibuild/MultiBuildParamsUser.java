package extensionmodel.smarteff.multibuild;

import org.ogema.core.model.ResourceList;
import org.smartrplace.efficiency.api.base.SmartEffResource;

/**
 * Parameters for MultiBuilding
 * See {@link (https://github.com/smartrplace/smartr-efficiency/blob/master/MultiBuilding.md#parameters}
 */
public interface MultiBuildParamsUser extends SmartEffResource {
	/** Components that are added for a specific project
	 * TODO: Not implemented yet*/
	ResourceList<BuildingComponent> buildingComponent();
	ResourceList<CommunicationBusType> communicationBusType();
}
