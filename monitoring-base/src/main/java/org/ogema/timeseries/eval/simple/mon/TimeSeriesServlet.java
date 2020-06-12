package org.ogema.timeseries.eval.simple.mon;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.ogema.core.application.ApplicationManager;
import org.ogema.core.channelmanager.measurements.FloatValue;
import org.ogema.core.channelmanager.measurements.Quality;
import org.ogema.core.channelmanager.measurements.SampledValue;
import org.ogema.core.model.schedule.Schedule;
import org.ogema.core.model.simple.SingleValueResource;
import org.ogema.core.recordeddata.RecordedData;
import org.ogema.core.timeseries.ReadOnlyTimeSeries;
import org.ogema.devicefinder.api.DatapointInfo.AggregationMode;
import org.ogema.timeseries.eval.simple.api.TimeProcUtil;
import org.ogema.timeseries.eval.simple.api.TimeProcUtil.MeterReference;
import org.ogema.tools.resource.util.ResourceUtils;
import org.ogema.tools.timeseries.v2.tools.TimeSeriesUtils;
import org.smartrplace.util.frontend.servlet.ServletNumProvider;
import org.smartrplace.util.frontend.servlet.UserServlet.ServletPageProvider;
import org.smartrplace.util.frontend.servlet.UserServlet.ServletValueProvider;

import de.iwes.timeseries.eval.base.provider.utils.TimeSeriesDataImpl;
import de.iwes.util.resource.ValueResourceHelper;
import de.iwes.util.timer.AbsoluteTimeHelper;
import de.iwes.util.timer.AbsoluteTiming;

/** Note: This servlet is currently foreseen to be registered under the path of an implementing application. The
 * {@link TimeseriesBaseServlet} is registered under the path of the monitoringApp.
 * TODO: It could make sense to offer also this servlet under the path of the monitoringApp.
 *
 */
public class TimeSeriesServlet implements ServletPageProvider<TimeSeriesDataImpl> {
	public static final long ACCEPTED_PREVIOUS_VALUE_DISTANCE_FOR_DAY_EVAL = TimeProcUtil.HOUR_MILLIS*12;
	
	Map<String, ReadOnlyTimeSeries> knownSpecialTs = new HashMap<>();
	protected final ApplicationManager appMan;
	
	public TimeSeriesServlet(ApplicationManager appMan) {
		this.appMan = appMan;
	}

	@Override
	public Map<String, ServletValueProvider> getProviders(TimeSeriesDataImpl object, String user,
			Map<String, String[]> paramMap) {
		Map<String, ServletValueProvider> result = new LinkedHashMap<>();
		float val = specialEvaluation(object.label(null), object.getTimeSeries(), appMan, paramMap);
		ServletValueProvider last24h = new ServletNumProvider(val);
		result.put("last24h", last24h);
		/*if(object.label(null).equals("L24")) {
			float val = getDiffOfLast24h(object.getTimeSeries(), appMan);
			ServletValueProvider last24h = new ServletNumProvider(val);
			result.put("last24h", last24h);
		} else if(object.label(null).equals("I24")) {
			float val = getIntegralOfLast24h(object.getTimeSeries(), appMan);
			ServletValueProvider last24h = new ServletNumProvider(val);
			result.put("last24h", last24h);
		}*/

		return result;
	}

	@Override
	public Collection<TimeSeriesDataImpl> getAllObjects(String user) {
		List<TimeSeriesDataImpl> result = new ArrayList<TimeSeriesDataImpl>();
		for(ReadOnlyTimeSeries know: knownSpecialTs.values()) {
			result.add(new TimeSeriesDataImpl(know, "XXX_", "XXX_", null));
		}
		return result;
	}

	@Override
	public String getObjectId(TimeSeriesDataImpl objIn) {
		ReadOnlyTimeSeries obj = objIn.getTimeSeries();
		if(obj instanceof Schedule)
			return objIn.label(null)+"_S:"+ResourceUtils.getHumanReadableShortName((Schedule)obj);
		else if(obj instanceof RecordedData)
			return objIn.label(null)+"_R:"+((RecordedData)obj).getPath();
		else {
			for(Entry<String, ReadOnlyTimeSeries> e: knownSpecialTs.entrySet()) {
				if(e.getValue() == obj)
					return objIn.label(null)+"_X:"+e.getKey();
			}
		}
		return null;
	}
	
	@Override
	public TimeSeriesDataImpl getObject(String objectIdin) {
		String label = objectIdin.substring(0, 3);
		String objectId = objectIdin.substring(4);
		if(objectId.startsWith("S:"))
			return new TimeSeriesDataImpl(appMan.getResourceAccess().getResource(objectId.substring(2)), label, label, null);
		else if(objectId.startsWith("R:")) {
			SingleValueResource svr = appMan.getResourceAccess().getResource(objectId.substring(2));
			return new TimeSeriesDataImpl(ValueResourceHelper.getRecordedData(svr), label, label, null);
		} else if(objectId.startsWith("X:"))
			return new TimeSeriesDataImpl(knownSpecialTs.get(objectId), label, label, null);
		//use L24_R: as default
		SingleValueResource svr = appMan.getResourceAccess().getResource(objectIdin);
		return new TimeSeriesDataImpl(ValueResourceHelper.getRecordedData(svr), "L24", "L24", null);		
		//hrow new IllegalArgumentException("ObjectId must declare a known type (S:,R: or X:), is:"+objectId);
	}
	
	public static float getIntegralOfLast24h(ReadOnlyTimeSeries ts, ApplicationManager appMan) {
		long now = appMan.getFrameworkTime();
		return (float) (TimeSeriesUtils.integrate(ts, now-TimeProcUtil.DAY_MILLIS, now)/TimeProcUtil.HOUR_MILLIS);
	}
	/** This method is only applicable for AggregationMode.Meter2Meter*/
	public static float getDiffOfLast24h(ReadOnlyTimeSeries ts, ApplicationManager appMan) {
		return getDiffOfLast24h(ts, Boolean.getBoolean("org.smartrplace.app.monbase.dointerpolate"), appMan);
	}
	/** This method is only applicable for AggregationMode.Meter2Meter*/
	public static float getDiffOfLast24h(ReadOnlyTimeSeries ts, boolean interpolate, ApplicationManager appMan) {
		long now = appMan.getFrameworkTime();
		long start = now-TimeProcUtil.DAY_MILLIS;
		SampledValue startval = ts.getPreviousValue(start);
		if(startval == null || start - startval.getTimestamp() > ACCEPTED_PREVIOUS_VALUE_DISTANCE_FOR_DAY_EVAL) {
			return -1;
		}
		SampledValue endval = ts.getPreviousValue(now);
		if(endval == null)
			return -1;
			//return Float.NaN;
		try {
		return endval.getValue().getFloatValue() - startval.getValue().getFloatValue();
		} catch(NullPointerException e) {
			e.printStackTrace();
			return -2;
		}
	}
	
	/** This method is only applicable for AggregationMode.Meter2Meter*/
	public static float getDiffForDay(long timeStamp, ReadOnlyTimeSeries ts) {
		return getDiffForDay(timeStamp, ts, Boolean.getBoolean("org.smartrplace.app.monbase.dointerpolate"));
	}
	/** This method is only applicable for AggregationMode.Meter2Meter*/
	public static float getDiffForDay(long timeStamp, ReadOnlyTimeSeries ts,
			boolean interpolate) {
		long start = AbsoluteTimeHelper.getIntervalStart(timeStamp, AbsoluteTiming.DAY);
		long end = start + TimeProcUtil.DAY_MILLIS;
		final float startFloat;
		final float endFloat;
		if(interpolate) {
			startFloat = getInterpolatedValue(ts, start);
			endFloat = getInterpolatedValue(ts, end);			
		} else {
			SampledValue startval = ts.getPreviousValue(start);
			if(startval == null || (start - startval.getTimestamp() > ACCEPTED_PREVIOUS_VALUE_DISTANCE_FOR_DAY_EVAL)) {
				return Float.NaN; //-1
			}
			SampledValue endval = ts.getPreviousValue(end);
			if(endval == null)
				return Float.NaN;
				//return Float.NaN;
			try {
				startFloat = startval.getValue().getFloatValue();
				endFloat = endval.getValue().getFloatValue();
			} catch(NullPointerException e) {
				e.printStackTrace();
				return -2;
			}
		}
		return endFloat - startFloat;
	}
	/** This method is only applicable for AggregationMode.Meter2Meter*/
	public static float getDiffForDayOrLast24(long timeStamp, ReadOnlyTimeSeries ts,
			ApplicationManager appMan) {
		return getDiffForDayOrLast24(timeStamp, ts, Boolean.getBoolean("org.smartrplace.app.monbase.dointerpolate"), appMan);
	}
	/** This method is only applicable for AggregationMode.Meter2Meter*/
	public static float getDiffForDayOrLast24(long timeStamp, ReadOnlyTimeSeries ts,
			boolean interpolate, ApplicationManager appMan) {
		long now = appMan.getFrameworkTime();
		long start = AbsoluteTimeHelper.getIntervalStart(timeStamp, AbsoluteTiming.DAY);		
		long end = start + TimeProcUtil.DAY_MILLIS;
		if(now>=start && now<=end)
			return getDiffOfLast24h(ts, interpolate, appMan);
		else
			return getDiffForDay(timeStamp, ts, interpolate);
	}
	
	public static float specialEvaluation(String label, ReadOnlyTimeSeries timeSeries, ApplicationManager appMan,
			Map<String, String[]> paramMap) {
		if(label.equals("L24")) {
			String[] timearr = paramMap.get("time");
			long ts;
			if(timearr == null)
				ts = appMan.getFrameworkTime();
			else
				ts = Long.parseLong(timearr[0]);
			return getDiffForDayOrLast24(ts, timeSeries, appMan);
		} else	if(label.equals("D24")) {
			long ts = Long.parseLong(paramMap.get("time")[0]);
			return getDiffForDay(ts, timeSeries);
		} else if(label.equals("I24")) {
			return getIntegralOfLast24h(timeSeries, appMan);
		}
		return Float.NaN;
	}
	
	public static class Power2MeterPrevValues {
		Float value;
		long timestamp;
	}
	/** Calculate a virtual meter series that has the same counter value at a reference point as another
	 * real meter so that the further consumption trend can be compared directly
	 * Note: Currently this method only supports interpolation
	 * TODO: param resultSerie especially for mode Power2Meter we would like to get a reference on a preexisting result
	 * 		series to avoid a recalculation all the time. For now we re-calculate every time. If the value of the reference
	 * 		time changes existing data is still not recalculated. The datapoint service app has to be restarted for this.
	 * TODO: For Power2Meter we should generate a warning regarding value gaps, for now we just use the last value for the entire
	 * 		interval until the next value is available.
	 * @return
	 */
	public static List<SampledValue> getMeterFromConsumption(ReadOnlyTimeSeries timeSeries, long start, long end,
			MeterReference ref, AggregationMode mode) {
			//ReadOnlyTimeSeries resultSeries) {
		List<SampledValue> result = new ArrayList<>();
		final double myRefValue;
		final double delta;
		Power2MeterPrevValues prevVal = null;
		if(mode == AggregationMode.Power2Meter)
			prevVal = new Power2MeterPrevValues();
		if(mode == AggregationMode.Consumption2Meter || mode == AggregationMode.Power2Meter) {
			long startLoc;
			long endLoc;
			if(ref.referenceTime > start) {
				startLoc = start;
				endLoc = ref.referenceTime;
			} else {
				startLoc = ref.referenceTime;
				endLoc = start;			
			}
			double counter = aggregateValuesForMeter(timeSeries, mode, startLoc, endLoc, prevVal, null, 0);
			/*if(mode == AggregationMode.Consumption2Meter)
				counter = getPartialConsumptionValue(timeSeries, startLoc, true);
			else
				counter = 0;
			final List<SampledValue> svList;
			svList = timeSeries.getValues(startLoc, endLoc);
			for(SampledValue sv: svList) {
				if(mode == AggregationMode.Power2Meter)
					counter += getPowerStep(prevVal, sv, startLoc);
				else
					counter += sv.getValue().getFloatValue();
			}
			if(mode == AggregationMode.Power2Meter)
				counter += getFinalPowerStep(prevVal, endLoc);
			else
				counter += getPartialConsumptionValue(timeSeries, endLoc, false);*/
			if(ref.referenceTime > start)
				myRefValue = counter;
			else
				myRefValue = -counter;
			delta = ref.referenceMeterValue - myRefValue;
			aggregateValuesForMeter(timeSeries, mode, start, end, prevVal, result, delta);
			return result;
		} else {
			myRefValue = getInterpolatedValue(timeSeries, ref.referenceTime);
		}
		if(Double.isNaN(myRefValue))
			return Collections.emptyList();
		delta = ref.referenceMeterValue - myRefValue;
		double counter = 0;
		//if(mode == AggregationMode.Consumption2Meter) {
		//	counter = getPartialConsumptionValue(timeSeries, start, true);
		//}
		//Float prevValue = null;
		//long prevTime = -1;
		for(SampledValue sv: timeSeries.getValues(start, end)) {
			/*if(mode == AggregationMode.Consumption2Meter)
				counter += sv.getValue().getFloatValue();
			else if(mode == AggregationMode.Power2Meter) {
				if(prevValue != null) {
					counter += (prevValue * (sv.getTimestamp() - prevTime));
					prevTime = sv.getTimestamp();
				} else
					prevTime = start;
				prevValue = sv.getValue().getFloatValue();
			} else*/
			counter = sv.getValue().getFloatValue();
			result.add(new SampledValue(new FloatValue((float) (counter+delta)), sv.getTimestamp(), sv.getQuality()));
		}
		return result;
	}
	public static List<SampledValue> getDayValues(ReadOnlyTimeSeries timeSeries, long start, long end,
			AggregationMode mode, float factor) {
		return getDayValues(timeSeries, start, end, mode, factor,
				Boolean.getBoolean("org.smartrplace.app.monbase.dointerpolate"));
	}
	public static List<SampledValue> getDayValues(ReadOnlyTimeSeries timeSeries, long start, long end,
			AggregationMode mode, float factor, boolean interpolate) {
		//if(mode == AggregationMode.Power2Meter)
		//	throw new UnsupportedClassVersionError("Power2Meter not supported yet");
		long nextDayStart = AbsoluteTimeHelper.getIntervalStart(start, AbsoluteTiming.DAY);
		/*float prevCounter;
		switch(mode) {
		case Meter2Meter:
			prevCounter = getInterpolatedValue(timeSeries, startDay);
			break;
		default:
			prevCounter = 0;
		}*/
		List<SampledValue> result = new ArrayList<>();
		while(nextDayStart < end) {
			long startCurrentDay = nextDayStart;
			nextDayStart = AbsoluteTimeHelper.addIntervalsFromAlignedTime(nextDayStart, 1, AbsoluteTiming.DAY);
			float newDayVal;
			if(mode == AggregationMode.Meter2Meter) {
				newDayVal = getDiffForDay(startCurrentDay, timeSeries, interpolate);
			} else {
				float newCounter = getInterpolatedValue(timeSeries, startCurrentDay);
				switch(mode) {
				//case Meter2Meter:
				//	newDayVal = newCounter - prevCounter;
				//	prevCounter = newCounter;
				//	break;
				case Power2Meter:
					//TODO: not really tested
					newDayVal = (float) (TimeSeriesUtils.integrate(
							timeSeries, startCurrentDay, nextDayStart)/TimeProcUtil.HOUR_MILLIS);
					break;
				case Consumption2Meter:
					double counter = getPartialConsumptionValue(timeSeries, start, true);
					List<SampledValue> svList = timeSeries.getValues(startCurrentDay, nextDayStart);
					for(SampledValue sv: svList) {
						counter += sv.getValue().getFloatValue();
					}
					//TODO: correct start / end value usage
					//counter += getPartialConsumptionValue(timeSeries, end, false);
					newDayVal = (float) counter;
					break;
				default:
					newDayVal = newCounter;
				}
			}
			result.add(new SampledValue(new FloatValue(newDayVal), startCurrentDay, Quality.GOOD));
		}
		return result;
	}
	
	protected static double interpolateEnergyValue(long start, long end, long ts, float valStart, float valEnd) {
		return valStart+interpolateEnergyStep(start, end, ts, valEnd-valStart);
	}
	protected static double interpolateEnergyStep(long start, long end, long ts, float deltaVal) {
		return ((double)(ts-start))/(end-start)*deltaVal;
	}
	protected static float getInterpolatedValue(ReadOnlyTimeSeries timeseries, long timestamp) {
		SampledValue sv = timeseries.getValue(timestamp);
		if(sv != null)
			return sv.getValue().getFloatValue();
		SampledValue svBefore = timeseries.getPreviousValue(timestamp);
		SampledValue svNext = timeseries.getNextValue(timestamp);
		if(svBefore == null || svNext == null)
			return Float.NaN;
		return (float) interpolateEnergyValue(svBefore.getTimestamp(), svNext.getTimestamp(),
				timestamp,
				svBefore.getValue().getFloatValue(), svNext.getValue().getFloatValue());
	}
	
	/** For Consumption2Meter time series get energy consumption from timestamp to the end of the
	 * interval within timestamp or from the start of the interval until timestamp<br>
	 * Note that the interval is defined by the values available in the timeseries, so this is
	 * mainly intended for manual timeseries e.g. with one value per day
	 * @param timeseries
	 * @param timestamp
	 * @param getConsumptionTowardsEnd if true the energy consumption from timestamp to the end of
	 * the current interval is returned, otherwise the energy consumption from the start of the current
	 * interval until timestamp
	 * @return
	 */
	protected static float getPartialConsumptionValue(ReadOnlyTimeSeries timeseries, long timestamp,
			boolean getConsumptionTowardsEnd) {
		SampledValue sv = timeseries.getValue(timestamp);
		if(sv != null)
			return 0;
		SampledValue svBefore = timeseries.getPreviousValue(timestamp);
		SampledValue svNext = timeseries.getNextValue(timestamp);
		if(svBefore == null || svNext == null)
			return 0;
		// Part of consumption represented by the value at the end of the interval that is used until timestamp
		float partialVal = (float) interpolateEnergyStep(svBefore.getTimestamp(), svNext.getTimestamp(),
				timestamp,
				svNext.getValue().getFloatValue());
		if(getConsumptionTowardsEnd)
			return svNext.getValue().getFloatValue() - partialVal;
		else
			return partialVal;
	}
	
	/**
	 * 
	 * @param internalVals
	 * @param sv
	 * @param evalStart required for initial step. The first value will be integrated not from its timstamp, but
	 * 		from the start of the evaluation as the real value before the first value usually is not in the
	 * 		integration
	 * @return
	 */
	protected static float getPowerStep(Power2MeterPrevValues internalVals, SampledValue sv, long evalStart) {
		float result;
		if(internalVals.value != null) {
			result = (internalVals.value * (sv.getTimestamp() - internalVals.timestamp));
			internalVals.timestamp = sv.getTimestamp();
		} else {
			result = 0;
			internalVals.timestamp = evalStart;
		}
		internalVals.value = sv.getValue().getFloatValue();
		return result;
	}
	protected static float getFinalPowerStep(Power2MeterPrevValues internalVals, long evalEnd) {
		float result;
		if(internalVals.value != null) {
			result = (internalVals.value * (evalEnd - internalVals.timestamp));
		} else {
			result = 0;
		}
		return result;		
	}
	
	protected static double aggregateValuesForMeter(ReadOnlyTimeSeries timeSeries, AggregationMode mode,
			long startLoc, long endLoc, Power2MeterPrevValues prevVal,
			 List<SampledValue> result, double delta) {
		double counter;
		if(mode == AggregationMode.Consumption2Meter)
			counter = getPartialConsumptionValue(timeSeries, startLoc, true);
		else
			counter = 0;
		final List<SampledValue> svList;
		svList = timeSeries.getValues(startLoc, endLoc);
		for(SampledValue sv: svList) {
			if(mode == AggregationMode.Power2Meter)
				counter += getPowerStep(prevVal, sv, startLoc);
			else
				counter += sv.getValue().getFloatValue();
			if(result != null)
				result.add(new SampledValue(new FloatValue((float) (counter+delta)), sv.getTimestamp(), sv.getQuality()));
		}
		if(mode == AggregationMode.Power2Meter) {
			counter += getFinalPowerStep(prevVal, endLoc);
			if(result != null)
				result.add(new SampledValue(new FloatValue((float) (counter+delta)), prevVal.timestamp, Quality.GOOD));
			else
				counter += getPartialConsumptionValue(timeSeries, endLoc, false);
		}
		return counter;
	}
}
