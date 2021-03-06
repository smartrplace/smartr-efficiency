package extensionmodel.smarteff.smartrheating;

import org.ogema.core.model.simple.FloatResource;
import org.ogema.core.model.simple.IntegerResource;
import org.smartrplace.extensionservice.proposal.ProjectProposalEfficiency;

public interface SmartrHeatingResult extends ProjectProposalEfficiency {
	/** Warm water energy (losses during heating season in heated areas are included) (kWh) */
	FloatResource wwEnergyPreRenovation();
	/** Heating energy (pre-renovation) (kWh) */
	FloatResource heatingEnergyPreRenovation();

	//Radiators
	IntegerResource thermostatNum();
	IntegerResource roomNumInBuilding();
	IntegerResource roomNumWithThermostats();

	//Windows
	IntegerResource windowNum();
	IntegerResource windowSensorNum();
	
	//Savings
	FloatResource savingsAbsolute();
	FloatResource savingsRelative();
	FloatResource hoursWithoutLowering();
	FloatResource hoursLoweringEffectiveBefore();
}
