package org.smartrplace.app.monbase.servlet;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.ogema.core.model.simple.BooleanResource;
import org.ogema.core.model.simple.FloatResource;
import org.ogema.core.model.simple.IntegerResource;
import org.ogema.core.model.simple.SingleValueResource;
import org.ogema.core.model.simple.StringResource;
import org.ogema.core.model.simple.TimeResource;
import org.ogema.core.model.units.PhysicalUnit;
import org.ogema.core.model.units.PhysicalUnitResource;
import org.ogema.core.timeseries.ReadOnlyTimeSeries;
import org.ogema.devicefinder.api.DPRoom;
import org.ogema.devicefinder.api.Datapoint;
import org.ogema.devicefinder.api.DatapointGroup;
import org.ogema.devicefinder.api.DatapointService;
import org.ogema.devicefinder.util.DpGroupUtil;
import org.ogema.tools.resource.util.ResourceUtils;
import org.ogema.tools.resource.util.ValueResourceUtils;
import org.smartrplace.app.monbase.MonitoringController;
import org.smartrplace.apps.hw.install.config.InstallAppDevice;
import org.smartrplace.external.accessadmin.config.SubCustomerData;
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
	public static final List<String> knownApplications = Arrays.asList(new String[] {"Grid", "Battery", "Solar", "Water", "Heating", "Elevator", "Tenant"});
	
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
		int roomIdInt1 = -3;
		int roomIdInt = -3;
		if(dpRoom != null) {
			roomIdInt1 = dpRoom.getLocation().hashCode();
			roomIdInt = ServletPageProvider.getNumericalId(dpRoom.getLocation(), true);
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
		
		final String locationStr;
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

		if(!UserServletUtil.isPOST(parameters)) {
			//perform filtering
			String roomFilter = UserServlet.getParameter("roomId", parameters);
			if(roomFilter == null)
				roomFilter = UserServlet.getParameter("room", parameters);
			if(roomFilter != null) {
				if(dpRoom == null)
					return null;
				int roomFilterId;
				try {
					roomFilterId = Integer.parseInt(roomFilter);
				} catch(NumberFormatException e) {
					roomFilterId = roomFilter.hashCode();
				}
				if((roomIdInt != roomFilterId) && (roomIdInt1 != roomFilterId))
					return null;
			}
			
			String dpTypeFilter = UserServlet.getParameter("typeId", parameters);
			if(dpTypeFilter == null)
				dpTypeFilter = UserServlet.getParameter("type", parameters);
			if(dpTypeFilter != null) {
				if(Boolean.getBoolean("org.smartrplace.app.monbase.servlet.replaceCurrentByForecast") && dpTypeFilter.equals("OutsideTemperatureExt"))
					dpTypeFilter = "OutsideTemperaturePerForcecast";
				String outSideTempUseOnly = System.getProperty("org.smartrplace.app.monbase.servlet.outsideTemp.useOnly");
				if((outSideTempUseOnly != null) && dpTypeFilter.equals("OutsideTemperatureExt") && (!locationStr.equals(outSideTempUseOnly)))
					return null;
				if(garo == null)
					return null;
				if(!dpTypeFilter.equals(typeId))
					return null;
			}
			
			String deviceFilter = UserServlet.getParameter("deviceId", parameters);
			if(deviceFilter == null)
				deviceFilter = UserServlet.getParameter("device", parameters);
			if(deviceFilter != null) {
				if(iad == null)
					return null;
				int deviceFilterId;
				boolean found = false;
				try {
					deviceFilterId = Integer.parseInt(deviceFilter);
				} catch(NumberFormatException e) {
					deviceFilterId = deviceFilter.hashCode();
				}
				if(deviceIdInt == deviceFilterId)
					found = true;
				else if(deviceFilter.equals(iad.deviceId().getValue()))
					found = true;
				if(!found)
					return null;
			}
		}

		ServletNumProvider id = new ServletNumProvider(ServletPageProvider.getNumericalId(locationStr));
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
			
			IntegerResource cmsId = dpRoom.getResource().getSubResource("cmsId", IntegerResource.class);
			if(cmsId != null && cmsId.exists()) {
				ServletNumProvider cmsIdP = new ServletNumProvider(cmsId.getValue());
				result.put("roomCmsId", cmsIdP);				
			}
		}
		String subloc = object.getSubRoomLocation(null, null);
		if(subloc != null) {
			ServletStringProvider subLocation = new ServletStringProvider(subloc);
			result.put("subLocation", subLocation);
		}
		if(iad != null) {
			ServletNumProvider deviceId = new ServletNumProvider(iad.getLocation().hashCode());
			result.put("deviceId", deviceId);
			
			ServletStringProvider deviceShortId = new ServletStringProvider(iad.deviceId().getValue());
			result.put("deviceShortId", deviceShortId);			

			String devInstallLocation = null;
			StringResource devAppRes = iad.getSubResource("apiApplication", StringResource.class);
			if(devAppRes != null && devAppRes.exists())
				devInstallLocation = devAppRes.getValue();
			else {
				devInstallLocation = iad.installationLocation().getValue();
				if(!knownApplications.contains(devInstallLocation))
					devInstallLocation = null;
			}
			if(devInstallLocation != null) {
				ServletStringProvider application = new ServletStringProvider(devInstallLocation);
				result.put("application", application);
			}
			StringResource utilityRes = iad.getSubResource("deviceUtility", StringResource.class);
			if(utilityRes != null && utilityRes.exists()) {
				String utily = utilityRes.getValue();
				ServletStringProvider utility = new ServletStringProvider(utily);
				result.put("utility", utility);
			}

		}
		if(type != null && typeName.contains("HeatCostAllocator")) {
			//no unit
		} else if(object.getResource() != null && (object.getResource() instanceof PhysicalUnitResource)) {
			PhysicalUnit unit = ((PhysicalUnitResource)object.getResource()).getUnit();
			ServletStringProvider unitProv = new ServletStringProvider(unit.toString());
			result.put("unit", unitProv);
		} else {
			String locLow = object.getLocation().toLowerCase();
			String unit = null;
			if(locLow.contains("water") || (typeId != null && typeId.toLowerCase().contains("water")))
				unit = "m3";
			else if(locLow.contains("volume") || (typeId != null && typeId.toLowerCase().contains("volume")))
				unit = "m3";
			else if(locLow.contains("energy") || (typeId != null && typeId.toLowerCase().contains("energy")))
				unit = "kWh";
			else if(locLow.contains("power") || (typeId != null && typeId.toLowerCase().contains("power")))
				unit = "W";
			if(unit != null) {
				ServletStringProvider unitProv = new ServletStringProvider(unit);
				result.put("unit", unitProv);				
			}
		}
		try {
			SubCustomerData subc = iad.device().location().getSubResource("tenant", SubCustomerData.class);
			if(subc != null && subc.exists()) {
				String tname = ResourceUtils.getHumanReadableName(subc);
				if(!tname.contains("GesamtGebäude(C)")) {
					ServletStringProvider tenantName = new ServletStringProvider(tname);
					result.put("tenantName", tenantName);
					
					IntegerResource cmsIdRes = subc.getSubResource("cmsTenancyId", IntegerResource.class);
					if(cmsIdRes != null && cmsIdRes.exists()) {
						ServletNumProvider tenId = new ServletNumProvider(cmsIdRes.getValue());
						result.put("tenancyCmsId", tenId);
					}
				}
			}
		} catch(Exception e) {
			e.printStackTrace();
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
			if(object.getAlternativeTimeSeries() != null)
				timeseries.replacementTimeseries = object.getAlternativeTimeSeries();
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
