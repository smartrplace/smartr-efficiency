package org.smartrplace.app.monbase.servlet;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.ogema.core.timeseries.InterpolationMode;
import org.ogema.core.timeseries.ReadOnlyTimeSeries;
import org.ogema.devicefinder.api.Datapoint;
import org.ogema.widgets.configuration.service.OGEMAConfigurations;
import org.smartrplace.app.monbase.MonitoringController;
import org.smartrplace.util.frontend.servlet.ServletNumProvider;
import org.smartrplace.util.frontend.servlet.ServletStringProvider;
import org.smartrplace.util.frontend.servlet.ServletTimeseriesProvider;
import org.smartrplace.util.frontend.servlet.UserServlet;
import org.smartrplace.util.frontend.servlet.UserServlet.ServletPageProvider;
import org.smartrplace.util.frontend.servlet.UserServlet.ServletValueProvider;

import de.iwes.timeseries.eval.base.provider.utils.TimeSeriesDataImpl;

/** Implementation of servlet on /org/sp/app/monappserv/userdata */
public class TimeseriesBaseServlet implements ServletPageProvider<TimeSeriesDataImpl> {
	//List<Room> knownRooms = null;
	
	/** Hash location or other ID -> Timeseries*/
	//final Map<String, TimeSeriesDataImpl> knownTimeseries = UserServlet.knownTS;
	final MonitoringController controller;
	
	public TimeseriesBaseServlet(MonitoringController controller) {
		this.controller = controller;
	}

	@Override
	public Map<String, ServletValueProvider> getProviders(TimeSeriesDataImpl object, String user, Map<String, String[]> parameters) {
		Map<String, ServletValueProvider> result = new HashMap<>();
		
		ServletStringProvider tsID = new ServletStringProvider(getObjectId(object));
		result.put("timeseriesID", tsID);
		ZoneOffset utcOffset = ZoneOffset.systemDefault().getRules().getOffset(Instant.now());
		result.put("UTCoffset", new ServletNumProvider(utcOffset.getTotalSeconds()*1000));
		ServletTimeseriesProvider recData = new ServletTimeseriesProvider(object.label(null),
				object.getTimeSeries(), controller.appMan, null, parameters);
		result.put("data", recData);

		return result;
	}

	@Override
	public Collection<TimeSeriesDataImpl> getAllObjects(String user) {
		return UserServlet.knownTS.values();
	}

	@Override
	public boolean useOriginalObjectId() {
		return true;
	}
	
	@Override
	public TimeSeriesDataImpl getObject(String objectId, String user) {
		// Calculate objectId as in UserServlet#getObjects
		String objectIdStandard;
		try {
			int numId = Integer.parseInt(objectId);
			objectIdStandard = UserServlet.num2StringGet(numId);
			if(objectIdStandard == null)
				objectIdStandard = objectId;				
		} catch(NumberFormatException e) {
			objectIdStandard = objectId;
		}
		TimeSeriesDataImpl result = getObjectV1(objectIdStandard, objectId, user);
		if(result != null)
			return result;
		return null;
	}
	public TimeSeriesDataImpl getObjectV1(String objectIdStandard, String objectIdRequested, String user) {
		TimeSeriesDataImpl obj = UserServlet.knownTS.get(objectIdStandard);
		if(obj != null)
			return correctTimeseriesID(obj, objectIdRequested); //TODO: Check if this is really a good idea
		Object objRaw = OGEMAConfigurations.getObject(UserServlet.TimeSeriesServletImplClassName, objectIdStandard);
		if(objRaw == null) {
			controller.log.error("objRaw not found for "+UserServlet.TimeSeriesServletImplClassName+", "+objectIdRequested+" / "+objectIdStandard);
			Datapoint dp = controller.dpService.getDataPointStandard(objectIdStandard);
			if(dp == null) {
				controller.log.error("Datapoint not found for "+UserServlet.TimeSeriesServletImplClassName+", "+objectIdRequested+" / "+objectIdStandard);
				return null;
			}
			TimeSeriesDataImpl ts = dp.getTimeSeriesDataImpl(null);
			if(ts == null) {
				controller.log.error("Timeseries of Datapoint not found for "+UserServlet.TimeSeriesServletImplClassName+", "+objectIdRequested+" / "+objectIdStandard);
				return null;				
			}
			objRaw = ts;
		}
		if(objRaw instanceof TimeSeriesDataImpl) {
			obj = (TimeSeriesDataImpl) objRaw;
			UserServlet.knownTS.put(objectIdStandard, obj);
		} else if(objRaw instanceof ReadOnlyTimeSeries) {
			obj = new TimeSeriesDataImpl((ReadOnlyTimeSeries)objRaw, objectIdRequested, objectIdRequested, InterpolationMode.NONE);
			UserServlet.knownTS.put(objectIdStandard, obj);			
		}
		//TODO: Check if this is really a good idea
		return correctTimeseriesID(obj, objectIdRequested);
	}
	
	private TimeSeriesDataImpl correctTimeseriesID(TimeSeriesDataImpl obj, String objectIdRequested) {
		if(obj.label(null) != objectIdRequested) {
			System.out.println("WARNING: objectId:"+objectIdRequested+" ,label:"+obj.label(null));
			TimeSeriesDataImpl newObj = new TimeSeriesDataImpl(obj, objectIdRequested, objectIdRequested);
			UserServlet.knownTS.put(objectIdRequested, newObj);	
			return newObj;
		}
		return obj;		
	}
	
	@Override
	public String getObjectId(TimeSeriesDataImpl obj) {
		return obj.label(null);
	}
}
