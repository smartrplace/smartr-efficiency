package extensionmodel.smarteff.api.common;

import org.ogema.core.model.simple.FloatResource;
import org.ogema.core.model.simple.IntegerResource;
import org.ogema.core.model.simple.TimeResource;
import org.smartrplace.efficiency.api.base.SmartEffResource;

public interface HeatCostBillingInfo extends SmartEffResource {
	/** 1: gas in m3
	 *  2: oil in l
	 *  3: solid fuel in kg
	 *  4: kWh (input, for heat pump this would be electricity)
	 *  5: kWh heat measured for heat pump
	 */
	IntegerResource unit();
	
	/** Especially for gas, potentially for oil the energy content should be given on the bill
	 * content in kWh/m3 oder kWh/l*/
	FloatResource energyContentAccordingToBill();
	
	/** Only to be provided if different from current building heat source
	 * see {@link BuildingData#heatSource()}
	 * Negative or zero values indicate that not used.
	 */
	IntegerResource heatSource();

	TimeResource beginningOfBillingPeriodDay();
	TimeResource endOfBillingPeriodDay();
	FloatResource billedConsumption();
	/** Total cost of bill including variable and fixed cost*/
	FloatResource cost();
	/** Variable cost per kWh, note that this may not be available for all bills*/
	FloatResource costPerKWhVar();
}
