package extensionmodel.smarteff.api.base;

import org.ogema.core.model.ResourceList;
import org.smartrplace.efficiency.api.base.SmartEffResource;

import extensionmodel.smarteff.api.common.BuildingData;

/**Top-level data specification. For models defining SmartEffUserData or ExtensionUserData as
 * parent that are not specified here respective decorators with names defined in
 * CapabilityHelper.getSingleResourceName or getMulti*ResourceName shall be attached.
 * For resources with a multi-cardinality a default element can be specified (usually only a
 * default element for generalData). The name starts with "default".
 */
public interface SmartEffTopLevelData extends SmartEffResource {
	SmartEffPriceData smartEffPriceData();
	
	ResourceList<BuildingData> buildingData();
	BuildingData defaultBuildingData();
}
