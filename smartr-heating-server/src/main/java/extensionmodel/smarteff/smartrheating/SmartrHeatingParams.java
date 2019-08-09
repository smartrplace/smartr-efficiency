package extensionmodel.smarteff.smartrheating;

import org.ogema.core.model.simple.FloatResource;
import org.ogema.core.model.simple.StringResource;
import org.ogema.core.model.units.TemperatureResource;
import org.smartrplace.efficiency.api.base.SmartEffResource;

public interface SmartrHeatingParams extends SmartEffResource {
	/** Supply temperature of drinking water into the building*/
	TemperatureResource wwSupplyTemp();
	
	/**Example value for global parameter that can be seen and replaced with own value by customer
	 * even for a vendor-specific evaluation*/
	FloatResource costOfCustomerPerRoom();
	
	/** CO2 emissions of burning natural gas (kg/kWh)*/
	FloatResource co2factorGas();	
	
	StringResource internalParamProvider();
}
