package extensionmodel.smarteff.basic.evals;

import org.smartrplace.efficiency.api.base.SmartEffResource;
import org.smartrplace.extensionservice.SmartEffTimeSeries;

import extensionmodel.smarteff.api.common.BuildingUnit;

public interface BuildingControlQualityFeedback extends SmartEffResource {
	/** Overall building control and data acquisition quality feedback
	 * 1: Everything fine, no complaints
	 * 2: Feels improved compared to earlier situations
	 * 10: Minor issues (please specify as comment)
	 * 100: Immediate support action requested (please speficy as comment)
	 * See also {@link BuildingUnit#roomTemperatureQualityRating()}
	 */
	SmartEffTimeSeries overallFeedback();
}
