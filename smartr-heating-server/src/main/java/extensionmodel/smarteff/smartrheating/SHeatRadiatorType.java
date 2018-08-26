package extensionmodel.smarteff.smartrheating;

import org.ogema.core.model.array.StringArrayResource;
import org.ogema.core.model.simple.IntegerResource;
import org.ogema.core.model.simple.StringResource;
import org.smartrplace.efficiency.api.base.SmartEffResource;

public interface SHeatRadiatorType extends SmartEffResource {
	IntegerResource numberOfRadiators();
	StringArrayResource radiatorPictureURLs();
	StringResource radiatorDescription();
}