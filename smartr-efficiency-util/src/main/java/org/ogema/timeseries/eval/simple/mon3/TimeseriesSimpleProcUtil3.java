package org.ogema.timeseries.eval.simple.mon3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.ogema.core.application.ApplicationManager;
import org.ogema.core.channelmanager.measurements.SampledValue;
import org.ogema.core.timeseries.ReadOnlyTimeSeries;
import org.ogema.devicefinder.api.DPRoom;
import org.ogema.devicefinder.api.Datapoint;
import org.ogema.devicefinder.api.DatapointInfo.AggregationMode;
import org.ogema.devicefinder.api.DatapointService;
import org.ogema.devicefinder.api.DpUpdateAPI.DpUpdated;
import org.ogema.timeseries.eval.simple.api.ProcessedReadOnlyTimeSeries3;
import org.ogema.timeseries.eval.simple.api.TimeProcPrint;
import org.ogema.timeseries.eval.simple.api.TimeProcUtil;
import org.ogema.timeseries.eval.simple.api.TimeProcUtil.MeterReference;
import org.ogema.timeseries.eval.simple.mon.TimeSeriesServlet;
import org.smartrplace.tsproc.persist.TsProcPersistUtil;

import de.iwes.util.timer.AbsoluteTimeHelper;
import de.iwes.util.timer.AbsoluteTiming;

public class TimeseriesSimpleProcUtil3 extends TimeseriesSimpleProcUtilBase3 { 
	public static final int DEFAULT_UPDATE_MODE = 4;
	
	/*public TimeseriesSimpleProcUtil3(ApplicationManager appMan, DatapointService dpService) {
		this(appMan, dpService, DEFAULT_UPDATE_MODE);
	}
	
	public TimeseriesSimpleProcUtil3(ApplicationManager appMan, DatapointService dpService, int updateMode) {
		this(appMan, dpService, updateMode, null);
	}*/

	/** Setup new instance for creating timeseries
	 * 
	 * @param appMan
	 * @param dpService
	 * @param updateMode see {@link TimeseriesSetProcMultiToSingle3#updateMode}:
	 * 	 Update mode regarding interval propagation and regarding singleInput.<br>
	 * 	 Note that from mode 2 on any change in the input data triggers a recalculation of the output data<br>
	 * 
	 *  0: Do not generate a result time series if input is empty, no updates
	 *  1: Update only at the end
	 *  2: Update exactly for any input change interval
	 *  3: Update for any input change onwards completely
	 *  4: Update completely if any input has a change 
	 */
	public TimeseriesSimpleProcUtil3(ApplicationManager appMan, DatapointService dpService,
			int updateMode, long minIntervalForReCalc) {
		super(appMan, dpService);
		TsProcPersistUtil.registerTsProcUtil(this, dpService);
		
		TimeseriesSetProcessor3 meterProc = new TimeseriesSetProcSingleToSingle3("_vm", null, minIntervalForReCalc,
				TimeseriesSimpleProcUtil3.this) {
			
			@Override
			protected List<SampledValue> calculateValues(ReadOnlyTimeSeries timeSeries, long start, long end,
					AggregationMode mode, ProcessedReadOnlyTimeSeries3 newTs2) {
				MeterReference ref = TimeSeriesServlet.getDefaultMeteringReference(timeSeries, start, appMan);
				return TimeSeriesServlet.getMeterFromConsumption(timeSeries, start, end, ref, mode);						
			}

			@Override
			protected void alignUpdateIntervalFromSource(DpUpdated updateInterval) {}
		};
		knownProcessors3.put(TimeProcUtil.METER_EVAL, meterProc);
		
		TimeseriesSetProcessor3 dayProc = new TimeseriesSetProcSingleToSingle3("_proTag", AbsoluteTiming.DAY, minIntervalForReCalc,
				TimeseriesSimpleProcUtil3.this) {
			
			@Override
			protected List<SampledValue> calculateValues(ReadOnlyTimeSeries timeSeries, long start, long end,
					AggregationMode mode, ProcessedReadOnlyTimeSeries3 newTs2) {
				List<SampledValue> result = TimeSeriesServlet.getDayValues(timeSeries, start, end, mode,
						newTs2.getInputDp()!=null?newTs2.getInputDp().getScale():null);
				return result;
			}

			@Override
			protected void alignUpdateIntervalFromSource(DpUpdated updateInterval) {
				updateInterval.start = AbsoluteTimeHelper.getIntervalStart(updateInterval.start, AbsoluteTiming.DAY);
				updateInterval.end = AbsoluteTimeHelper.getNextStepTime(updateInterval.end, AbsoluteTiming.DAY)-1;				
			}
		};
		knownProcessors3.put(TimeProcUtil.PER_DAY_EVAL, dayProc);
		
		TimeseriesSetProcessor3 hourProc = new TimeseriesSetProcSingleToSingle3("_proStunde", AbsoluteTiming.HOUR, minIntervalForReCalc,
				TimeseriesSimpleProcUtil3.this) {
			
			@Override
			protected List<SampledValue> calculateValues(ReadOnlyTimeSeries timeSeries, long start, long end,
					AggregationMode mode, ProcessedReadOnlyTimeSeries3 newTs2) {
				List<SampledValue> result = TimeSeriesServlet.getDayValues(timeSeries, start, end, mode,
						newTs2.getInputDp()!=null?newTs2.getInputDp().getScale():null, false, AbsoluteTiming.HOUR);
				return result;
			}

			@Override
			protected void alignUpdateIntervalFromSource(DpUpdated updateInterval) {
				updateInterval.start = AbsoluteTimeHelper.getIntervalStart(updateInterval.start, AbsoluteTiming.HOUR);
				updateInterval.end = AbsoluteTimeHelper.getNextStepTime(updateInterval.end, AbsoluteTiming.HOUR)-1;				
			}
		};
		knownProcessors3.put(TimeProcUtil.PER_HOUR_EVAL, hourProc);
		
		TimeseriesSetProcessor3 min15Proc = new TimeseriesSetProcSingleToSingle3("_per15min", AbsoluteTiming.FIFTEEN_MINUTE, minIntervalForReCalc,
				TimeseriesSimpleProcUtil3.this) {
			
			@Override
			protected List<SampledValue> calculateValues(ReadOnlyTimeSeries timeSeries, long start, long end,
					AggregationMode mode, ProcessedReadOnlyTimeSeries3 newTs2) {
				List<SampledValue> result = TimeSeriesServlet.getDayValues(timeSeries, start, end, mode,
						newTs2.getInputDp()!=null?newTs2.getInputDp().getScale():null, false, AbsoluteTiming.FIFTEEN_MINUTE);
				return result;
			}

			@Override
			protected void alignUpdateIntervalFromSource(DpUpdated updateInterval) {
				updateInterval.start = AbsoluteTimeHelper.getIntervalStart(updateInterval.start, AbsoluteTiming.FIFTEEN_MINUTE);
				updateInterval.end = AbsoluteTimeHelper.getNextStepTime(updateInterval.end, AbsoluteTiming.FIFTEEN_MINUTE)-1;				
			}
		};
		knownProcessors3.put(TimeProcUtil.PER_15M_EVAL, min15Proc);

		TimeseriesSetProcessor3 minuteProc = new TimeseriesSetProcSingleToSingle3(TimeProcUtil.PER_MINUTE_SUFFIX,
				AbsoluteTiming.FIFTEEN_MINUTE, minIntervalForReCalc, TimeseriesSimpleProcUtil3.this) {
			
			@Override
			protected List<SampledValue> calculateValues(ReadOnlyTimeSeries timeSeries, long start, long end,
					AggregationMode mode, ProcessedReadOnlyTimeSeries3 newTs2) {
				List<SampledValue> result = TimeSeriesServlet.getDayValues(timeSeries, start, end, mode,
						newTs2.getInputDp()!=null?newTs2.getInputDp().getScale():null, false, AbsoluteTiming.MINUTE);
				return result;
			}

			@Override
			protected void alignUpdateIntervalFromSource(DpUpdated updateInterval) {
				updateInterval.start = AbsoluteTimeHelper.getIntervalStart(updateInterval.start, AbsoluteTiming.MINUTE);
				updateInterval.end = AbsoluteTimeHelper.getNextStepTime(updateInterval.end, AbsoluteTiming.MINUTE)-1;				
			}
		};
		knownProcessors3.put(TimeProcUtil.PER_MINUTE_EVAL, minuteProc);
		
		TimeseriesSetProcessor3 monthProc = new TimeseriesSetProcSingleToSingle3("_perMonth", AbsoluteTiming.MONTH, minIntervalForReCalc,
				TimeseriesSimpleProcUtil3.this) {
			
			@Override
			protected List<SampledValue> calculateValues(ReadOnlyTimeSeries timeSeries, long start, long end,
					AggregationMode mode, ProcessedReadOnlyTimeSeries3 newTs2) {
				List<SampledValue> result = TimeSeriesServlet.getDayValues(timeSeries, start, end, mode,
						newTs2.getInputDp()!=null?newTs2.getInputDp().getScale():null, false, AbsoluteTiming.MONTH);
				return result;
			}

			@Override
			protected void alignUpdateIntervalFromSource(DpUpdated updateInterval) {
				updateInterval.start = AbsoluteTimeHelper.getIntervalStart(updateInterval.start, AbsoluteTiming.MONTH);
				updateInterval.end = AbsoluteTimeHelper.getNextStepTime(updateInterval.end, AbsoluteTiming.MONTH)-1;				
			}
		};
		knownProcessors3.put(TimeProcUtil.PER_MONTH_EVAL, monthProc);
		
		TimeseriesSetProcessor3 yearProc = new TimeseriesSetProcSingleToSingle3("_perYear", AbsoluteTiming.YEAR, minIntervalForReCalc,
				TimeseriesSimpleProcUtil3.this) {
			
			@Override
			protected List<SampledValue> calculateValues(ReadOnlyTimeSeries timeSeries, long start, long end,
					AggregationMode mode, ProcessedReadOnlyTimeSeries3 newTs2) {
				List<SampledValue> result = TimeSeriesServlet.getDayValues(timeSeries, start, end, mode,
						newTs2.getInputDp()!=null?newTs2.getInputDp().getScale():null, false, AbsoluteTiming.YEAR);
				return result;
			}
			
			@Override
			protected void alignUpdateIntervalFromSource(DpUpdated updateInterval) {
				updateInterval.start = AbsoluteTimeHelper.getIntervalStart(updateInterval.start, AbsoluteTiming.YEAR);
				updateInterval.end = AbsoluteTimeHelper.getNextStepTime(updateInterval.end, AbsoluteTiming.YEAR)-1;				
			}
		};
		knownProcessors3.put(TimeProcUtil.PER_YEAR_EVAL, yearProc);

		TimeseriesSetProcessor3 sumProc = new TimeseriesSetProcessor3() {
			
			@Override
			public List<Datapoint> getResultSeries(List<Datapoint> input, boolean registersTimedJob, DatapointService dpService) {
if(Boolean.getBoolean("evaldebug")) TimeProcPrint.printTimeSeriesSet(input, "IN(0):Dayproc", 1, null, null);
				List<Datapoint> result1 = dayProc.getResultSeries(input, false, dpService);
				generatedTss.addAll(result1);
				TimeseriesSetProcSum3 sumProc = new TimeseriesSetProcSum3("total_sum", AbsoluteTiming.DAY,
						(updateMode>0)?AbsoluteTiming.DAY:null, minIntervalForReCalc, updateMode, TimeseriesSimpleProcUtil3.this) {
					@Override
					protected void debugCalculationResult(List<Datapoint> input, List<SampledValue> resultLoc) {
						if(Boolean.getBoolean("evaldebug")) TimeProcPrint.printTimeSeriesSet(input, "--RT-OUT/IN(2):Dayproc", 1, null, null);
						if(Boolean.getBoolean("evaldebug")) TimeProcPrint.printFirstElements(resultLoc, "--RT-OUT(1):Total_Sum");
					}

				};
				//sumProc.updateMode = updateMode;
				List<Datapoint> result = sumProc.getResultSeries(result1, registersTimedJob, dpService);
				return result;
			}
		};
		knownProcessors3.put(TimeProcUtil.SUM_PER_DAY_EVAL, sumProc);

		TimeseriesSetProcessor3 sumProcHour = new TimeseriesSetProcessor3() {
			
			@Override
			public List<Datapoint> getResultSeries(List<Datapoint> input, boolean registersTimedJob, DatapointService dpService) {
if(Boolean.getBoolean("evaldebug")) TimeProcPrint.printTimeSeriesSet(input, "IN(0):Hourproc", 1, null, null);
				List<Datapoint> result1 = hourProc.getResultSeries(input, false, dpService);
				generatedTss.addAll(result1);
				TimeseriesSetProcSum3 sumProc = new TimeseriesSetProcSum3("total_sum_hour", AbsoluteTiming.HOUR,
						(updateMode>0)?AbsoluteTiming.HOUR:null, minIntervalForReCalc, updateMode, TimeseriesSimpleProcUtil3.this) {
					@Override
					protected void debugCalculationResult(List<Datapoint> input, List<SampledValue> resultLoc) {
						if(Boolean.getBoolean("evaldebug")) TimeProcPrint.printTimeSeriesSet(input, "--RT-OUT/IN(2):Hourproc", 1, null, null);
						if(Boolean.getBoolean("evaldebug")) TimeProcPrint.printFirstElements(resultLoc, "--RT-OUT(1):Total_Sum_hour");
					}
				};
				List<Datapoint> result = sumProc.getResultSeries(result1, registersTimedJob, dpService);
				//sumProc.setUpdateMode(updateMode);
				return result;
			}
		};
		knownProcessors3.put(TimeProcUtil.SUM_PER_HOUR_EVAL, sumProcHour);

		TimeseriesSetProcessor3 sumProc15Min = new TimeseriesSetProcessor3() {
			
			@Override
			public List<Datapoint> getResultSeries(List<Datapoint> input, boolean registersTimedJob, DatapointService dpService) {
if(Boolean.getBoolean("evaldebug")) TimeProcPrint.printTimeSeriesSet(input, "IN(0):15Minproc", 1, null, null);
				List<Datapoint> result1 = min15Proc.getResultSeries(input, false, dpService);
				generatedTss.addAll(result1);
				TimeseriesSetProcSum3 sumProc = new TimeseriesSetProcSum3("total_sum_15min", AbsoluteTiming.FIFTEEN_MINUTE,
						(updateMode>0)?AbsoluteTiming.FIFTEEN_MINUTE:null, minIntervalForReCalc, updateMode, TimeseriesSimpleProcUtil3.this) {
					@Override
					protected void debugCalculationResult(List<Datapoint> input, List<SampledValue> resultLoc) {
						if(Boolean.getBoolean("evaldebug")) TimeProcPrint.printTimeSeriesSet(input, "--RT-OUT/IN(2):15Minproc", 1, null, null);
						if(Boolean.getBoolean("evaldebug")) TimeProcPrint.printFirstElements(resultLoc, "--RT-OUT(1):Total_Sum_15min");
					}
				};
				List<Datapoint> result = sumProc.getResultSeries(result1, registersTimedJob, dpService);
				//sumProc.updateMode = updateMode;
				return result;
			}
		};
		knownProcessors3.put(TimeProcUtil.SUM_PER_15M_EVAL, sumProc15Min);
		
		TimeseriesSetProcessor3 sumProcMinute = new TimeseriesSetProcessor3() {
			
			@Override
			public List<Datapoint> getResultSeries(List<Datapoint> input, boolean registersTimedJob, DatapointService dpService) {
if(Boolean.getBoolean("evaldebug")) TimeProcPrint.printTimeSeriesSet(input, "IN(0):MinuteProc", 1, null, null);
				List<Datapoint> result1 = minuteProc.getResultSeries(input, false, dpService);
				generatedTss.addAll(result1);
				TimeseriesSetProcSum3 sumProc = new TimeseriesSetProcSum3("total_sum_minute", AbsoluteTiming.MINUTE,
						(updateMode>0)?AbsoluteTiming.MINUTE:null, minIntervalForReCalc, updateMode, TimeseriesSimpleProcUtil3.this) {
					@Override
					protected void debugCalculationResult(List<Datapoint> input, List<SampledValue> resultLoc) {
						if(Boolean.getBoolean("evaldebug")) TimeProcPrint.printTimeSeriesSet(input, "--RT-OUT/IN(2):MinuteProc", 1, null, null);
						if(Boolean.getBoolean("evaldebug")) TimeProcPrint.printFirstElements(resultLoc, "--RT-OUT(1):Total_Sum_minute");
					}
				};
				List<Datapoint> result = sumProc.getResultSeries(result1, registersTimedJob, dpService);
				//sumProc.updateMode = updateMode;
				return result;
			}
		};
		knownProcessors3.put(TimeProcUtil.SUM_PER_MINUTE_EVAL, sumProcMinute);

		TimeseriesSetProcessor3 sumProcMonth = new TimeseriesSetProcessor3() {
			
			@Override
			public List<Datapoint> getResultSeries(List<Datapoint> input, boolean registersTimedJob, DatapointService dpService) {
if(Boolean.getBoolean("evaldebug")) TimeProcPrint.printTimeSeriesSet(input, "IN(0):Monthproc", 1, null, null);
				List<Datapoint> result1 = monthProc.getResultSeries(input, false, dpService);
				generatedTss.addAll(result1);
				TimeseriesSetProcSum3 sumProc = new TimeseriesSetProcSum3("total_sum_month", AbsoluteTiming.MONTH,
						(updateMode>0)?AbsoluteTiming.MONTH:null, minIntervalForReCalc, updateMode,
								TimeseriesSimpleProcUtil3.this) {
					@Override
					protected void debugCalculationResult(List<Datapoint> input, List<SampledValue> resultLoc) {
						if(Boolean.getBoolean("evaldebug")) TimeProcPrint.printTimeSeriesSet(input, "--RT-OUT/IN(2):Monthproc", 1, null, null);
						if(Boolean.getBoolean("evaldebug")) TimeProcPrint.printFirstElements(resultLoc, "--RT-OUT(1):Total_Sum_month");
					}
				};
				List<Datapoint> result = sumProc.getResultSeries(result1, registersTimedJob, dpService);
				//sumProc.updateMode = updateMode;
				return result;
			}
		};
		knownProcessors3.put(TimeProcUtil.SUM_PER_MONTH_EVAL, sumProcMonth);

		TimeseriesSetProcessor3 sumProcYear = new TimeseriesSetProcessor3() {
			
			@Override
			public List<Datapoint> getResultSeries(List<Datapoint> input, boolean registersTimedJob, DatapointService dpService) {
if(Boolean.getBoolean("evaldebug")) TimeProcPrint.printTimeSeriesSet(input, "IN(0):Yearproc", 1, null, null);
				List<Datapoint> result1 = yearProc.getResultSeries(input, false, dpService);
				generatedTss.addAll(result1);
				TimeseriesSetProcSum3 sumProc = new TimeseriesSetProcSum3("total_sum_year", AbsoluteTiming.YEAR,
						(updateMode>0)?AbsoluteTiming.YEAR:null, minIntervalForReCalc, updateMode, TimeseriesSimpleProcUtil3.this) {
					@Override
					protected void debugCalculationResult(List<Datapoint> input, List<SampledValue> resultLoc) {
						if(Boolean.getBoolean("evaldebug")) TimeProcPrint.printTimeSeriesSet(input, "--RT-OUT/IN(2):Yearproc", 1, null, null);
						if(Boolean.getBoolean("evaldebug")) TimeProcPrint.printFirstElements(resultLoc, "--RT-OUT(1):Total_Sum_year");
					}
				};
				List<Datapoint> result = sumProc.getResultSeries(result1, registersTimedJob, dpService);
				//sumProc.updateMode = updateMode;
				return result;
			}
		};
		knownProcessors3.put(TimeProcUtil.SUM_PER_YEAR_EVAL, sumProcYear);

		/* TODO: This might not really work yet für PS3!*/
		TimeseriesSetProcessor3 dayPerRoomProc = new TimeseriesSetProcessor3() {
			
			@Override
			public List<Datapoint> getResultSeries(List<Datapoint> input, boolean registersTimedJob, DatapointService dpService) {
				List<Datapoint> result1 = dayProc.getResultSeries(input, false, dpService);
				generatedTss.addAll(result1);
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
					TimeseriesSetProcSum3 sumProc = new TimeseriesSetProcSum3(roomData.getKey()+"_sum", AbsoluteTiming.DAY,
							(updateMode>0)?AbsoluteTiming.DAY:null, minIntervalForReCalc, updateMode, TimeseriesSimpleProcUtil3.this);
					//sumProc.updateMode = updateMode;
					List<Datapoint> resultLoc = sumProc.getResultSeries(roomData.getValue(), registersTimedJob, dpService);
					if(!roomData.getValue().isEmpty()) {
						DPRoom room = roomData.getValue().get(0).getRoom();
						for(Datapoint dpLoc: resultLoc) {
							if(room != null)
								dpLoc.setRoom(room);
							else
								dpLoc.setRoom(TimeProcUtil.unknownRoom);
						}
					}
					result.addAll(resultLoc);
				}
				return result;
			}
		};

		knownProcessors3.put(TimeProcUtil.SUM_PER_DAY_PER_ROOM_EVAL, dayPerRoomProc);
	}
}
