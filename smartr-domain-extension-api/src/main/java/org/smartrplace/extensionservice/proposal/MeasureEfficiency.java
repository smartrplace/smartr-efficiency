package org.smartrplace.extensionservice.proposal;

import org.ogema.core.model.simple.FloatResource;
import org.ogema.core.model.simple.StringResource;

/** A Measure is a special CalculatedData that provides some information on a
 * potential efficiency work. It is focused on technical aspects and does NOT include
 * price information for the measure itself, but may include information on energy cost	 
 */
public interface MeasureEfficiency extends CalculatedData {
	/** Savings in yearly operating costs compared to a reference case that does not change
	 * the building energy supply situation.
	 */
	FloatResource yearlySavings();
	
	/** CO2 emission savings in kg/year*/
	FloatResource yearlyCO2savings();
	
	/** A description of the project benefit. If inherited models include modeling of benefits,
	 * e.g. monetary and/or environmental benefits an additional description may not be necessary here
	 */
	StringResource benefitDescription();
}
