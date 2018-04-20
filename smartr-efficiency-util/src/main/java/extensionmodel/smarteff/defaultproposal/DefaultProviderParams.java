package extensionmodel.smarteff.defaultproposal;

import org.ogema.core.model.simple.FloatResource;
import org.smartrplace.efficiency.api.base.SmartEffResource;

public interface DefaultProviderParams extends SmartEffResource {
	FloatResource basePriceBuildingAnalysis();
	FloatResource pricePerSQMBuildingAnalysis();
	FloatResource costOfCustomerHour();
	/** Default estimated energy demand per m2 if no more information is
	 * available
	 */
	FloatResource defaultKwhPerSQM();
}
