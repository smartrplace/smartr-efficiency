package org.smartrplace.app.monbase.power;

import java.util.Collection;

import org.ogema.devicefinder.api.DatapointInfo.UtilityType;

import de.iwes.timeseries.eval.garo.api.base.GaRoDataType;

public interface ConsumptionEvalTableI<C extends ConsumptionEvalTableLineI> {

	public Collection<C> getAllObjectsInTable();
	
	Collection<UtilityType> getUtilityType();
	Collection<GaRoDataType> getDataTypes();
	
	default boolean isPowerTable() {return false;}
	
	default int hasSubPhaseNum() {return 0;}
	String[] getPhaseLabels();
}
