package extensionmodel.smarteff.api.common;

import org.ogema.core.model.units.LengthResource;
import org.smartrplace.efficiency.api.base.SmartEffResource;

public interface Window extends SmartEffResource {
	
	/** Type of window or door */
	WindowType type();

	/** Width of the window */
	LengthResource width();
}
