package org.smartrplace.timeseries.manual.servlet;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.ogema.accessadmin.api.ApplicationManagerPlus;
import org.ogema.core.application.ApplicationManager;
import org.smartrplace.timeseries.manual.model.ManualEntryData;
import org.smartrplace.util.frontend.servlet.ServletNumProvider;
import org.smartrplace.util.frontend.servlet.ServletStringProvider;
import org.smartrplace.util.frontend.servlet.ServletSubDataProvider;
import org.smartrplace.util.frontend.servlet.UserServlet.ServletPageProvider;
import org.smartrplace.util.frontend.servlet.UserServlet.ServletValueProvider;

/**
 * An HTML page, generated from the Java code.
 */
public class ManualTimeseriesServlet implements ServletPageProvider<ManualEntryData> {
	
	final ApplicationManager appMan;
	final ApplicationManagerPlus appManPlus;
	final ManualEntryData configResource;
	
	public ManualTimeseriesServlet(ManualEntryData configResource, final ApplicationManagerPlus appManPlus) {
		this.appMan = appManPlus.appMan();
		this.appManPlus = appManPlus;
		this.configResource = configResource;
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

		ServletPageProvider<ManualEntryData> provider = new ManualTimeseriesServletList(configResource, appManPlus);
		ServletSubDataProvider<ManualEntryData> tsList = new ServletSubDataProvider<ManualEntryData>(provider , data, true, paramMap);
		result.put("timeseriesList", tsList);
			
		return result;
	}
	
	@Override
	public List<ManualEntryData> getAllObjects(String user) {
		return Arrays.asList(new ManualEntryData[] {configResource});
	}

	@Override
	public ManualEntryData getObject(String objectId, String user) {
		return configResource;
	}
	
	@Override
	public String getObjectId(ManualEntryData obj) {
		return obj.getLocation();
	}

}
