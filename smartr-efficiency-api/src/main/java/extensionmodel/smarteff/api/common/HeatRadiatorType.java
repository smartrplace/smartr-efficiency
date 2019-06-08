package extensionmodel.smarteff.api.common;

import org.ogema.core.model.array.StringArrayResource;
import org.ogema.core.model.simple.FloatResource;
import org.ogema.core.model.simple.IntegerResource;
import org.ogema.core.model.simple.StringResource;
import org.ogema.core.model.units.LengthResource;
import org.smartrplace.efficiency.api.base.SmartEffResource;

/** Note that each radiator type should have a name resource used as an individual ID*/
public interface HeatRadiatorType extends SmartEffResource {
	/** 1: Standard on radiators<br>
	 *  2: Control knob connected via pressure cable<br>
	 *  3: room control device<br>
	 *  4: building automation system
	 */
	IntegerResource typeOfThermostat();
	/** Usually this model is used for radiators in the room, not for underfloor heating.
	 * But the model can also be used for this, values:
	 * 0: hot-water powered radiator for walls (default, used if sub resource does not exist)
	 * 1: under-floor heating (powered by hot water)
	 * 2: ceiling heating (powered by hot water)
	 * 10: electricity-powered radiator. In this case several values given
	 * 		here might need a re-definition. This option needs further elobaration
	 * 		in the future.*/
	IntegerResource radiatorType();
	
	/** If the single rooms of a building are not represented in the database or
	 * the radiators (or their types) are not modeled per room the total number of radiators
	 * of a type can be stored in this value for the entire building. If only some radiators
	 * of the type are represented in rooms this number should only give the radiators not
	 * modeled in rooms.
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
