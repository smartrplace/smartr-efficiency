package org.smartrplace.timeseries.manual.servlet;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.ogema.accessadmin.api.ApplicationManagerPlus;
import org.ogema.core.application.ApplicationManager;
import org.ogema.core.model.simple.FloatResource;
import org.ogema.core.model.simple.StringResource;
import org.ogema.devicefinder.api.Datapoint;
import org.smartrplace.apps.hw.install.config.InstallAppDevice;
import org.smartrplace.timeseries.manual.model.ManualEntryData;
import org.smartrplace.tissue.util.format.StringFormatHelperSP;
import org.smartrplace.util.frontend.servlet.ServletNumProvider;
import org.smartrplace.util.frontend.servlet.ServletStringProvider;
import org.smartrplace.util.frontend.servlet.ServletSubDataProvider;
import org.smartrplace.util.frontend.servlet.UserServlet.ReturnStructure;
import org.smartrplace.util.frontend.servlet.UserServlet.ServletPageProvider;
import org.smartrplace.util.frontend.servlet.UserServlet.ServletValueProvider;

import de.iwes.timeseries.eval.garo.api.base.GaRoDataType;
import de.iwes.timeseries.eval.garo.api.base.GaRoDataTypeI.AggregationModePlus;
import de.iwes.util.resource.ResourceHelper;

/**
 * An HTML page, generated from the Java code.
 */
public class ManualTimeseriesServlet implements ServletPageProvider<ManualEntryData> {
	public static final List<String> knownApplications = Arrays.asList(new String[] {"Grid", "Battery", "Solar", "Water", "Heating", "Elevator", "Tenant"});
	
	final ApplicationManager appMan;
	final ApplicationManagerPlus appManPlus;
	private final static Map<String, ManualEntryData> configResources = new HashMap<>();
	
	public static void registerManualEntryData(ManualEntryData data) {
		configResources.put(data.getLocation(), data);
	}
	
	public ManualTimeseriesServlet(final ApplicationManagerPlus appManPlus) {
		this.appMan = appManPlus.appMan();
		this.appManPlus = appManPlus;
	}
	
	@Override
	public Map<String, ServletValueProvider> getProviders(ManualEntryData data, String user, Map<String, String[]> paramMap) {
		Map<String, ServletValueProvider> result = new HashMap<>();
		
		ServletValueProvider unit = new ServletStringProvider(data.manualEntryUnit().getValue());
		result.put("unit", unit);
	
		ServletNumProvider lowerLimit = new ServletNumProvider(data.lowerLimit().getValue());
		result.put("lowerLimit", lowerLimit);
		ServletNumProvider upperLimit = new ServletNumProvider(data.upperLimit().getValue());
		result.put("upperLimit", upperLimit);
		
		ZoneOffset utcOffset = ZoneOffset.systemDefault().getRules().getOffset(Instant.now());
		result.put("UTCoffset", new ServletNumProvider(utcOffset.getTotalSeconds()*1000));

		ServletPageProvider<ManualEntryData> provider = new ManualTimeseriesServletList(data, appManPlus);
		ServletSubDataProvider<ManualEntryData> tsList = new ServletSubDataProvider<ManualEntryData>(provider , data, true,
				ReturnStructure.DICTIONARY, paramMap, true);
		result.put("timeseriesList", tsList);
		
		FloatResource fres = ResourceHelper.getSubResourceBest(data.manualEntryDataHolder(), "energySensor", FloatResource.class);
		if(fres != null) {
			Datapoint dp = appManPlus.dpService().getDataPointStandard(fres);
			ServletStringProvider idp = new ServletStringProvider(dp.id());
			result.put("datapointId", idp);
			InstallAppDevice iad = appManPlus.dpService().getMangedDeviceResourceForSubresource(fres.getLocationResource());
			if(iad != null) {
				GaRoDataType garo = dp.getGaroDataType();
				addInfoDpFields(iad, garo, result);
			}
		}
		
		return result;
	}
	
	@Override
	public List<ManualEntryData> getAllObjects(String user) {
		return new ArrayList<>(configResources.values()); //Arrays.asList(new ManualEntryData[] {configResource});
	}

	@Override
	public ManualEntryData getObject(String objectId, String user) {
		return configResources.get(objectId);
	}
	
	@Override
	public String getObjectId(ManualEntryData obj) {
		return obj.getLocation();
	}

	@Override
	public String getObjectName() {
		return "manualentryset";
	}
	
	public static void addInfoDpFields(InstallAppDevice iad, GaRoDataType garo,
			Map<String, ServletValueProvider> result ) {
		String devInstallLocation = null;
		StringResource devAppRes = iad.getSubResource("apiApplication", StringResource.class);
		if(devAppRes != null && devAppRes.exists())
			devInstallLocation = devAppRes.getValue();
		else {
			devInstallLocation = iad.installationLocation().getValue();
			if(!knownApplications.contains(devInstallLocation))
				devInstallLocation = null;
		}
		if(devInstallLocation != null) {
			ServletStringProvider application = new ServletStringProvider(devInstallLocation);
			result.put("application", application);
		}
		StringResource utilityRes = iad.getSubResource("deviceUtility", StringResource.class);
		if(utilityRes != null && utilityRes.exists()) {
			String utily = utilityRes.getValue();
			ServletStringProvider utility = new ServletStringProvider(utily);
			result.put("utility", utility);
		}
		String displayName = null;
		StringResource displayNameRes = iad.getSubResource("deviceDisplayName", StringResource.class);
		if(displayNameRes != null && displayNameRes.exists()) {
			displayName = displayNameRes.getValue();
			ServletStringProvider deviceDisplayName = new ServletStringProvider(displayName);
			result.put("deviceDisplayName", deviceDisplayName);
		}
		if(garo != null) {
			AggregationModePlus agg = garo.aggregationMode();
			if(agg != null && agg != AggregationModePlus.AVERAGE_VALUE_PER_STEP) {
				ServletStringProvider deviceAggregationType = new ServletStringProvider(StringFormatHelperSP.getCamelCase(agg.toString()));
				result.put("aggregationType", deviceAggregationType);
				
			}
		}		
	}
}
