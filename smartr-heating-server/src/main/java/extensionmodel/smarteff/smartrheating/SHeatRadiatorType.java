package extensionmodel.smarteff.smartrheating;

import org.ogema.core.model.array.StringArrayResource;
import org.ogema.core.model.simple.FloatResource;
import org.ogema.core.model.simple.IntegerResource;
import org.ogema.core.model.simple.StringResource;
import org.ogema.core.model.units.LengthResource;
import org.smartrplace.efficiency.api.base.SmartEffResource;

/** Note that each radiator type should have a name resource used as an individual ID*/
public interface SHeatRadiatorType extends SmartEffResource {
	/** If the single rooms of a building are not represented in the database or
	 * the radiators (or their types) are not modelled per room the total number of radiators
	 * of a type can be stored in this value.
	 */
	IntegerResource numberOfRadiators();
	/** TODO: No support for this in EditPageGeneric yet*/
	StringArrayResource radiatorPictureURLs();
	/** Human readable free description*/
	StringResource radiatorDescription();
	
	/** Usually each radiator type is available in different lengths. The height and width of
	 * each radiator type usually is fixed and can be given with the type. The hight would be
	 * measured for horizontal installation (as typically would be found below a window). If the
	 * radiator is installed vertical the measured dimension has to be adapted.*/
	LengthResource radiatorHight();
	/**This is usually the smallest of the three dimensions of a radiator*/
	LengthResource radiatorDepth();
	
	/** Nominal temperature difference at which the power of the type is measured*/
	FloatResource nominalTemperatureDifference();
	/** As an estimation thermal power of the radiator at nominal temperature assumed that
	 * the power is proportional to the length of the actual radiator (which requires a
	 * fixed hight and depth for the type). 
	 * Measured in W/m.
	 */
	FloatResource powerPerLength();
}
