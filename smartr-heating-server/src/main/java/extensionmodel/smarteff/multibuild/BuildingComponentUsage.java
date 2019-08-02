package extensionmodel.smarteff.multibuild;

import org.ogema.core.model.simple.IntegerResource;
import org.smartrplace.efficiency.api.base.SmartEffResource;

/**
 * Selection configuration for hardware components for installation in 
 * a specific building IoT project
 */
public interface BuildingComponentUsage extends SmartEffResource {
	BuildingComponent paramType();
	/** Number of the selected items to be used per building in the project*/
	IntegerResource number();
}
