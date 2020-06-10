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
import org.ogema.devicefinder.api.DPRoom;
import org.ogema.devicefinder.api.Datapoint;
import org.ogema.devicefinder.api.DatapointInfo.AggregationMode;
import org.ogema.devicefinder.util.AggregationModeProvider;
import org.ogema.devicefinder.util.DPRoomImpl;
import org.ogema.devicefinder.util.DPUtil;
import org.ogema.devicefinder.api.DatapointService;
import org.ogema.externalviewer.extensions.ScheduleViewerOpenButtonEval.TimeSeriesNameProvider;
import org.smartrplace.app.monbase.gui.TimeSeriesServlet;
import org.smartrplace.app.monbase.gui.TimeSeriesServlet.MeterReference;
import org.smartrplace.tissue.util.resource.ResourceHelperSP;

import de.iwes.timeseries.eval.api.TimeSeriesData;

public class TimeseriesSimpleProcUtil {
	public static final String METER_EVAL = "METER";
	public static final String PER_DAY_EVAL = "DAY";
	public static final String SUM_PER_DAY_EVAL = "SUM_PER_DAY";
	public static final String SUM_PER_DAY_PER_ROOM_EVAL = "DAY_PER_ROOM";
	
	public static final DPRoom unknownRoom = new DPRoomImpl(Datapoint.UNKNOWN_ROOM_ID,
			Datapoint.UNKNOWN_ROOM_ID);
	
	protected final Map<String, TimeseriesSetProcessor> knownProcessors = new HashMap<>();
	public TimeseriesSetProcessor getProcessor(String procID) {
		return knownProcessors.get(procID);
	}
	
	//protected TimeSeriesNameProvider nameProvider() {return null;}
	//protected abstract AggregationMode getMode(String tsLabel);
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
			protected List<SampledValue> calculateValues(ReadOnlyTimeSeries timeSeries, long start, long end,
					AggregationMode mode) {
				List<SampledValue> result = TimeSeriesServlet.getDayValues(timeSeries, start, end, mode, 1.0f);
				return result;
			}
		};
		knownProcessors.put(PER_DAY_EVAL, dayProc);
		
		TimeseriesSetProcessor sumProc = new TimeseriesSetProcessor() {
			
			@Override
			public List<Datapoint> getResultSeries(List<Datapoint> input, DatapointService dpService) {
TimeProcUtil.printTimeSeriesSet(input, "IN(0):Dayproc", 1, null, null);
				List<Datapoint> result1 = dayProc.getResultSeries(input, dpService);
				TimeseriesSetProcessor sumProc = new TimeseriesSetProcSum("total_sum") {
					@Override
					protected void debugCalculationResult(List<Datapoint> input, List<SampledValue> resultLoc) {
						TimeProcUtil.printTimeSeriesSet(input, "--RT-OUT/IN(2):Dayproc", 1, null, null);
						TimeProcUtil.printFirstElements(resultLoc, "--RT-OUT(1):Total_Sum");
					}
				};
				List<Datapoint> result = sumProc.getResultSeries(result1, dpService);
				return result;
			}
		};
		knownProcessors.put(SUM_PER_DAY_EVAL, sumProc);

		
		TimeseriesSetProcessor dayPerRoomProc = new TimeseriesSetProcessor() {
			
			@Override
			public List<Datapoint> getResultSeries(List<Datapoint> input, DatapointService dpService) {
				List<Datapoint> result1 = dayProc.getResultSeries(input, dpService);
				List<Datapoint> result = new ArrayList<>();
				// RoomID -> Timeseries in the room
				Map<String, List<Datapoint>> sortedbyRoom = new HashMap<>();
				for(Datapoint tsd: result1) {
					Datapoint dp = dpService.getDataPointAsIs(tsd.getLocation());
					String label;
					if(dp.getRoom() != null)
						label = dp.getRoom().label(null);
					else
						label = "noRoom";
					List<Datapoint> roomList = sortedbyRoom.get(label);
					if(roomList == null) {
						roomList = new ArrayList<>();
						sortedbyRoom.put(label, roomList);
					}
					roomList.add(tsd);
				}
				for(Entry<String, List<Datapoint>> roomData: sortedbyRoom.entrySet()) {
					TimeseriesSetProcessor sumProc = new TimeseriesSetProcSum(roomData.getKey()+"_sum");
					List<Datapoint> resultLoc = sumProc.getResultSeries(roomData.getValue(), dpService);
					if(!roomData.getValue().isEmpty()) {
						DPRoom room = roomData.getValue().get(0).getRoom();
						for(Datapoint dpLoc: resultLoc) {
							if(room != null)
								dpLoc.setRoom(room);
							else
								dpLoc.setRoom(unknownRoom);
						}
					}
					result.addAll(resultLoc);
				}
				return result;
			}
		};

		knownProcessors.put(SUM_PER_DAY_PER_ROOM_EVAL, dayPerRoomProc);
	}
	public List<Datapoint> process(String tsProcessRequest, List<Datapoint> input) {
		TimeseriesSetProcessor proc = knownProcessors.get(tsProcessRequest);
		if(proc == null)
			throw new IllegalArgumentException("Unknown timeseries processor: "+tsProcessRequest);
		return proc.getResultSeries(input, dpService);
	}
	
	public List<TimeSeriesData> processTSD(String tsProcessRequest, List<TimeSeriesData> input,
			TimeSeriesNameProvider nameProvider, AggregationModeProvider aggProv) {
		return DPUtil.getTSList(process(tsProcessRequest, DPUtil.getDPList(input, nameProvider, aggProv)), null);
	}
}
