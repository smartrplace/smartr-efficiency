package extensionmodel.smarteff.api.common;

import org.ogema.core.model.ResourceList;
import org.ogema.core.model.ModelModifiers.NonPersistent;
import org.ogema.core.model.simple.BooleanResource;
import org.ogema.core.model.simple.FloatResource;
import org.ogema.core.model.simple.IntegerResource;
import org.ogema.core.model.units.AreaResource;
import org.ogema.core.model.units.EnergyResource;
import org.ogema.core.model.units.LengthResource;
import org.smartrplace.efficiency.api.base.SmartEffResource;
import org.smartrplace.extensionservice.SmartEffTimeSeries;
import org.smartrplace.extensionservice.SpartEffModelModifiers.DataType;

public interface BuildingData extends SmartEffResource {
	/** 1: single family home (SFH)<br>
	 *  2: multi family home< (MFH)br>
	 *  3: apartment<br>
	 *  4: office building<br>
	 *  5: other commercial building<br>
	 *  10: school<br>
	 *  11: gym<br>
	 *  12: lecture hall, conference center, theater etc.<br>
	 *  20: other
	 */
	IntegerResource typeOfBuilding();
	
	/** 1: owner<br>
	 *  2: tenant<br>
	 *  3: property manager<br>
	 */
	IntegerResource typeOfUser();
	
	/** Only shown when unit type is not SFH*/
	IntegerResource numberOfUnitsInBuilding();
	
	/**Number of rooms in building or unit. If detailed room data is specified in {@link #buildingUnit()} then
	 * this value should be larger or equal to the number of rooms specified in the list. If not
	 * for all rooms is given detailed data the total number shall be indicated here. If this value
	 * is zero or negative this indicates that the number of rooms shall be obtained from the
	 * details list.*/
	IntegerResource roomNum();
	
	/**Usually only country and postal code is used*/
	LocationExtended address();
	
	FloatResource heatedLivingSpace();
	
	IntegerResource yearOfConstruction();
	
	/** 1: Not reconstructed
	 *  2: Partially reconstructed
	 *  3: Fully reconstructed to the state-of-the-art of the year of reconstruction
	 */
	IntegerResource reconstructionStatus();
	IntegerResource yearOfReconstruction();
	
	/** Only to be provided if different from current building heat source
	 * 1: L gas
	 * 2: H gas
	 * 10: oil
	 * 11: charcoal
	 * 12: lignite
	 * 13: wood pellets
	 * 14: dry wood
	 * 20: district heating
	 * 21: building-internal heat meter
	 * 30: heat pump
	 * 31: night storage heating
	 * 32: direct electric heating
	 */
	IntegerResource heatSource();
	BooleanResource coGeneration();

	ResourceList<HeatCostBillingInfo> heatCostBillingInfo();
	
	/** Information or rooms or sub-units like flats (usually rooms)*/
	ResourceList<BuildingUnit> buildingUnit();
	//TODO: add further elements
	
	/** Electricity consumption metered for the entire building (power in W)*/
	SmartEffTimeSeries electricityMainProfile();
	
	/** Electricity meter count value at the time of value recording (Energy in kWh)*/
	public SmartEffTimeSeries electricityMeterCountValue();
	
	/** Radiator types in the building*/
	public ResourceList<HeatRadiatorType> heatRadiatorType();
	
	/** Window types in the building*/
	public ResourceList<WindowType> windowType();

	/** Usually one value per day should be given here with hours where
	 * heating is generally required in the building*/
	SmartEffTimeSeries estimatedHoursWithPresence();
	
	/** Heated area not in rooms */
	AreaResource heatedAreaNotInRooms();
	
}
