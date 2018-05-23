package extensionmodel.smarteff.driver.basic;

import org.ogema.core.model.array.StringArrayResource;
import org.ogema.core.model.simple.StringResource;
import org.smartrplace.efficiency.api.base.SmartEffResource;

public interface BasicGaRoDataProviderConfig extends SmartEffResource {
	StringResource dataProviderId();
	StringArrayResource gwIdsAllowed();
	StringArrayResource buildingLocations();
}
