package extensionmodel.smarteff.hpadapt;

import org.ogema.core.model.simple.FloatResource;
import org.smartrplace.efficiency.api.base.SmartEffResource;

/**
 * Data, e.g. energy costs, for a certain price scenario.
 */
public interface PriceScenarioData extends SmartEffResource {

	/* ENERGY COSTS */

	/** Price of standard electricity energy (EUR/kWh) */
	FloatResource electrictiyPricePerkWh();

	/** Price for heat pump electricity energy (EUR/kWh) */
	FloatResource electrictiyPriceHeatPerkWh();

	/** Price for natural gas energy (EUR/kWh) */
	FloatResource gasPricePerkWh();

	/** Base price for natural gas per year (EUR) */
	FloatResource gasPriceBase();
	
	/** Base price for standard electricity per year (EUR)*/
	FloatResource electricityPriceBase();
	
	/** Base price for heat pump electricity per year (EUR)*/
	FloatResource electricityPriceHeatBase();

	/** Price for heating oil energy (EUR/kWh) */
	FloatResource oilPricePerkWh();

	/** Base price for heating oil supply per year (EUR) */
	FloatResource oilPriceBase();
	
	/** Price for wood pellets energy (EUR/kWh) */
	FloatResource woodPelletPricePerkWh();

	/** Base price for wood pellets supply per year (EUR) */
	FloatResource woodPelletPriceBase();	/** Price for heating oil (EUR/kWh) */

	/** Price for district heating energy (EUR/kWh) */
	FloatResource districtHeatingPerkWh();

	/** Base price for district heating per year (EUR) */
	FloatResource districtHeatingBase();
}
