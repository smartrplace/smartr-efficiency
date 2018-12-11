package extensionmodel.smarteff.basic.evals;

import org.ogema.core.model.simple.IntegerResource;
import org.ogema.core.model.units.LuminousFluxResource;
import org.ogema.core.model.units.PowerResource;
import org.smartrplace.efficiency.api.base.SmartEffResource;
import org.smartrplace.extensionservice.SmartEffTimeSeries;

public interface RoomLightingData extends SmartEffResource {
	/** Number of lights in the room
	 */
	IntegerResource lightNum();
	
	/** Total eletrcical power of lighting in the room*/
	PowerResource installedLightPower();
	
	/** Total maximum luminous flux from lighting installed. This should take into account inherent
	 * losses by fixed installed covers of lights etc.*/
	LuminousFluxResource installedLuminousFlux();
	
	/** 0: Lights off
	 * 1: Lights on and room is used
	 * 2: Lights on and room is not used (currently)
	 * 3: Lights on and room has obviously not been used for more than an hour
	 */
	SmartEffTimeSeries lightingSituation();
}
