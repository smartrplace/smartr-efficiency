package extensionmodel.smarteff.hpadapt;

import org.ogema.core.model.simple.FloatResource;
import org.smartrplace.efficiency.api.base.SmartEffResource;

public interface HPAdaptData extends SmartEffResource {
	/** Estimated savings by economical measures improving building envelope*/
	FloatResource estimatedSavingsBuildingEnvelope();
	
	//TODO: Further yellow lines from table "Last Building"
}
