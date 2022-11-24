package org.smartrplace.timeseries.manual.servlet;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.ogema.accessadmin.api.ApplicationManagerPlus;
import org.ogema.core.application.ApplicationManager;
import org.ogema.core.model.simple.FloatResource;
import org.ogema.devicefinder.api.Datapoint;
import org.smartrplace.timeseries.manual.model.ManualEntryData;
import org.smartrplace.util.frontend.servlet.ServletTimeseriesProvider;
import org.smartrplace.util.frontend.servlet.ServletTimeseriesProvider.WriteMode;
import org.smartrplace.util.frontend.servlet.UserServlet.ServletPageProvider;
import org.smartrplace.util.frontend.servlet.UserServlet.ServletValueProvider;

import de.iwes.util.resourcelist.ResourceListHelper;

/**
 * An HTML page, generated from the Java code.
 */
public class ManualTimeseriesServletList implements ServletPageProvider<ManualEntryData> {
	
	private final ApplicationManager appMan;
	private final ApplicationManagerPlus appManPlus;
	private final ManualEntryData configResource;
	
	public ManualTimeseriesServletList(ManualEntryData configResource, final ApplicationManagerPlus appManPlus) {
		this.appMan = appManPlus.appMan();
		this.appManPlus = appManPlus;
		this.configResource = configResource;
	}
	
	@Override
	public Map<String, ServletValueProvider> getProviders(ManualEntryData data, String user, Map<String, String[]> paramMap) {
		Map<String, ServletValueProvider> result = new HashMap<>();
		
		for(FloatResource fres: data.manualEntryDataHolder().getAllElements()) {
			String name = ResourceListHelper.getNameForElement(fres);
			ServletTimeseriesProvider tsProv = new ServletTimeseriesProvider(name, fres.program(), appMan, null,
					WriteMode.ANY, null, paramMap, -999f, null);
			Datapoint dp = appManPlus.dpService().getDataPointStandard(fres);
			tsProv.datapointForChangeNotification = dp;
			result.put(name, tsProv);
		}
			
		return result;
	}
	
	@Override
	public List<ManualEntryData> getAllObjects(String user) {
		return Arrays.asList(new ManualEntryData[] {configResource});
		//return result ;
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
