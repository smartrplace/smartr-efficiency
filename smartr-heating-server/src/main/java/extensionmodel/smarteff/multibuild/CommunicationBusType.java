package extensionmodel.smarteff.multibuild;

import org.ogema.core.model.simple.FloatResource;
import org.ogema.core.model.simple.StringResource;
import org.smartrplace.efficiency.api.base.SmartEffResource;

/**
 * Hardware components for installation in building IoT projects
 */
public interface CommunicationBusType extends SmartEffResource {
	@Override
	StringResource name();
	FloatResource cost();
	FloatResource yearlyCost();
	/** Cost for development of the driver if not yet existing*/
	FloatResource development();
	StringResource link();
}
