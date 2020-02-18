package org.smartrplace.app.monbase.staticcharts;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.ogema.externalviewer.extensions.DefaultScheduleViewerConfigurationProviderExtended;
import org.smartrplace.app.monbase.MonitoringController;
import org.smartrplace.app.monbase.gui.ScheduleViewerOpenButtonDataProviderImpl;
import org.smartrplace.app.monbase.gui.TimeSeriesNameProviderImpl;

import com.iee.app.evaluationofflinecontrol.util.ScheduleViewerConfigProvEvalOff;

import de.iwes.timeseries.eval.api.TimeSeriesData;
import de.iwes.timeseries.eval.garo.multibase.GaRoSingleEvalProvider;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;

public class StaticChartInitBase {
	protected final MonitoringController controller;
	
	public StaticChartInitBase(MonitoringController controller) { //, long startTime, long endTime) {
		this.controller = controller;
	}

	public void registerWaterAirTemp(List<String> ROOM_NAMES) {
		//List<TemperatureSensor> tempSens = appMan.getResourceAccess().getResources(TemperatureSensor.class);
		DefaultScheduleViewerConfigurationProviderExtended schedConfigProv = ScheduleViewerConfigProvEvalOff.getInstance();
		
		for(String room: ROOM_NAMES) {
			ScheduleViewerOpenButtonDataProviderImpl allProv = new ScheduleViewerOpenButtonDataProviderImpl(controller) {
				
				@Override
				protected List<String> getRoomIDs(OgemaHttpRequest req) {
					return Arrays.asList(new String[] {room});
				}
				
				@Override
				protected GaRoSingleEvalProvider getEvalProvider(OgemaHttpRequest req) {
					return controller.getDefaultProvider();
				}
				
				@Override
				protected String getDataType(OgemaHttpRequest req) {
					return "Wasser- und Luftemperatur";
					//return "Alle(Std)";
				}
			};
			registerData(room, "Wasser- und Luftemperatur", allProv, schedConfigProv);
		}
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
