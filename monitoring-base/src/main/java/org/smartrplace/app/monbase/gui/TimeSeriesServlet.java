package org.smartrplace.app.monbase.gui;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.ogema.core.application.ApplicationManager;
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

public class TimeSeriesServlet implements ServletPageProvider<TimeSeriesDataImpl> {
	Map<String, ReadOnlyTimeSeries> knownSpecialTs = new HashMap<>();
	protected final ApplicationManager appMan;
	
	public TimeSeriesServlet(ApplicationManager appMan) {
		this.appMan = appMan;
	}

	@Override
	public Map<String, ServletValueProvider> getProviders(TimeSeriesDataImpl object, String user) {
		Map<String, ServletValueProvider> result = new LinkedHashMap<>();
		float val = specialEvaluation(object.label(null), object.getTimeSeries(), appMan);
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
	
	public static float specialEvaluation(String label, ReadOnlyTimeSeries timeSeries, ApplicationManager appMan) {
		if(label.equals("L24")) {
			return getDiffOfLast24h(timeSeries, appMan);
		} else if(label.equals("I24")) {
			return getIntegralOfLast24h(timeSeries, appMan);
		}
		return Float.NaN;
	}
}
