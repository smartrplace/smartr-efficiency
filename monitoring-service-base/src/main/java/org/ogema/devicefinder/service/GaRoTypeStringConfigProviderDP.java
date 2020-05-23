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
import org.ogema.widgets.configuration.service.OGEMAConfigurationProvider;

import de.iwes.timeseries.eval.garo.api.base.GaRoDataType;
import de.iwes.timeseries.eval.garo.api.helper.base.GaRoEvalHelper;
import de.iwes.timeseries.eval.garo.api.helper.base.GaRoEvalHelper.RecIdVal;
import de.iwes.widgets.api.widgets.localisation.OgemaLocale;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;

/** Finds a GaRoDataType for a recId. Uses recId as property name. For this reason no list of properties
 * can be provided
 * @author dnestle
 *
 */
@Service(OGEMAConfigurationProvider.class)
@Component
public class GaRoTypeStringConfigProviderDP implements OGEMAConfigurationProvider {
	
	static final Map<String, RecIdVal> recIdSnippets = new LinkedHashMap<>(GaRoEvalHelper.recIdSnippets);
	static Set<String> typeIdsForEval = new LinkedHashSet<>();
	static Map<String, GaRoDataType> typesWithoutDescription = new HashMap<>();

	//static {
	//	GaRoEvalHelper.addRecId(GaRoDataType.WaterConductivityValue, new String[] {"/Leitwert_S__1/sensor/reading"}, recIdSnippets);
	//}

	@Override
	public String className() {
		return GaRoDataType.class.getName();
	}

	@Override
	public int priority() {
		return 10;
	}

	@Override
	public List<OGEMAConfigurationProvider> additionalProviders() {
		return null;
	}

	@Override
	public Collection<String> propertiesProvided() {
		return null;
	}

	@Override
	public String getProperty(String property, OgemaLocale locale, OgemaHttpRequest req, Object context) {
		return null;
	}

	@Override
	public Object getObject(String property, OgemaLocale locale, OgemaHttpRequest req, Object context) {
		if(property.equals("%recSnippets")) {
			return recIdSnippets;
		}
		if(property.equals("%evalTypes")) {
			List<GaRoDataType> result = new ArrayList<>();
			for(String id: typeIdsForEval) {
				GaRoDataType type = getGaRoType(id);
				if(type != null)
					result.add(type);
			}
			return result;
		}
		return null;
	}

	public static GaRoDataType getGaRoType(String id) {
		GaRoDataType type = GaRoTypeStringConfigProviderDP.typesWithoutDescription.get(id);
		if(type != null)
			return type;
		RecIdVal rec = GaRoTypeStringConfigProviderDP.recIdSnippets.get(id);
		if(rec != null) {
			return rec.type;
		}
		return null;
	}
}
