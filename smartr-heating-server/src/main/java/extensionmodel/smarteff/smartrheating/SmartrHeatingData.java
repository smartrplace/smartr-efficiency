package extensionmodel.smarteff.smartrheating;

import org.ogema.core.model.ResourceList;
import org.ogema.core.model.simple.BooleanResource;
import org.ogema.core.model.simple.IntegerResource;
import org.smartrplace.efficiency.api.base.SmartEffResource;

import extensionmodel.smarteff.api.common.BuildingData;

public interface SmartrHeatingData extends SmartEffResource {
	/** 1: Standard on radiators<br>
	 *  2: Control knob connected via pressure cable<br>
	 *  3: room control device<br>
	 *  4: building automation system
	 */
	IntegerResource typeOfThermostats();
	
	/**TODO: This value already exists as {@link BuildingData#roomNum()}*/
	IntegerResource numberOfRooms();
	
	IntegerResource numberOfRadiators();
	
	/** 1: None<br>
	 *  2: Relevant<br>
	 *  3: Important Issue
	 */
	IntegerResource problemsWithFungusOrMould();
	
	BooleanResource gasMeterHasPulseOutput();
	
	ResourceList<SHeatRadiatorType> sHeatRadiatorType();
	
	//TODO: add further elements
}
