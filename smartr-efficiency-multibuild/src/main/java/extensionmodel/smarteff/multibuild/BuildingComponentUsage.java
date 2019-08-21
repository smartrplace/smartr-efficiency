package extensionmodel.smarteff.multibuild;

import org.ogema.core.model.simple.FloatResource;
import org.ogema.core.model.simple.IntegerResource;
import org.ogema.core.model.simple.StringResource;
import org.smartrplace.efficiency.api.base.SmartEffResource;

/**
 * Selection configuration for hardware components for installation in 
 * a specific building IoT project
 */
public interface BuildingComponentUsage extends SmartEffResource {
	BuildingComponent paramType();
	/** Number of the selected items to be used per building in the project*/
	IntegerResource number();
	
	/** optional*/
	StringResource comment();
	
	/** Cost for configuration, installation per item, project-specific (EUR)*/
	FloatResource additionalCostPerItem();
	
	FloatResource additionalYearlyCost();
	
	/**If active this text will be used in the offer table
	 * instead of the standard text of the component 
	 */
	StringResource alternativeOfferText();
}
