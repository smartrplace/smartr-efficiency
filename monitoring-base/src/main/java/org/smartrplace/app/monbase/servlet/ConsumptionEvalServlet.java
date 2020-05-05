package org.smartrplace.app.monbase.servlet;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.ogema.core.application.ApplicationManager;
import org.ogema.devicefinder.api.ConsumptionInfo.UtilityType;
import org.ogema.devicefinder.api.Datapoint;
import org.smartrplace.app.monbase.MonitoringController;
import org.smartrplace.app.monbase.power.ConsumptionEvalAdmin;
import org.smartrplace.app.monbase.power.ConsumptionEvalTableBase;
import org.smartrplace.app.monbase.power.ConsumptionEvalTableLineI;
import org.smartrplace.util.frontend.servlet.ServletNumProvider;
import org.smartrplace.util.frontend.servlet.ServletStringProvider;
import org.smartrplace.util.frontend.servlet.ServletTimeseriesProvider;
import org.smartrplace.util.frontend.servlet.UserServlet.ServletPageProvider;
import org.smartrplace.util.frontend.servlet.UserServlet.ServletValueProvider;


/** Servlet  /org/sp/app/monappserv/userdata?object={:object}&structure={:structure}&startTime={:startTime}&endtime={:endTime}&page=consumptionData
 * Return data for all {@link ConsumptionEvalTableLineI}s in all pages for the utility type requested
 *
 */
public class ConsumptionEvalServlet implements ServletPageProvider<UtilityType> {
	protected final ApplicationManager appMan;
	protected final ConsumptionEvalAdmin evalAdm;
	
	public ConsumptionEvalServlet(MonitoringController controller) {
		this.appMan = controller.appMan;
		this.evalAdm = controller.evalAdm;
	}

	@Override
	public Map<String, ServletValueProvider> getProviders(UtilityType object, String user,
			Map<String, String[]> parameters) {
		Map<String, ServletValueProvider> result = new HashMap<>();
		
		List<ConsumptionEvalTableBase<?>> pages = evalAdm.getTables(object);
		long now = appMan.getFrameworkTime();
		long[] startEnd = ServletTimeseriesProvider.getDayStartEnd(parameters, appMan);
		for(ConsumptionEvalTableBase<?> page: pages) {
			for(ConsumptionEvalTableLineI line: page.getAllObjectsInTable()) {
				Datapoint dp = line.getDatapoint();
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
					ServletStringProvider nameP = new ServletStringProvider(line.getLabel());
					result.put("name", nameP);					
				}
				float val = line.getPhaseValue(0, startEnd[0], startEnd[1], now, null);
				ServletNumProvider valueP = new ServletNumProvider(val);
				result.put("value", valueP);
				List<Float> values = new ArrayList<>();
				List<String> names = new ArrayList<>();
				for(int subPh=0; subPh<line.hasSubPhaseNum(); subPh++) {
					values.add(line.getPhaseValue(subPh, startEnd[0], startEnd[1], now, null));
					names.add(""+(subPh+1));
				}
				//TODO
				//ServletNumProvider phaseValuesP = new ServletNumListProvider(values);
				//result.put("phaseValues", phaseValuesP);
				//ServletNumProvider phaseNamesP = new ServletNumListProvider(names);
				//result.put("phaseNames", phaseNamesP);					
			}
		}
		
		return result;
	}

	@Override
	public Collection<UtilityType> getAllObjects(String user) {
		return Arrays.asList(UtilityType.values());
	}

	@Override
	public UtilityType getObject(String objectId) {
		return UtilityType.valueOf(objectId);
	}
}
