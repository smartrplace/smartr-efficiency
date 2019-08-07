package extensionmodel.smarteff.hpadapt;

import org.ogema.core.model.simple.StringResource;
import org.ogema.core.model.units.PercentageResource;
import org.ogema.core.model.units.TemperatureResource;
import org.smartrplace.efficiency.api.base.SmartEffResource;
import org.smartrplace.extensionservice.SmartEff2DMap;
import org.smartrplace.extensionservice.SmartEffTimeSeries;

/**
 * Parameters for HPAdapt.
 * Yellow cells in "Parameter" spread sheet.
 * @author jruckel
 *
 */
public interface HPAdaptParams extends SmartEffResource {

	/* ENERGY COSTS */
	
	PriceScenarioData pricesConventional();
	PriceScenarioData pricesCO2neutral();
	PriceScenarioData prices100EE();
	
	/** Price of CO2-neutral electricity (EUR/kWh) */
	//FloatResource electrictiyPriceCO2neutralPerkWh();
	/** Price of 100EE neutral electricity (EUR/kWh) */
	//FloatResource electrictiyPrice100EEPerkWh();

	/** Base price for heat pump electricity */
	//FloatResource electrictiyPriceHeatBase();
	/** Price for heat pump electricity (EUR/kWh) */
	//FloatResource electrictiyPriceHeatPerkWh();
	/** Price for CO2-neutral heat pump electricity (EUR/kWh) */
	//FloatResource electrictiyPriceHeatCO2neutralPerkWh();
	/** Price for 100EE heat pump electricity (EUR/kWh) */
	//FloatResource electrictiyPriceHeat100EEPerkWh();

	/** Price for CO2-neutral gas (EUR/kWh) */
	//FloatResource gasPriceCO2neutralPerkWh();
	/** Price for 100EE gas (EUR/kWh) */
	//FloatResource gasPrice100EEPerkWh();

	/* BOILER POWER */
	
	/** Boiler Power Reduction switching from LT→CD */
	PercentageResource boilerPowerReductionLTtoCD();


	/* WARM DRINKING WATER ESTIMATES */

	/** Warm water supply temperature (°C) */
	TemperatureResource wwSupplyTemp();
	

	/* OTHER */

	/** Historical Temperature Data to be imported via CSV. Daily mean temperatures over one year. */
	SmartEffTimeSeries temperatureHistory();
	
	/**
	 * COP characteristics of heat pump. The outside temperature is used as primary key,
	 * the supply temperature is used as secondary key, the values are COP values.
	 */
	SmartEff2DMap copCharacteristics();
	
	StringResource internalParamProvider();
}
