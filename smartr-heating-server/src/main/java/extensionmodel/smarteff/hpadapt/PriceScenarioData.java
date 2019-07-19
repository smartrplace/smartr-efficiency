package extensionmodel.smarteff.hpadapt;

import org.ogema.core.model.simple.FloatResource;
import org.smartrplace.efficiency.api.base.SmartEffResource;

/**
 * Data, e.g. energy costs, for a certain price scenario.
 */
public interface PriceScenarioData extends SmartEffResource {

	/* ENERGY COSTS */

	/** Price of standard electricity (EUR/kWh) */
	FloatResource electrictiyPricePerkWh();

	/** Price for heat pump electricity (EUR/kWh) */
	FloatResource electrictiyPriceHeatPerkWh();

	/** Price for natural gas (EUR/kWh) */
	FloatResource gasPricePerkWh();
	
	/** Base price for standard electricity (EUR)*/
	FloatResource electricityPriceBase();
	
	/** Base price for heat pump electricity (EUR)*/
	FloatResource electricityPriceHeatBase();

}
