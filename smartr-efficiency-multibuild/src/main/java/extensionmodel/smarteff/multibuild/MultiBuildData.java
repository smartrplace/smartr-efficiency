package extensionmodel.smarteff.multibuild;

import org.ogema.core.model.ResourceList;
import org.ogema.core.model.simple.FloatResource;
import org.ogema.core.model.simple.IntegerResource;
import org.smartrplace.efficiency.api.base.SmartEffResource;

/**
 * Data for MultiBuilding
 * See {@link (https://github.com/smartrplace/smartr-efficiency/blob/master/MultiBuilding.md#data}
*/
public interface MultiBuildData extends SmartEffResource {

	IntegerResource buildingNum();
	FloatResource operationalCost();

	@Deprecated
	IntegerResource insideTempNum();
	@Deprecated
	IntegerResource outsideTempNum();
	@Deprecated
	IntegerResource waterTempNum();
	@Deprecated
	IntegerResource powerSwitchNum();
	@Deprecated
	IntegerResource powerMeterNum();

	ResourceList<BuildingComponentUsage> buildingComponentUsage();	
}
