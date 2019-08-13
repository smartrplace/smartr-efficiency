package extensionmodel.smarteff.smartrheating.intern;

import extensionmodel.smarteff.multibuild.MultiBuildResult;
import extensionmodel.smarteff.smartrheating.SmartrHeatingResult;

public interface SmartrHeatingResultPricing extends SmartrHeatingResult {
	
	/** Sub result*/
	MultiBuildResult multiBuildResult();

}
