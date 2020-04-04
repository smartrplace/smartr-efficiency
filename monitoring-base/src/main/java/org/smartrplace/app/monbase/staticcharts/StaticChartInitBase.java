package org.smartrplace.app.monbase.staticcharts;

import java.util.ArrayList;
import java.util.List;

import org.ogema.externalviewer.extensions.DefaultScheduleViewerConfigurationProviderExtended;
import org.smartrplace.app.monbase.MonitoringController;
import org.smartrplace.app.monbase.gui.ScheduleViewerOpenButtonDataProviderImpl;
import org.smartrplace.app.monbase.gui.TimeSeriesNameProviderImpl;

import de.iwes.timeseries.eval.api.TimeSeriesData;

public class StaticChartInitBase {
	protected final MonitoringController controller;
	
	public StaticChartInitBase(MonitoringController controller) { //, long startTime, long endTime) {
		this.controller = controller;
	}
	
	protected void registerData(String room, String dataType, ScheduleViewerOpenButtonDataProviderImpl allProv,
			DefaultScheduleViewerConfigurationProviderExtended schedConfigProv) {
		List<TimeSeriesData> input = allProv.getData(null);
		List<TimeSeriesData> toRemove = new ArrayList<>();
		List<String> found = new ArrayList<String>();
		for(TimeSeriesData inp: input) {
			if(found.contains(inp.id())) toRemove.add(inp);
			else found.add(inp.id());
		}
		input.removeAll(toRemove);
		
		TimeSeriesNameProviderImpl sprov = new TimeSeriesNameProviderImpl(controller);
		String ci = controller.registerStaticTimeSeriesViewerLink(sprov ,
					input,
					schedConfigProv, false);			
		System.out.println("      For "+room+":"+dataType+" added ts expert ID:"+ci);		
	}

}
