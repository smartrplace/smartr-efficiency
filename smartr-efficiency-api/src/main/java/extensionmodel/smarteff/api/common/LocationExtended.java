package extensionmodel.smarteff.api.common;

import org.ogema.core.model.simple.IntegerResource;
import org.ogema.core.model.simple.StringResource;
import org.ogema.model.locations.Location;

public interface LocationExtended extends Location{
	/**Based on international phone prefix, e.g.
	 * 31: Netherlands
	 * 32: Belgium
	 * 33: France
	 * 41: Switzerland
	 * 43: Austria
	 * 44: UK
	 * 45: Denmark
	 * 46: Sweden
	 * 48: Poland
	 * 49: Germany
	 * 352: Luxemburg
	 * 420: Czechia
	 * */
	IntegerResource country();
	
	StringResource state();
	
	StringResource postalCode();
	
	StringResource city();
	
	StringResource street();
	
	StringResource number();
}
