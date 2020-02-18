package org.smartrplace.app.monbase.config;

import org.ogema.core.model.Resource;
import org.ogema.core.model.units.EnergyResource;
import org.ogema.model.prototypes.Data;

public interface EnergyEvalIntervalMeterData extends Data {
	/** Meter counter for which the interval data is stored here*/
	Resource conn();
	
	EnergyResource startCounterValue();
	/** For completed intervals, otherwise must not be active*/
	EnergyResource energyConsumedInInterval();	
}
