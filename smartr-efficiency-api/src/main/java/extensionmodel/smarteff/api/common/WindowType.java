package extensionmodel.smarteff.api.common;

import org.ogema.core.model.simple.FloatResource;
import org.ogema.core.model.simple.IntegerResource;
import org.ogema.core.model.simple.StringResource;
import org.ogema.core.model.units.LengthResource;
import org.smartrplace.efficiency.api.base.SmartEffResource;

public interface WindowType extends SmartEffResource {
	
	/* Window Type IDs */
	static final int WINDOW_NORMAL = 0;
	static final int WINDOW_DOOR = 1;
	static final int WINDOW_DEFECTIVE = 2;
	/** Type ID of window */
	IntegerResource id();
	
	/** Number of windows of this type in the building */
	IntegerResource count();
	
	/** U-Value of this window type */
	FloatResource uValue();
	
	/** Height of this window type */
	LengthResource height();
	
	// TODO Tightness
}
