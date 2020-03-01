package org.smartrplace.app.monbase.gui;

import java.util.ArrayList;
import java.util.Collection;
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
import org.ogema.tools.resource.util.ResourceUtils;
import org.ogema.tools.timeseries.v2.tools.TimeSeriesUtils;
import org.smartrplace.monbase.alarming.AlarmingManagement;
import org.smartrplace.util.frontend.servlet.ServletNumProvider;
import org.smartrplace.util.frontend.servlet.UserServlet.ServletPageProvider;
import org.smartrplace.util.frontend.servlet.UserServlet.ServletValueProvider;

import de.iwes.timeseries.eval.base.provider.utils.TimeSeriesDataImpl;
import de.iwes.util.resource.ValueResourceHelper;
import de.iwes.util.timer.AbsoluteTimeHelper;
import de.iwes.util.timer.AbsoluteTiming;

public class TimeSeriesServlet implements ServletPageProvider<TimeSeriesDataImpl> {
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
	public static float getDiffOfLast24h(ReadOnlyTimeSeries ts, ApplicationManager appMan) {
		long now = appMan.getFrameworkTime();
		SampledValue startval = ts.getPreviousValue(now-AlarmingManagement.DAY_MILLIS);
		SampledValue endval = ts.getPreviousValue(now);
		if(startval == null || endval == null)
			return -1;
			//return Float.NaN;
		try {
		return endval.getValue().getFloatValue() - startval.getValue().getFloatValue();
		} catch(NullPointerException e) {
			e.printStackTrace();
			return -2;
		}
	}
	public static float getDiffForDay(long timeStamp, ReadOnlyTimeSeries ts, ApplicationManager appMan) {
		long start = AbsoluteTimeHelper.getIntervalStart(timeStamp, AbsoluteTiming.DAY);
		long end = start + AlarmingManagement.DAY_MILLIS;
		SampledValue startval = ts.getPreviousValue(start);
		SampledValue endval = ts.getPreviousValue(end);
		if(startval == null || endval == null)
			return -1;
			//return Float.NaN;
		try {
		return endval.getValue().getFloatValue() - startval.getValue().getFloatValue();
		} catch(NullPointerException e) {
			e.printStackTrace();
			return -2;
		}
	}
	public static float getDiffForDayOrLast24(long timeStamp, ReadOnlyTimeSeries ts, ApplicationManager appMan) {
		long now = appMan.getFrameworkTime();
		long start = AbsoluteTimeHelper.getIntervalStart(timeStamp, AbsoluteTiming.DAY);		
		long end = start + AlarmingManagement.DAY_MILLIS;
		if(now>=start && now<=end)
			return getDiffOfLast24h(ts, appMan);
		else
			return getDiffForDay(timeStamp, ts, appMan);
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
			return getDiffForDay(ts, timeSeries, appMan);
		} else if(label.equals("I24")) {
			return getIntegralOfLast24h(timeSeries, appMan);
		}
		return Float.NaN;
	}
	
	public enum AggregationMode {
		/** In this mode the input is expected as power value that has to be integrated over time to get
		 * a daily value*/
		Power2Meter,
		/** In this mode the input is a meter value that has to be read e.g. once per day to
		 * generate daily values or the first derivate has to be calculated to get power values
		 */
		Meter2Meter,
		/** In this mode the input contains consumption values that reflect the consumption since the 
		 * last value provided. So these values have to be added up to generate a real meter or have to
		 * be divided by the respective time step to get power estimation values*/
		Consumption2Meter
	}
	
	public static class MeterReference {
		long referenceTime;
		float referenceMeterValue;
	}
	
	/** Calculate a virtual meter series that has the same counter value at a reference point as another
	 * real meter so that the further consumption trend can be compared directly
	 * @return
	 */
	public static List<SampledValue> getMeterFromConsumption(ReadOnlyTimeSeries timeSeries, long start, long end,
			MeterReference ref) {
		List<SampledValue> result = new ArrayList<>();
		double myRefValue = getInterpolatedValue(timeSeries, ref.referenceTime);
		double delta = ref.referenceMeterValue - myRefValue;
		double counter = 0;
		for(SampledValue sv: timeSeries.getValues(start, end)) {
			counter += sv.getValue().getFloatValue();
			result.add(new SampledValue(new FloatValue((float) (counter+delta)), sv.getTimestamp(), sv.getQuality()));
		}
		return result;
	}
	public static List<SampledValue> getDayValues(ReadOnlyTimeSeries timeSeries, long start, long end,
			AggregationMode mode, float factor) {
		if(mode == AggregationMode.Power2Meter)
			throw new UnsupportedClassVersionError("Power2Meter not supported yet");
		long startDay = AbsoluteTimeHelper.getIntervalStart(start, AbsoluteTiming.DAY);
		float prevCounter;
		switch(mode) {
		case Meter2Meter:
			prevCounter = getInterpolatedValue(timeSeries, startDay);
			break;
		default:
			prevCounter = 0;
		}
		List<SampledValue> result = new ArrayList<>();
		while(startDay < end) {
			long startDayPrev = startDay;
			startDay = AbsoluteTimeHelper.addIntervalsFromAlignedTime(startDay, 1, AbsoluteTiming.DAY);
			float newCounter = getInterpolatedValue(timeSeries, startDay);
			float newDayVal;
			switch(mode) {
			case Meter2Meter:
				newDayVal = newCounter - prevCounter;
				prevCounter = newCounter;
				break;
			default:
				newDayVal = newCounter;
			}
			result.add(new SampledValue(new FloatValue(newDayVal), startDayPrev, Quality.GOOD));
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
}
