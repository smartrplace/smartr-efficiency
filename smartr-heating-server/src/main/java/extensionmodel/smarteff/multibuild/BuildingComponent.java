package extensionmodel.smarteff.multibuild;

import org.ogema.core.model.simple.FloatResource;
import org.ogema.core.model.simple.StringResource;
import org.smartrplace.efficiency.api.base.SmartEffResource;

/**
 * Hardware components for installation in building IoT projects
 */
public interface BuildingComponent extends SmartEffResource {
	@Override
	StringResource name();
	/** Initial cost (EUR)*/
	FloatResource cost();
	FloatResource yearlyCost();
	CommunicationBusType type();
	StringResource link();
}
