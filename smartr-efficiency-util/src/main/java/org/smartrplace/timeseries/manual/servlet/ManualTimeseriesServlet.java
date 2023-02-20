package org.smartrplace.timeseries.manual.servlet;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.ogema.accessadmin.api.ApplicationManagerPlus;
import org.ogema.core.application.ApplicationManager;
import org.smartrplace.timeseries.manual.model.ManualEntryData;
import org.smartrplace.util.frontend.servlet.ServletNumProvider;
import org.smartrplace.util.frontend.servlet.ServletStringProvider;
import org.smartrplace.util.frontend.servlet.ServletSubDataProvider;
import org.smartrplace.util.frontend.servlet.UserServlet.ReturnStructure;
import org.smartrplace.util.frontend.servlet.UserServlet.ServletPageProvider;
import org.smartrplace.util.frontend.servlet.UserServlet.ServletValueProvider;

/**
 * An HTML page, generated from the Java code.
 */
public class ManualTimeseriesServlet implements ServletPageProvider<ManualEntryData> {
	
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
}
