package org.smartrplace.app.monbase.servlet;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.ogema.devicefinder.api.DPRoom;
import org.ogema.devicefinder.api.Datapoint;
import org.ogema.devicefinder.api.DatapointGroup;
import org.ogema.devicefinder.api.DatapointService;
import org.smartrplace.app.monbase.MonitoringController;
import org.smartrplace.util.frontend.servlet.ServletNumProvider;
import org.smartrplace.util.frontend.servlet.ServletStringProvider;
import org.smartrplace.util.frontend.servlet.ServletSubDataProvider;
import org.smartrplace.util.frontend.servlet.UserServlet;
import org.smartrplace.util.frontend.servlet.UserServlet.ServletPageProvider;
import org.smartrplace.util.frontend.servlet.UserServlet.ServletValueProvider;
import org.smartrplace.util.frontend.servlet.UserServletUtil;

import de.iwes.timeseries.eval.base.provider.utils.TimeSeriesDataImpl;
import de.iwes.widgets.api.widgets.localisation.OgemaLocale;

/** Implementation of servlet on /org/sp/app/monappserv/userdata */
public class DatapointServlet implements ServletPageProvider<Datapoint> {
	//List<Room> knownRooms = null;
	
	/** Hash location or other ID -> Timeseries*/
	//final Map<String, TimeSeriesDataImpl> knownTimeseries = UserServlet.knownTS;
	final MonitoringController controller;
	final DatapointService dpService;
	
	public DatapointServlet(MonitoringController controller) {
		this.controller = controller;
		this.dpService = controller.dpService;
		if(dpService == null)
			throw new IllegalStateException("Datapoint Service required for Datapoint Servlet!");
	}

	@Override
	public Map<String, ServletValueProvider> getProviders(Datapoint object, String user, Map<String, String[]> parameters) {
		Map<String, ServletValueProvider> result = new HashMap<>();
		
		ServletStringProvider location = new ServletStringProvider(object.getLocation());
		result.put("location", location);
		ServletNumProvider id = new ServletNumProvider(getNumericalId(object.getLocation()));
		result.put("id", id);
		ServletStringProvider labelStd = new ServletStringProvider(object.label(null));
		result.put("labelStd", labelStd);
		OgemaLocale locale = UserServlet.getLocale(parameters);
		if(locale != null) {
			ServletStringProvider labelLocale = new ServletStringProvider(object.label(locale));
			result.put("labelLocale", labelLocale);
		}
		ServletStringProvider type = new ServletStringProvider(object.getTypeName(null));
		result.put("type", type);
		ServletStringProvider roomName = new ServletStringProvider(object.getRoomName(locale));
		result.put("roomName", roomName);
		DPRoom dpRoom = object.getRoom();
		if(dpRoom != null) {
			ServletNumProvider roomId = new ServletNumProvider(dpRoom.getLocation().hashCode());
			result.put("roomId", roomId);
		}
		ServletStringProvider subLocation = new ServletStringProvider(object.getSubRoomLocation(null, null));
		result.put("subLocation", subLocation);
		
		if(!UserServletUtil.isDepthTimeSeries(parameters))
			return result;
		TimeSeriesDataImpl ts = object.getTimeSeriesDataImpl(locale);
		if(ts != null) {
			@SuppressWarnings("unchecked")
			ServletPageProvider<TimeSeriesDataImpl> timeSeriesProvider =
					(ServletPageProvider<TimeSeriesDataImpl>) UserServlet.getProvider("org.smartrplace.app.monbase.servlet.TimeseriesBaseServlet", "timeseries");
			ServletSubDataProvider<TimeSeriesDataImpl> timeseries = new ServletSubDataProvider<TimeSeriesDataImpl>(timeSeriesProvider,
				ts, true, parameters);
			result.put("timeseries", timeseries);
		}
		
		return result;
	}

	@Override
	public Collection<Datapoint> getAllObjects(String user) {
		return dpService.getAllDatapoints();
	}

	@Override
	public Datapoint getObject(String objectId) {
		Datapoint obj;
		if(objectId.contains("::")) {
			String[] gwd = DatapointGroup.getGroupIdAndGwForDp(objectId);
			obj = dpService.getDataPointAsIs(gwd[0], gwd[1]);
		} else
			obj = dpService.getDataPointAsIs(objectId);
		return obj;
	}
	
	@Override
	public String getObjectId(Datapoint obj) {
		return obj.id();
	}
	
	@Override
	public String getObjectName() {
		return "datapoint";
	}
}
