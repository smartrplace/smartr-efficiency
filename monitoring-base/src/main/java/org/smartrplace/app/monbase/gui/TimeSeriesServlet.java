package org.smartrplace.app.monbase.gui;

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
import org.ogema.devicefinder.api.ConsumptionInfo.AggregationMode;
import org.ogema.tools.resource.util.ResourceUtils;
import org.ogema.tools.timeseries.v2.tools.TimeSeriesUtils;
import org.smartrplace.app.monbase.servlet.TimeseriesBaseServlet;
import org.smartrplace.monbase.alarming.AlarmingManagement;
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
	public static final long ACCEPTED_PREVIOUS_VALUE_DISTANCE_FOR_DAY_EVAL = AlarmingManagement.HOUR_MILLIS*12;
	
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
		return (float) (TimeSeriesUtils.integrate(ts, now-AlarmingManagement.DAY_MILLIS, now)/AlarmingManagement.HOUR_MILLIS);
	}
	/** This method is only applicable for AggregationMode.Meter2Meter*/
	public static float getDiffOfLast24h(ReadOnlyTimeSeries ts, ApplicationManager appMan) {
		return getDiffOfLast24h(ts, Boolean.getBoolean("org.smartrplace.app.monbase.dointerpolate"), appMan);
	}
	/** This method is only applicable for AggregationMode.Meter2Meter*/
	public static float getDiffOfLast24h(ReadOnlyTimeSeries ts, boolean interpolate, ApplicationManager appMan) {
		long now = appMan.getFrameworkTime();
		long start = now-AlarmingManagement.DAY_MILLIS;
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
		long end = start + AlarmingManagement.DAY_MILLIS;
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
		long end = start + AlarmingManagement.DAY_MILLIS;
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
	
	public static class MeterReference {
		public long referenceTime;
		public float referenceMeterValue;
	}
	
	/** Calculate a virtual meter series that has the same counter value at a reference point as another
	 * real meter so that the further consumption trend can be compared directly
	 * Note: Currently this method only supports interpolation
	 * @return
	 */
	public static List<SampledValue> getMeterFromConsumption(ReadOnlyTimeSeries timeSeries, long start, long end,
			MeterReference ref, AggregationMode mode) {
		if(mode == AggregationMode.Power2Meter)
			throw new UnsupportedClassVersionError("Power2Meter not supported yet");
		List<SampledValue> result = new ArrayList<>();
		final double myRefValue;
		final double delta;
		if(mode == AggregationMode.Consumption2Meter) {
			double counter = getPartialConsumptionValue(timeSeries, start, true);
			final List<SampledValue> svList;
			if(ref.referenceTime > start)
				svList = timeSeries.getValues(start, ref.referenceTime);
			else
				svList = timeSeries.getValues(ref.referenceTime, start);
			for(SampledValue sv: svList) {
				counter += sv.getValue().getFloatValue();
			}
			counter += getPartialConsumptionValue(timeSeries, end, false);
			if(ref.referenceTime > start)
				myRefValue = counter;
			else
				myRefValue = -counter;
		} else {
			myRefValue = getInterpolatedValue(timeSeries, ref.referenceTime);
		}
		if(Double.isNaN(myRefValue))
			return Collections.emptyList();
		delta = ref.referenceMeterValue - myRefValue;
		double counter = 0;
		if(mode == AggregationMode.Consumption2Meter) {
			counter = getPartialConsumptionValue(timeSeries, start, true);
		}
		for(SampledValue sv: timeSeries.getValues(start, end)) {
			if(mode == AggregationMode.Consumption2Meter)
				counter += sv.getValue().getFloatValue();
			else
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
							timeSeries, startCurrentDay, nextDayStart)/AlarmingManagement.HOUR_MILLIS);
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
}
