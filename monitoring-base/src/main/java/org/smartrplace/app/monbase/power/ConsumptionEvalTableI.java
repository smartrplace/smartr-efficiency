package org.smartrplace.app.monbase.power;

import java.util.Collection;

import org.ogema.devicefinder.api.ConsumptionInfo.UtilityType;

public interface ConsumptionEvalTableI<C extends ConsumptionEvalTableLineI> {

	public Collection<C> getAllObjectsInTable();
	
	UtilityType getUtilityType();
	
	default boolean isPowerTable() {return false;}
}
