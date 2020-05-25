package org.ogema.devicefinder.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.ogema.core.model.ValueResource;
import org.ogema.core.model.simple.SingleValueResource;
import org.ogema.core.recordeddata.RecordedData;
import org.ogema.devicefinder.api.ConsumptionInfo.AggregationMode;
import org.ogema.devicefinder.api.Datapoint;
import org.ogema.devicefinder.api.DatapointService;
import org.ogema.devicefinder.util.DatapointImpl;
import org.smartrplace.tissue.util.resource.ValueResourceHelperSP;
import org.smartrplace.util.frontend.servlet.UserServletUtil;

import de.iwes.timeseries.eval.garo.api.base.GaRoDataType;
import de.iwes.timeseries.eval.garo.api.base.GaRoDataTypeI;
import de.iwes.timeseries.eval.garo.api.base.GaRoMultiEvalDataProvider;
import de.iwes.timeseries.eval.garo.api.helper.base.GaRoEvalHelper;
import de.iwes.timeseries.eval.garo.api.helper.base.GaRoEvalHelper.RecIdVal;
import de.iwes.timeseries.eval.garo.api.helper.base.GaRoEvalHelper.TypeChecker;
import de.iwes.widgets.api.widgets.localisation.OgemaLocale;

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
		GaRoDataTypeI type = result.getGaroDataType();
		if(type != null) {
			typeIdsRegistered.add(type.label(null));
			typeIdsKnown.add(type.label(null));
			if(type instanceof GaRoDataType)
				GaRoTypeStringConfigProviderDP.typesWithoutDescription.put(type.label(null), (GaRoDataType) type);
		}
	}
	public static void addStandardData(Datapoint result, ValueResource valRes) {
		addStandardData(result);
		SingleValueResource svr;
		if(valRes instanceof SingleValueResource)
			svr = (SingleValueResource) valRes;
		else
			svr = null;
		if(result.getTimeSeriesID() == null && svr != null) {
			RecordedData recData = ValueResourceHelperSP.getRecordedData(svr);
			//if(LoggingUtils.isLoggingEnabled(svr)) {
			if(recData != null && (!recData.isEmpty())) {	
				String id = UserServletUtil.getOrAddTimeSeriesData(recData, recData.getPath());
				result.setTimeSeriesID(id);
			}
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
		addStandardData(result, valRes);
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

	static Map<DataTypeRegistrationStatus, Collection<String>> typeIds = new HashMap<>();
	static Set<String> typeIdsKnown = new LinkedHashSet<String>(GaRoEvalHelper.recIdSnippets.keySet());
	static Set<String> typeIdsEvalDefault = new LinkedHashSet<String>();
	static Set<String> typeIdsRegistered = new LinkedHashSet<String>();
	static {
		for(GaRoDataType t: GaRoDataType.standardEvalTypes) {
			typeIdsEvalDefault.add(t.label(null));
		}
		typeIdsKnown.addAll(typeIdsEvalDefault);
		typeIds.put(DataTypeRegistrationStatus.ALL, typeIdsKnown);
		typeIds.put(DataTypeRegistrationStatus.EVAL_DEFAULT, typeIdsEvalDefault);
		typeIds.put(DataTypeRegistrationStatus.REGISTERED, typeIdsRegistered);
		typeIds.put(DataTypeRegistrationStatus.FOR_EVAL, GaRoTypeStringConfigProviderDP.typeIdsForEval);
	}
	@Override
	public List<GaRoDataType> getRegisteredDataTypes(DataTypeRegistrationStatus filter) {
		Map<String, RecIdVal> full = getDataTypeDescriptions(filter, true);
		List<GaRoDataType> result = new ArrayList<>();
		for(RecIdVal desc: full.values()) {
			result.add(desc.type);
		}
		return result ;
	}

	@Override
	public Map<String, RecIdVal> getDataTypeDescriptions(DataTypeRegistrationStatus filter,
			boolean includeEmptyDescriptions) {
		Collection<String> relevantIds = typeIds.get(filter);
		Map<String, RecIdVal> result = new LinkedHashMap<>();
		for(String id: relevantIds) {
			RecIdVal rec = GaRoTypeStringConfigProviderDP.recIdSnippets.get(id);
			if(rec == null) {
				if(includeEmptyDescriptions) {
					GaRoDataType type = GaRoTypeStringConfigProviderDP.typesWithoutDescription.get(id);
					if(type == null) {
						//very ugly, should not occur
						type = new GaRoDataType(id, null);
					}
					Map<OgemaLocale, String> label = new HashMap<OgemaLocale, String>();
					label.put(OgemaLocale.ENGLISH, "unknown:"+type.label(null));
					result.put(id, new RecIdVal(type, (String[])null, label));
				}
				continue;
			}
			result.put(id, rec);
		}
		return result ;
	}

	@Override
	public RecIdVal addDataTypeDescription(GaRoDataType type, List<String> snippets, String labelEnglish,
			boolean registerForEvalution) {
		Map<OgemaLocale, String> labels = new HashMap<OgemaLocale, String>();
		labels.put(OgemaLocale.ENGLISH, labelEnglish);
		return addDataTypeDescription(type, snippets, labels);
	}

	@Override
	public RecIdVal addDataTypeDescription(GaRoDataType type, List<String> snippets, Map<OgemaLocale, String> labels) {
		RecIdVal rec = new RecIdVal(type, snippets, labels);
		return addDataTypeDescription(rec);
	}

	@Override
	public RecIdVal addDataTypeDescription(GaRoDataType type, TypeChecker typeChecker, String labelEnglish,
			boolean registerForEvalution) {
		Map<OgemaLocale, String> labels = new HashMap<OgemaLocale, String>();
		labels.put(OgemaLocale.ENGLISH, labelEnglish);
		return addDataTypeDescription(type, typeChecker, labels);
	}

	@Override
	public RecIdVal addDataTypeDescription(GaRoDataType type, TypeChecker typeChecker,
			Map<OgemaLocale, String> labels) {
		RecIdVal rec = new RecIdVal(type, typeChecker, labels);
		return addDataTypeDescription(rec);
	}

	@Override
	public RecIdVal addDataTypeDescription(RecIdVal recIdVal) {
		RecIdVal existing = GaRoTypeStringConfigProviderDP.recIdSnippets.get(recIdVal.type.label(null));
		RecIdVal result;
		if(existing == null) {
			result = recIdVal;
			GaRoTypeStringConfigProviderDP.recIdSnippets.put(recIdVal.type.label(null), recIdVal);
		} else {
			result = existing;
			if(recIdVal.snippets != null) {
				if(existing.snippets != null)
					existing.snippets.addAll(recIdVal.snippets);
				else
					existing.snippets = recIdVal.snippets;
			}			
			if(recIdVal.typeChecker != null) {
				//higher priority
				existing.typeChecker = recIdVal.typeChecker;
			}			
		}
		typeIdsKnown.add(recIdVal.type.label(null));
		return result;
	}

	@Override
	public RecIdVal registerTypeForEvaluation(GaRoDataType type) {
		typeIdsKnown.add(type.label(null));
		GaRoTypeStringConfigProviderDP.typeIdsForEval.add(type.label(null));
		RecIdVal existing = GaRoTypeStringConfigProviderDP.recIdSnippets.get(type.label(null));
		return existing;
	}
}
