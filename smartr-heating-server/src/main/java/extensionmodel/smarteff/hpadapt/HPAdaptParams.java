package extensionmodel.smarteff.hpadapt;

import org.ogema.core.model.simple.FloatResource;
import org.smartrplace.efficiency.api.base.SmartEffResource;

public interface HPAdaptParams extends SmartEffResource {
	/** Price for CO2-neutral gas per kWh*/
	FloatResource gasPriceCO2neutralPerkWh();
	FloatResource gasPrice100EEPerkWh();
	FloatResource electrictiyPriceCO2neutralPerkWh();
	FloatResource electrictiyPrice100EEPerkWh();

	FloatResource electrictiyPriceHeatBase();
	FloatResource electrictiyPriceHeatPerkWh();
	FloatResource electrictiyPriceHeatCO2neutralPerkWh();
	FloatResource electrictiyPriceHeat100EEPerkWh();

	//TODO: Further yellow lines from table "Parameter"
}
