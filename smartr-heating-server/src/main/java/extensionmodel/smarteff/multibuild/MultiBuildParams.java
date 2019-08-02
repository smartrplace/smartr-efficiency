package extensionmodel.smarteff.multibuild;

import org.ogema.core.model.ResourceList;
import org.ogema.core.model.simple.FloatResource;
import org.smartrplace.efficiency.api.base.SmartEffResource;

/**
 * Parameters for MultiBuilding
 * See {@link (https://github.com/smartrplace/smartr-efficiency/blob/master/MultiBuilding.md#parameters}
 */
public interface MultiBuildParams extends SmartEffResource {
	FloatResource costSPBox();
	FloatResource costProjectBase();
	FloatResource operationalCost();

	@Deprecated
	FloatResource insideTempCost();
	@Deprecated
	FloatResource outsideTempCost();
	@Deprecated
	FloatResource waterTempCost();
	@Deprecated
	FloatResource powerSwitchCost();
	@Deprecated
	FloatResource powerMeterCost();

	ResourceList<BuildingComponent> buildingComponent();
	ResourceList<CommunicationBusType> communicationBusType();
}
