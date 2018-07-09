package extensionmodel.smarteff.smartrheating;

import org.ogema.core.model.simple.FloatResource;
import org.smartrplace.efficiency.api.base.SmartEffResource;

/** Parameters that are not visible publicly*/
public interface SmartrHeatingInternalParams extends SmartEffResource {
	FloatResource baseCost();
	FloatResource costPerRoom();
	FloatResource costPerThermostat();
}
