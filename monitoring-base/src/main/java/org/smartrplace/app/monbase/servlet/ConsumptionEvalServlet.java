package org.smartrplace.app.monbase.servlet;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.ogema.core.application.ApplicationManager;
import org.ogema.devicefinder.api.Datapoint;
import org.ogema.devicefinder.api.DatapointInfo;
import org.smartrplace.app.monbase.MonitoringController;
import org.smartrplace.app.monbase.power.ConsumptionEvalAdmin;
import org.smartrplace.app.monbase.power.ConsumptionEvalAdmin.SumType;
import org.smartrplace.app.monbase.power.ConsumptionEvalTableBase;
import org.smartrplace.app.monbase.power.ConsumptionEvalTableLineI;
import org.smartrplace.app.monbase.power.ColumnDataProvider;
import org.smartrplace.util.frontend.servlet.ServletNumListProvider;
import org.smartrplace.util.frontend.servlet.ServletNumProvider;
import org.smartrplace.util.frontend.servlet.ServletStringProvider;
import org.smartrplace.util.frontend.servlet.ServletTimeseriesProvider;
import org.smartrplace.util.frontend.servlet.UserServlet.ServletPageProvider;
import org.smartrplace.util.frontend.servlet.UserServlet.ServletValueProvider;


/** Servlet  /org/sp/app/monappserv/userdata?object={:object}&structure={:structure}&startTime={:startTime}&endtime={:endTime}&page=consumptionData
 * Return data for all {@link ConsumptionEvalTableLineI}s in all pages for the utility type requested
 *
 */
public class ConsumptionEvalServlet implements ServletPageProvider<ConsumptionEvalPageEntry> {
	protected final ApplicationManager appMan;
	protected final ConsumptionEvalAdmin evalAdm;
	
	public ConsumptionEvalServlet(MonitoringController controller) {
		this.appMan = controller.appMan;
		this.evalAdm = controller.evalAdm;
	}

	@Override
	public Map<String, ServletValueProvider> getProviders(ConsumptionEvalPageEntry object, String user,
			Map<String, String[]> parameters) {
		Map<String, ServletValueProvider> result = new HashMap<>();
		
		
		long now = appMan.getFrameworkTime();
		long[] startEnd = ServletTimeseriesProvider.getDayStartEnd(parameters, appMan);
		//for(ConsumptionEvalTableBase<?> page: pages) {
			//@SuppressWarnings("unchecked")
			//Collection<ConsumptionEvalTableLineI> allLines = (Collection<ConsumptionEvalTableLineI>) page.getAllObjectsInTable();
			//for(ConsumptionEvalTableLineI line: allLines) {
		Datapoint dp = object.line.getDatapoint();
		if(dp != null) {
			ServletStringProvider nameP = new ServletStringProvider(dp.label(null));
			result.put("name", nameP);
			ServletStringProvider roomP = new ServletStringProvider(dp.getRoomName(null));
			result.put("room", roomP);
			if(dp.getSubRoomLocation(null, null) != null) {
				ServletStringProvider subRoomP = new ServletStringProvider(dp.getSubRoomLocation(null, null));
				result.put("locationInRoom", subRoomP);						
			}
		} else {
			ServletStringProvider nameP = new ServletStringProvider(object.line.getLabel());
			result.put("name", nameP);					
		}
		String label = object.line.getLabel();
		boolean isMainMeter = false;
		if(object.line.getLineType() == SumType.SUM_LINE &&
				(!(label.contains("Device")||label.contains("Room")||label.contains("Manual")||label.contains("manuell")))) {
			isMainMeter = true;
		}
		ServletNumProvider mainMeterP = new ServletNumProvider(isMainMeter);
		result.put("isMainMeter", mainMeterP);
		float val = object.line.getPhaseValue(0, startEnd[0], startEnd[1], now, object.allLines);
		ServletNumProvider valueP = new ServletNumProvider(val);
		result.put("value", valueP);
		ServletStringProvider util = new ServletStringProvider(object.line.getUtilityType().name());
		result.put("utilityType", util);
		String unit = DatapointInfo.getDefaultUnit(object.line.getUtilityType());
		if(unit != null) {
			ServletStringProvider unitP = new ServletStringProvider(unit);
			result.put("unit", unitP);			
		}
		ColumnDataProvider costProv = object.line.getCostProvider();
		ServletValueProvider cost;
		if(costProv != null) {
			String costVal = costProv.getString(val, startEnd[0], startEnd[1]);
			try {
				float costF = Float.parseFloat(costVal);
				cost = new ServletNumProvider(costF);
			} catch(NumberFormatException e) {
				cost = new ServletStringProvider(costVal);				
			}
			result.put("cost", cost);
		}
		ServletNumProvider index = new ServletNumProvider(Integer.parseInt(object.line.getLinePosition()));
		result.put("index", index);
		List<Float> values = new ArrayList<>();
		List<String> names = new ArrayList<>();
		Float savings = null;
		for(int subPh=0; subPh<object.line.hasSubPhaseNum(); subPh++) {
			float valLoc = object.line.getPhaseValue(subPh, startEnd[0], startEnd[1], now, object.allLines);
			values.add(valLoc);
			names.add(object.page.getPhaseLabels()[subPh]); //line.""+(subPh+1));
			if(object.page.getPhaseLabels()[subPh].startsWith("Estimated Savings"))
				savings = valLoc;
		}

		ServletNumListProvider<Float> phaseValuesP = new ServletNumListProvider<Float>(values, "--");
		result.put("phaseValues", phaseValuesP);
		ServletNumListProvider<String> phaseNamesP = new ServletNumListProvider<String>(names);
		result.put("phaseNames", phaseNamesP);					

		ServletValueProvider savingsP;
		if(savings != null) {
			savingsP = new ServletNumProvider(savings);
			result.put("estimatedSavings", savingsP);
		}

		//Datapoint dailyTs = object.line.getDailyConsumptionValues();
		Datapoint dailyTs = null;
		if(object.line.getEvalObjConn() != null) {
			dailyTs = object.line.getEvalObjConn().getDailyConsumptionValues();
		}
		if(dailyTs != null) {
			dailyTs.setTimeSeriesID(null);
			String tsId = dailyTs.getTimeSeriesID();
			ServletStringProvider timeSeriesId = new ServletStringProvider(tsId);
			result.put("dailyValues_TsId", timeSeriesId);				
		}
		//Datapoint hourlyTs = object.line.getHourlyConsumptionValues();
		Datapoint hourlyTs = null;
		if(object.line.getEvalObjConn() != null) {
			hourlyTs = object.line.getEvalObjConn().getHourlyConsumptionValues();
		}
		if(hourlyTs != null) {
			hourlyTs.setTimeSeriesID(null);
			String tsId = hourlyTs.getTimeSeriesID();
			ServletStringProvider timeSeriesId = new ServletStringProvider(tsId);
			result.put("hourlyValues_TsId", timeSeriesId);				
		}
		return result;
	}

	//TODO: This won't work for several pages sharing rows with same labels
	Map<String, ConsumptionEvalPageEntry> lines = new HashMap<>();
	
	@Override
	public Collection<ConsumptionEvalPageEntry> getAllObjects(String user) {
		List<ConsumptionEvalTableBase<?>> pages = evalAdm.getTables(null);
		Collection<ConsumptionEvalPageEntry> result = new ArrayList<>();
		for(ConsumptionEvalTableBase<?> page: pages) {
			@SuppressWarnings("unchecked")
			Collection<ConsumptionEvalTableLineI> allLines = (Collection<ConsumptionEvalTableLineI>) page.getAllObjectsInTable();
			for(ConsumptionEvalTableLineI line: allLines) {
				ConsumptionEvalPageEntry entry = new ConsumptionEvalPageEntry();
				entry.allLines = allLines;
				entry.line = line;
				entry.page = page;
				lines.put(line.getLabel(), entry);
				result.add(entry);
			}
		}
		return result ;
	}

	@Override
	public ConsumptionEvalPageEntry getObject(String objectId) {
		return lines.get(objectId);
	}
	
	@Override
	public String getObjectId(ConsumptionEvalPageEntry obj) {
		return obj.line.getLabel();
	}
}
