package extensionmodel.smarteff.smartrheating;

import org.ogema.core.model.simple.FloatResource;
import org.smartrplace.efficiency.api.base.SmartEffResource;

public interface SmartrHeatingParams extends SmartEffResource {
	/**Example value for global parameter that can be seen and replaced with own value by customer
	 * even for a vendor-specific evaluation*/
	FloatResource costOfCustomerPerRoom();
}
