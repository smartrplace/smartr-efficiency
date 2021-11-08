package org.smartrplace.app.monbase.servlet;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.ogema.core.model.simple.BooleanResource;
import org.ogema.core.model.simple.FloatResource;
import org.ogema.core.model.simple.IntegerResource;
import org.ogema.core.model.simple.SingleValueResource;
import org.ogema.core.model.simple.TimeResource;
import org.ogema.core.model.units.PhysicalUnit;
import org.ogema.core.model.units.PhysicalUnitResource;
import org.ogema.core.timeseries.ReadOnlyTimeSeries;
import org.ogema.devicefinder.api.DPRoom;
import org.ogema.devicefinder.api.Datapoint;
import org.ogema.devicefinder.api.DatapointGroup;
import org.ogema.devicefinder.api.DatapointService;
import org.ogema.devicefinder.util.DpGroupUtil;
import org.ogema.tools.resource.util.ValueResourceUtils;
import org.smartrplace.app.monbase.MonitoringController;
import org.smartrplace.apps.hw.install.config.InstallAppDevice;
import org.smartrplace.util.frontend.servlet.ServletNumProvider;
import org.smartrplace.util.frontend.servlet.ServletStringProvider;
import org.smartrplace.util.frontend.servlet.ServletTimeseriesProvider;
import org.smartrplace.util.frontend.servlet.UserServlet;
import org.smartrplace.util.frontend.servlet.UserServlet.ServletPageProvider;
import org.smartrplace.util.frontend.servlet.UserServlet.ServletValueProvider;
import org.smartrplace.util.frontend.servlet.UserServletUtil;

import de.iwes.timeseries.eval.garo.api.base.GaRoDataType;
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
		
		DPRoom dpRoom = object.getRoom();
		int roomIdInt = -3;
		if(dpRoom != null) {
			roomIdInt = dpRoom.getLocation().hashCode();
		}

		GaRoDataType garo = object.getGaroDataType();
		String typeId = null;
		if(garo != null) {
			typeId = garo.id();
		}
		
		int deviceIdInt = -5;
		InstallAppDevice iad = null;
		if(object.getResource() != null) {
			iad = DpGroupUtil.getInstallAppDeviceForSubCashed(object.getResource(), controller.appMan);
			if(iad != null)
				deviceIdInt = iad.getLocation().hashCode();
		}
		
		if(!UserServletUtil.isPOST(parameters)) {
			//perform filtering
			String roomFilter = UserServlet.getParameter("room", parameters);
			if(roomFilter != null) {
				if(dpRoom == null)
					return null;
				int roomFilterId;
				try {
					roomFilterId = Integer.parseInt(roomFilter);
				} catch(NumberFormatException e) {
					roomFilterId = roomFilter.hashCode();
				}
				if(roomIdInt != roomFilterId)
					return null;
			}
			
			String dpTypeFilter = UserServlet.getParameter("type", parameters);
			if(dpTypeFilter != null) {
				if(Boolean.getBoolean("org.smartrplace.app.monbase.servlet.replaceCurrentByForecast") && dpTypeFilter.equals("OutsideTemperatureExt"))
					dpTypeFilter = "OutsideTemperaturePerForcecast";
				if(garo == null)
					return null;
				if(!dpTypeFilter.equals(typeId))
					return null;
			}
			
			String deviceFilter = UserServlet.getParameter("device", parameters);
			if(deviceFilter != null) {
				if(iad == null)
					return null;
				int deviceFilterId;
				try {
					deviceFilterId = Integer.parseInt(deviceFilter);
				} catch(NumberFormatException e) {
					deviceFilterId = deviceFilter.hashCode();
				}
				if(deviceIdInt != deviceFilterId)
					return null;
			}
		}

		String locationStr;
		if(object.isLocal()) {
			locationStr = object.getLocation();
		} else {
			String locloc = object.getLocation();
			String gw = object.getGatewayId();
			locationStr = DatapointGroup.getGroupIdForGw(locloc, gw);
			ServletStringProvider gateway = new ServletStringProvider(gw);
			result.put("gateway", gateway);
			ServletStringProvider locationLocal = new ServletStringProvider(locloc);
			result.put("locationLocal", locationLocal);
		}
		ServletNumProvider id = new ServletNumProvider(getNumericalId(locationStr));
		result.put("id", id);
		
		if(object.getResource() != null && (object.getResource() instanceof SingleValueResource)) {
			SingleValueResource sres = (SingleValueResource) object.getResource();
			ServletNumProvider valueP;
			if(sres instanceof FloatResource) {
				valueP = new ServletNumProvider(((FloatResource)sres).getValue());
			} else if(sres instanceof IntegerResource) {
				valueP = new ServletNumProvider(((IntegerResource)sres).getValue());
			} else if(sres instanceof TimeResource) {
				valueP = new ServletNumProvider(((TimeResource)sres).getValue());
			} else if(sres instanceof BooleanResource) {
				valueP = new ServletNumProvider(((BooleanResource)sres).getValue());
			} else
				valueP = new ServletNumProvider(ValueResourceUtils.getFloatValue(sres));
			result.put("currentValue", valueP);
		} else if(object.getCurrentValue() != null) {
			ServletNumProvider valueP = new ServletNumProvider(object.getCurrentValue());			
			result.put("currentValue", valueP);
		}
		if(UserServletUtil.isValueOnly(parameters))
			return result;
		
		ServletStringProvider location = new ServletStringProvider(locationStr);
		result.put("location", location);

		ServletStringProvider labelStd = new ServletStringProvider(object.label(null));
		result.put("labelStd", labelStd);
		OgemaLocale locale = UserServlet.getLocale(parameters);
		if(locale != null) {
			ServletStringProvider labelLocale = new ServletStringProvider(object.label(locale));
			result.put("labelLocale", labelLocale);
		}
		String typeName = object.getTypeName(null);
		ServletStringProvider type = new ServletStringProvider(typeName);
		result.put("type", type);
		if(garo != null) {
			ServletStringProvider typeGaro = new ServletStringProvider(typeId);
			result.put("typeId", typeGaro);
		}
		ServletStringProvider roomName = new ServletStringProvider(object.getRoomName(locale));
		result.put("roomName", roomName);
		if(dpRoom != null) {
			ServletNumProvider roomId = new ServletNumProvider(roomIdInt);
			result.put("roomId", roomId);
		}
		ServletStringProvider subLocation = new ServletStringProvider(object.getSubRoomLocation(null, null));
		result.put("subLocation", subLocation);
		
		if(iad != null) {
			ServletNumProvider deviceId = new ServletNumProvider(iad.getLocation().hashCode());
			result.put("deviceId", deviceId);
		}
		if(object.getResource() != null && (object.getResource() instanceof PhysicalUnitResource)) {
			PhysicalUnit unit = ((PhysicalUnitResource)object.getResource()).getUnit();
			ServletStringProvider unitProv = new ServletStringProvider(unit.toString());
			result.put("unit", unitProv);
		}
		
		if(!UserServletUtil.isDepthTimeSeries(parameters))
			return result;
		ReadOnlyTimeSeries ts = object.getTimeSeries();
		if(ts != null) {
			//@SuppressWarnings("unchecked")
			//ServletPageProvider<TimeSeriesDataImpl> timeSeriesProvider =
			//		(ServletPageProvider<TimeSeriesDataImpl>) UserServlet.getProvider("org.smartrplace.app.monbase.servlet.TimeseriesBaseServlet", "timeseries");
			//TimeSeriesDataImpl tsi = UserServletUtil.getOrAddTimeSeriesDataPlus(ts, locationStr);
			//ServletSubDataProvider<TimeSeriesDataImpl> timeseries = new ServletSubDataProvider<TimeSeriesDataImpl>(timeSeriesProvider,
			//	tsi, true, ReturnStructure.TOPARRAY_DICTIONARY, parameters);
			ServletTimeseriesProvider timeseries = new ServletTimeseriesProvider(null, ts, controller.appMan, null, parameters);
			timeseries.addUTCOffset = true;
			result.put("timeseries", timeseries);
		}
		
		return result;
	}

	@Override
	public Collection<Datapoint> getAllObjects(String user) {
		return dpService.getAllDatapoints();
	}

	@Override
	public Datapoint getObject(String objectId, String user) {
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
		if(obj.isLocal()) {
			return obj.id();
		} else {
			return DatapointGroup.getGroupIdForGw(obj.id(), obj.getGatewayId());
		}
	}
	
	@Override
	public String getObjectName() {
		return "datapoint";
	}
}
