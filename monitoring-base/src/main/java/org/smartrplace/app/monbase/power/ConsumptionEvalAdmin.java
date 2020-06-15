package org.smartrplace.app.monbase.power;

import java.util.ArrayList;
import java.util.List;

import org.ogema.devicefinder.api.Datapoint;
import org.ogema.devicefinder.api.DatapointGroup;
import org.ogema.devicefinder.api.DatapointInfo;
import org.ogema.devicefinder.api.DatapointInfo.UtilityType;
import org.smartrplace.app.monbase.MonitoringController;
import org.smartrplace.app.monbase.power.ConsumptionEvalTableLineI.EnergyEvalObjI;

public class ConsumptionEvalAdmin {
	public static final long UPDATE_MIN_INTERVAL = 10000;
	
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
		update(table, true);
	}
	
	long lastUpdate = -1;
	public void update(ConsumptionEvalTableBase<?> table, boolean force) {
		long now = controller.appMan.getFrameworkTime();
		if((!force) && (lastUpdate - now < UPDATE_MIN_INTERVAL))
			return;
		lastUpdate = now;
		for(ConsumptionEvalTableLineI line: table.getObjectsInTable(null)) {
			
			Datapoint dps = line.getDailyConsumptionValues();
			registerPlotDps(dps, table, line, "_Daily");
			Datapoint dps3 = line.getHourlyConsumptionValues();
			registerPlotDps(dps3, table, line, "_Hourly");
			Datapoint dps4 = line.getAvergageValues();
			registerPlotDps(dps4, table, line, "_Av");

			EnergyEvalObjI conn = line.getEvalObjConn();
			if(conn == null)
				continue;
			Datapoint dps2 = conn.getMeterComparisonValues();
			registerPlotDps(dps2, table, line, "_MeterComp");
			
		}		
	}
	
	protected void registerPlotDps(Datapoint dps, ConsumptionEvalTableBase<?> table, ConsumptionEvalTableLineI line,
			String postFix) {
		if(dps != null) {
			String plotGroupName = table.getShortLabel()+"_"+DatapointInfo.getDefaultShortLabel(line.getUtilityType())+postFix;
			DatapointGroup grp = controller.dpService.getGroup(plotGroupName);
			grp.addDatapoint(dps);
			grp.setLabel(null, plotGroupName);
			grp.registerAsChart(null);
		}		
	}
	
	/** Get all registered tables of a type
	 * 
	 * @param type if null tables for all types are returned
	 * @return
	 */
	public List<ConsumptionEvalTableBase<?>> getTables(UtilityType type) {
		List<ConsumptionEvalTableBase<?>> result = new ArrayList<>();
		for(ConsumptionEvalTableBase<?> table: evaluations) {
			if(type == null || table.getUtilityType().contains(type))
				result.add(table);
		}
		return result ;
	}
}
