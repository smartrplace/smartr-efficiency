package extensionmodel.smarteff.api.common;

import org.ogema.core.model.simple.FloatResource;
import org.ogema.core.model.simple.IntegerResource;
import org.ogema.core.model.units.LengthResource;
import org.smartrplace.efficiency.api.base.SmartEffResource;

public interface WindowType extends SmartEffResource {
	
	/* Window Type IDs */
	static final int WINDOW_NORMAL = 0;
	static final int WINDOW_DOOR = 1;
	static final int WINDOW_DEFECTIVE = 2;
	
	/** Type ID of window
	 * Note: The ID is only required in the spreadsheet as no direct references are possible there	*/
	//IntegerResource id();
	
	/** Number of windows of this type in the building excluding windows modeled in room (see
	 * also {@link HeatRadiatorType#numberOfRadiators()} */
	IntegerResource count();
	
	/** Share of windows of the type that require a window sensor for optimized operation
	 * TODO: This value may be shifted in the future as this is not a general physical value*/
	FloatResource sensorInstallationShare();
	
	/** U-Value of this window type */
	FloatResource uValue();
	
	/** Height of this window type */
	LengthResource height();
	
	// TODO Tightness
}
