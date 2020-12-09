package org.smartrplace.app.monbase.power;

import java.util.Collection;

import org.ogema.devicefinder.api.DatapointInfo.UtilityType;

import de.iwes.timeseries.eval.garo.api.base.GaRoDataType;

public interface ConsumptionEvalTableI<C extends ConsumptionEvalTableLineI> {

	public Collection<C> getAllObjectsInTable();
	
	Collection<UtilityType> getUtilityType();
	Collection<GaRoDataType> getDataTypes();
	
	default boolean isPowerTable() {return false;}
	
	/** Number of additional values apart from the line label, the line main value and the cost column*/
	default int hasSubPhaseNum() {return 0;}
	
	/** Note that the length of getPhaseLabels should be equal to the result of {@link #hasSubPhaseNum()}
	 */
	String[] getPhaseLabels();
}
