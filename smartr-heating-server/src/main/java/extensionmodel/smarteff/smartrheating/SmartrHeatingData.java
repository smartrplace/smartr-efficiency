package extensionmodel.smarteff.smartrheating;

import org.ogema.core.model.simple.BooleanResource;
import org.ogema.core.model.simple.IntegerResource;
import org.smartrplace.efficiency.api.base.SmartEffResource;

public interface SmartrHeatingData extends SmartEffResource {
	/** Standard type of thermostats for entire building (exceptions might be modeled separatly
	 * for special rooms). See {@link HeatRadiatorType#typeOfThermostat() for values}. 
	 */
	//IntegerResource typeOfThermostats();
	
	/**TODO: This value already exists as {@link BuildingData#roomNum()}*/
	//IntegerResource numberOfRooms();
	
	/** If no information on radiator types is provided the total number of radiators can be given here.
	 * Note that typically the number of raditors is the number of radiators that can technically be controlled
	 * individually (which is equal to the number of valves).*/
	//IntegerResource numberOfRadiators();
	
	/** 1: None<br>
	 *  2: Relevant<br>
	 *  3: Important Issue
	 */
	IntegerResource problemsWithFungusOrMould();
	
	BooleanResource gasMeterHasPulseOutput();
}
