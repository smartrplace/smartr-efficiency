package org.ogema.timeseries.eval.simple.api;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.ogema.core.application.ApplicationManager;
import org.ogema.core.channelmanager.measurements.SampledValue;
import org.ogema.core.model.simple.TimeResource;
import org.ogema.core.timeseries.ReadOnlyTimeSeries;
import org.ogema.devicefinder.api.ConsumptionInfo.AggregationMode;
import org.ogema.devicefinder.api.Datapoint;
import org.ogema.devicefinder.api.DatapointService;
import org.ogema.externalviewer.extensions.ScheduleViewerOpenButtonEval.TimeSeriesNameProvider;
import org.smartrplace.app.monbase.gui.TimeSeriesServlet;
import org.smartrplace.app.monbase.gui.TimeSeriesServlet.MeterReference;
import org.smartrplace.tissue.util.resource.ResourceHelperSP;

import de.iwes.timeseries.eval.api.TimeSeriesData;

public abstract class TimeseriesSimpleProcUtil {
	public static final String METER_EVAL = "METER";
	public static final String PER_DAY_EVAL = "DAY";
	public static final String SUM_PER_DAY_EVAL = "SUM_PER_DAY";
	public static final String SUM_PER_DAY_PER_ROOM_EVAL = "DAY_PER_ROOM";
	
	public final Map<String, TimeseriesSetProcessor> knownProcessors = new HashMap<>();
	
	protected abstract TimeSeriesNameProvider nameProvider();
	protected abstract AggregationMode getMode(String tsLabel);
	protected final ApplicationManager appMan;
	protected final DatapointService dpService;
	
	// This is from the MonitoringController API definition. Maybe this should be revised anyways.
	//protected abstract List<String> getAllRooms(OgemaLocale locale);
	//protected String getRoomLabel(String resLocation, OgemaLocale locale);
	
	//Better use this from DatapointService?
	
	public TimeseriesSimpleProcUtil(ApplicationManager appMan, DatapointService dpService) {
		this.appMan = appMan;
		this.dpService = dpService;
		
		TimeseriesSetProcessor meterProc = new TimeseriesSetProcSingleToSingle("_vm") {
			
			@Override
			protected TimeSeriesNameProvider nameProvider() {
				return TimeseriesSimpleProcUtil.this.nameProvider();
			}
			
			@Override
			protected AggregationMode getMode(String tsLabel) {
				return TimeseriesSimpleProcUtil.this.getMode(tsLabel);
			}
			
			@Override
			protected List<SampledValue> calculateValues(ReadOnlyTimeSeries timeSeries, long start, long end,
					AggregationMode mode) {
				MeterReference ref = new MeterReference();
				ref.referenceMeterValue = 0;
				TimeResource refRes = ResourceHelperSP.getSubResource(null,
						"offlineEvaluationControlConfig/energyEvaluationInterval/initialTest/start",
						TimeResource.class, appMan.getResourceAccess());
				ref.referenceTime = refRes.getValue();
				return TimeSeriesServlet.getMeterFromConsumption(timeSeries, start, end, ref, mode);						
			}
		};
		knownProcessors.put(METER_EVAL, meterProc);
		
		TimeseriesSetProcessor dayProc = new TimeseriesSetProcSingleToSingle("_proTag") {
			
			@Override
			protected TimeSeriesNameProvider nameProvider() {
				return TimeseriesSimpleProcUtil.this.nameProvider();
			}
			
			@Override
			protected AggregationMode getMode(String tsLabel) {
				return TimeseriesSimpleProcUtil.this.getMode(tsLabel);
			}
			
			@Override
			protected List<SampledValue> calculateValues(ReadOnlyTimeSeries timeSeries, long start, long end,
					AggregationMode mode) {
				return TimeSeriesServlet.getDayValues(timeSeries, start, end, mode, 1.0f);						
			}
		};
		knownProcessors.put(PER_DAY_EVAL, dayProc);
		
		TimeseriesSetProcessor sumProc = new TimeseriesSetProcessor() {
			
			@Override
			public List<TimeSeriesData> getResultSeries(List<TimeSeriesData> input, DatapointService dpService) {
				List<TimeSeriesData> result1 = dayProc.getResultSeries(input, dpService);
				TimeseriesSetProcessor sumProc = new TimeseriesSetProcSum("total_sum");
				List<TimeSeriesData> result = sumProc.getResultSeries(result1, dpService);
				return result;
			}
		};
		knownProcessors.put(SUM_PER_DAY_EVAL, sumProc);

		
		TimeseriesSetProcessor dayPerRoomProc = new TimeseriesSetProcessor() {
			
			@Override
			public List<TimeSeriesData> getResultSeries(List<TimeSeriesData> input, DatapointService dpService) {
				List<TimeSeriesData> result1 = dayProc.getResultSeries(input, dpService);
				List<TimeSeriesData> result = new ArrayList<>();
				// RoomID -> Timeseries in the room
				Map<String, List<TimeSeriesData>> sortedbyRoom = new HashMap<>();
				for(TimeSeriesData tsd: result1) {
					Datapoint dp = dpService.getDataPointStandard(tsd.id());
					if(dp.getRoom() != null) {
						List<TimeSeriesData> roomList = sortedbyRoom.get(dp.getRoom().id());
						if(roomList == null) {
							roomList = new ArrayList<>();
							sortedbyRoom.put(dp.getRoom().id(), roomList);
						}
						roomList.add(tsd);
					}
						
				}
				for(Entry<String, List<TimeSeriesData>> roomData: sortedbyRoom.entrySet()) {
					TimeseriesSetProcessor sumProc = new TimeseriesSetProcSum(roomData.getKey()+"_sum");
					List<TimeSeriesData> resultLoc = sumProc.getResultSeries(roomData.getValue(), dpService);
					result.addAll(resultLoc);
				}
				return result;
			}
		};

		knownProcessors.put(SUM_PER_DAY_PER_ROOM_EVAL, dayPerRoomProc);
	}
	public List<TimeSeriesData> process(String tsProcessRequest, List<TimeSeriesData> input) {
		TimeseriesSetProcessor proc = knownProcessors.get(tsProcessRequest);
		if(proc == null)
			throw new IllegalArgumentException("Unknown timeseries processor: "+tsProcessRequest);
		return proc.getResultSeries(input, dpService);
	}
}
