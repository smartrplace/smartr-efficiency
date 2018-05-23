package extensionmodel.smarteff.basic.evals;

import org.ogema.core.model.simple.TimeResource;
import org.smartrplace.efficiency.api.base.SmartEffResource;

public interface BuildingEvalData extends SmartEffResource {
	/** Absence times below this time will be covered with presence
	 */
	TimeResource minimumAbsenceTime();
}
