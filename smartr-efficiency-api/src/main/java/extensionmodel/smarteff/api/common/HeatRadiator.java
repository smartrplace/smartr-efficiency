package extensionmodel.smarteff.api.common;

import org.ogema.core.model.simple.BooleanResource;
import org.ogema.core.model.units.LengthResource;
import org.ogema.model.devices.buildingtechnology.Radiator;
import org.smartrplace.efficiency.api.base.SmartEffResource;
import org.smartrplace.extensionservice.SmartEffTimeSeries;

/** Note that each radiator type should have a name resource used as an individual ID*/
public interface HeatRadiator extends SmartEffResource {
	/** Usually this should be a reference to a type modeled for a building or 
	 * provided as global data
	 */
	HeatRadiatorType type();
	
	/** Length of radiators, usually height and depth should be given by the type*/
	LengthResource radiatorLength();
	
	/** Reference to OGEMA device model used for actual control operations*/
	Radiator controlDevice();
	
	BooleanResource hasHeatCostAllocator();
	SmartEffTimeSeries heatCostAllocatorReadings();
}
