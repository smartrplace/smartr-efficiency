package org.smartrplace.app.monbase.power;

import java.util.ArrayList;
import java.util.List;

import org.ogema.devicefinder.api.ConsumptionInfo.UtilityType;
import org.smartrplace.app.monbase.MonitoringController;

public class ConsumptionEvalAdmin {
	public static enum SumType {
		STD,
		INIT,
		SUM_LINE
	}
	
	/*public static class ConsumptionEvalConfiguration {
		/** Sub circuits can be phases or other subcircuits that occur regularly, e.g. for every meter
		 * or every part of the building. If a building is diveded into two main heating circuits for each
		 * floor a separate metering for both sub circuits could be done.<br>
		 * A representation which meters add up to another sum meter is represented in... (TODO)<br>
		 * If this is null or empty no subPhases are shown.
		 */
		/*List<String> subPhases;
		
		List<ConsumptionEvalTableLineI> evaluationLines;
	}*/
	
	protected final MonitoringController controller;
	
	public ConsumptionEvalAdmin(MonitoringController controller) {
		super();
		this.controller = controller;
	}

	protected final List<ConsumptionEvalTableBase<?>> evaluations = new ArrayList<>();
	
	public void registerEvaluationTable(ConsumptionEvalTableBase<?> table) {
		evaluations.add(table);
	}
	
	/** Get all registered tables of a type
	 * 
	 * @param type if null tables for all types are returned
	 * @return
	 */
	public List<ConsumptionEvalTableBase<?>> getTables(UtilityType type) {
		List<ConsumptionEvalTableBase<?>> result = new ArrayList<>();
		for(ConsumptionEvalTableBase<?> table: evaluations) {
			if(type == null || type == table.getUtilityType())
				result.add(table);
		}
		return result ;
	}
}
