package extensionmodel.smarteff.smartrheating;

import org.ogema.core.model.simple.BooleanResource;
import org.ogema.core.model.simple.FloatResource;
import org.ogema.core.model.simple.IntegerResource;
import org.ogema.core.model.units.PercentageResource;
import org.ogema.core.model.units.TemperatureResource;
import org.ogema.core.model.units.VolumeResource;
import org.smartrplace.efficiency.api.base.SmartEffResource;

public interface SmartrHeatingData extends SmartEffResource {
	
	/** Is gas consumption for drinking warm water contained in yearly energy consumption?*/
	BooleanResource wwIsContained();
	/** Known or estimated warm drinking water consumption */
	VolumeResource wwConsumption();
	/** Estimated warm water energy loss from storage, circulation at current temperature in heated areas */
	PercentageResource wwLossHeatedAreas();
	/** Warm water energy loss in unheated areas */
	PercentageResource wwLossUnheatedAreas();
	/** Warm water temperature */
	TemperatureResource wwTemp();

	/**Heating degree days per year*/
	//FloatResource heatingDegreeDaysManual();
	/** Average number of heating days per year*/
	FloatResource heatingDaysManual();
	
	/** Price for heating energy source per kWh set for building without fixed base fee (EUR/kWh)*/
	FloatResource gasPricePerkWh();
	
	/** Average usage time of building hours per week*/
	FloatResource usageTimePerWeek();
	
	/** Average number of blocks of usage time per week*/ 
	IntegerResource usageBlocksPerWeek();
	
	/** Average duration for cooling down of the building to temperature with low loss (hours)*/
	FloatResource coolingDownHours();
	
	/** Average duration for heating up of the building from temperature with low loss (hours)*/
	FloatResource heatingUpHours();
	
	/** Number of hours the building was cooled per week down before introduction of SmartrHeating*/
	FloatResource heatingReductionHoursBefore();
	
	/** Number of heating reduction blocks per week performed before introduction of SmartrHeating*/
	IntegerResource heatingReductionBlocksBefore();
	
	/** 1: None<br>
	 *  2: Relevant<br>
	 *  3: Important Issue
	 */
	IntegerResource problemsWithFungusOrMould();
	
	BooleanResource gasMeterHasPulseOutput();
}
