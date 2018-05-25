package extensionmodel.smarteff.electricity.example;

import org.ogema.core.model.simple.FloatResource;
import org.ogema.core.model.units.PowerResource;
import org.smartrplace.efficiency.api.base.SmartEffResource;

public interface ElectricityProfileEvalConfig extends SmartEffResource {
	FloatResource offpeakPrice();
	FloatResource peakPrice();
	/** Add this power to all values and create new electricity profile*/
	PowerResource addPower();
}
