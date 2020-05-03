package org.ogema.devicefinder.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.ogema.core.model.ValueResource;
import org.ogema.devicefinder.api.ConsumptionInfo.AggregationMode;
import org.ogema.devicefinder.api.Datapoint;
import org.ogema.devicefinder.api.DatapointService;
import org.ogema.devicefinder.util.DatapointImpl;

import de.iwes.timeseries.eval.garo.api.base.GaRoMultiEvalDataProvider;
import de.iwes.timeseries.eval.garo.api.helper.base.GaRoEvalHelper;

/** Implementation of central Data Point Service
 * TODO: Currently identification and generation of data row information for plots is done via an 
 * EvaluationProvider. So this information does not contain any Datapoint information yet.
 * Goals for Plotting:
 *   > Offer plots that have content (if no ElectricityConsumption, WaterConsumption info is there, the respective plot should not
 *   be offered at all)
 *   > On the other hand drivers and their extensions should be able to provide all information to offer a plot for the data and
 *   to include the data into an "all data plot" for a room etc.
 *   > Drivers or their extensions should also be able to provide {@link AggregationMode} information etc.
 */

@Service(DatapointService.class)
@Component
public class DatapointServiceImpl implements DatapointService {
	/** GatewayId -> Resource-location -> Datapoint object*/
	Map<String, Map<String, Datapoint>> knownDps = new HashMap<>();
	
	@Override
	public Datapoint getDataPointStandard(String resourceLocation) {
		return getDataPointStandard(resourceLocation, GaRoMultiEvalDataProvider.LOCAL_GATEWAY_ID);
	}

	@Override
	public Datapoint getDataPointAsIs(String resourceLocation) {
		return getDataPointAsIs(resourceLocation, GaRoMultiEvalDataProvider.LOCAL_GATEWAY_ID);
	}

	@Override
	public Datapoint getDataPointStandard(String resourceLocation, String gatewayId) {
		Datapoint result = getDataPointAsIs(resourceLocation);
		if(result == null) {
			result = new DatapointImpl(resourceLocation, gatewayId, null, null);
			Map<String, Datapoint> gwMap = getGwMap(gatewayId);
			gwMap.put(resourceLocation, result);
		}
		addStandardData(result);
		return result;
	}

	@Override
	public Datapoint getDataPointAsIs(String resourceLocation, String gatewayId) {
		Map<String, Datapoint> subMap = knownDps.get(gatewayId);
		if(subMap == null)
			return null;
		return subMap.get(resourceLocation);
	}

	protected Map<String, Datapoint> getGwMapLocal() {
		return getGwMap(GaRoMultiEvalDataProvider.LOCAL_GATEWAY_ID);
	}
	protected Map<String, Datapoint> getGwMap(String gatewayId) {
		Map<String, Datapoint> subMap = knownDps.get(gatewayId);
		if(subMap == null) {
			subMap = new HashMap<String, Datapoint>();
			knownDps.put(gatewayId, subMap);
		}
		return subMap;
	}
	
	public static void addStandardData(Datapoint result) {
		if(result.getGaroDataType() == null) {
			result.setGaroDataType(GaRoEvalHelper.getDataType(result.getLocation()));
		}
	}

	@Override
	public Datapoint getDataPointStandard(ValueResource valRes) {
		Datapoint result = getDataPointAsIs(valRes);
		if(result == null) {
			result = new DatapointImpl(valRes.getLocation(), null, valRes, null);
			Map<String, Datapoint> gwMap = getGwMap(GaRoMultiEvalDataProvider.LOCAL_GATEWAY_ID);
			gwMap.put(valRes.getLocation(), result);
		}
		addStandardData(result);
		return result;
	}

	@Override
	public Datapoint getDataPointAsIs(ValueResource valRes) {
		return getDataPointAsIs(valRes.getLocation());
	}

	@Override
	public List<Datapoint> getAllDatapoints() {
		List<Datapoint> result = new ArrayList<Datapoint>();
		for(Map<String, Datapoint> baselist: knownDps.values()) {
			result.addAll(baselist.values());
			//for(Datapoint dp: baselist.values()) {
			//	result.add(dp);
			//}
		}
		return result;
	}
}
