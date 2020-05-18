package org.smartrplace.app.monbase.servlet;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.ogema.core.timeseries.InterpolationMode;
import org.ogema.core.timeseries.ReadOnlyTimeSeries;
import org.ogema.model.locations.Room;
import org.ogema.widgets.configuration.service.OGEMAConfigurations;
import org.smartrplace.app.monbase.MonitoringController;
import org.smartrplace.util.frontend.servlet.ServletTimeseriesProvider;
import org.smartrplace.util.frontend.servlet.UserServlet;
import org.smartrplace.util.frontend.servlet.UserServlet.ServletPageProvider;
import org.smartrplace.util.frontend.servlet.UserServlet.ServletValueProvider;

import de.iwes.timeseries.eval.base.provider.utils.TimeSeriesDataImpl;

/** Implementation of servlet on /org/sp/app/monappserv/userdata */
public class TimeseriesBaseServlet implements ServletPageProvider<TimeSeriesDataImpl> {
	List<Room> knownRooms = null;
	
	/** Hash location or other ID -> Timeseries*/
	//final Map<String, TimeSeriesDataImpl> knownTimeseries = UserServlet.knownTS;
	final MonitoringController controller;
	
	public TimeseriesBaseServlet(MonitoringController controller) {
		this.controller = controller;
	}

	@Override
	public Map<String, ServletValueProvider> getProviders(TimeSeriesDataImpl object, String user, Map<String, String[]> parameters) {
		Map<String, ServletValueProvider> result = new HashMap<>();
		
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
	public TimeSeriesDataImpl getObject(String objectId) {
		TimeSeriesDataImpl obj = UserServlet.knownTS.get(objectId);
		if(obj != null) return obj;
		Object objRaw = OGEMAConfigurations.getObject(UserServlet.TimeSeriesServletImplClassName, objectId);
		if(objRaw == null)
			return null;
		if(objRaw instanceof TimeSeriesDataImpl) {
			obj = (TimeSeriesDataImpl) objRaw;
			UserServlet.knownTS.put(objectId, obj);
		} else if(objRaw instanceof ReadOnlyTimeSeries) {
			obj = new TimeSeriesDataImpl((ReadOnlyTimeSeries)objRaw, objectId, objectId, InterpolationMode.NONE);
			UserServlet.knownTS.put(objectId, obj);			
		}
		return obj;
	}
	
	@Override
	public String getObjectId(TimeSeriesDataImpl obj) {
		return obj.label(null);
	}
}
