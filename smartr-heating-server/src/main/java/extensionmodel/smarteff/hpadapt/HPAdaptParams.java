package extensionmodel.smarteff.hpadapt;

import org.ogema.core.model.simple.FloatResource;
import org.ogema.core.model.units.PercentageResource;
import org.ogema.core.model.units.TemperatureResource;
import org.smartrplace.efficiency.api.base.SmartEffResource;
import org.smartrplace.extensionservice.SmartEffTimeSeries;

/**
 * Parameters for HPAdapt.
 * Yellow cells in "Parameter" spread sheet.
 * @author jruckel
 *
 */
public interface HPAdaptParams extends SmartEffResource {

	/* ENERGY COSTS */

	/** Price of CO2-neutral electricity (EUR/kWh) */
	FloatResource electrictiyPriceCO2neutralPerkWh();
	/** Price of 100EE neutral electricity (EUR/kWh) */
	FloatResource electrictiyPrice100EEPerkWh();

	/** Base price for heat pump electricity */
	FloatResource electrictiyPriceHeatBase();
	/** Price for heat pump electricity (EUR/kWh) */
	FloatResource electrictiyPriceHeatPerkWh();
	/** Price for CO2-neutral heat pump electricity (EUR/kWh) */
	FloatResource electrictiyPriceHeatCO2neutralPerkWh();
	/** Price for 100EE heat pump electricity (EUR/kWh) */
	FloatResource electrictiyPriceHeat100EEPerkWh();

	/** Price for CO2-neutral gas (EUR/kWh) */
	FloatResource gasPriceCO2neutralPerkWh();
	/** Price for 100EE gas (EUR/kWh) */
	FloatResource gasPrice100EEPerkWh();


	/* MISC COSTS */

	/** Condensing Boiler → Condensing Boiler (CD→CD), Base (EUR) */
	FloatResource boilerChangeCDtoCD();
	/** Low-Temperature Boiler → Condensing Boiler (LT→CD), Base (EUR) */
	FloatResource boilerChangeLTtoCD();
	/** Additional CD→CD (per kW) (EUR) */
	FloatResource boilerChangeCDtoCDAdditionalPerkW();
	/** Additional LT→CD (per kW) (EUR)*/
	FloatResource boilerChangeLTtoCDAdditionalPerkW();
	/** Additional Bivalent Heat Pump Base (EUR)*/
	FloatResource additionalBivalentHPBase();
	/** Additional Bivalent Heat Pump per kW (EUR)*/
	FloatResource additionalBivalentHPPerkW();
	

	/* BOILER POWER */
	
	/** Boiler Power Reduction switching from LT→CD */
	PercentageResource boilerPowerReductionLTtoCD();


	/* WARM DRINKING WATER ESTIMATES */

	/** Warm water supply temperature (°C) */
	TemperatureResource wwSupplyTemp();
	
	/* OTHER */
	/**
	 * Historical Temperature Data to be imported via CSV
	 */
	SmartEffTimeSeries temperatureHistory();
}
