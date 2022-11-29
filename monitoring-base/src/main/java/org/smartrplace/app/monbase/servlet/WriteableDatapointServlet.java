package org.smartrplace.app.monbase.servlet;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.ogema.core.application.ApplicationManager;
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
import org.ogema.devicefinder.api.DatapointService;
import org.ogema.devicefinder.util.DpGroupUtil;
import org.ogema.eval.timeseries.simple.smarteff.AlarmingUtiH;
import org.ogema.model.extended.alarming.AlarmConfiguration;
import org.ogema.tools.resource.util.LoggingUtils;
import org.ogema.tools.resource.util.ValueResourceUtils;
import org.smartrplace.app.monbase.MonitoringController;
import org.smartrplace.apps.hw.install.config.InstallAppDevice;
import org.smartrplace.spapi.model.WriteableDatapoint;
import org.smartrplace.spapi.model.WriteableDatapoints;
import org.smartrplace.util.frontend.servlet.ServletBooleanResourceProvider;
import org.smartrplace.util.frontend.servlet.ServletFloatResourceProvider;
import org.smartrplace.util.frontend.servlet.ServletIntegerResourceProvider;
import org.smartrplace.util.frontend.servlet.ServletNumProvider;
import org.smartrplace.util.frontend.servlet.ServletStringProvider;
import org.smartrplace.util.frontend.servlet.ServletTimeResourceProvider;
import org.smartrplace.util.frontend.servlet.ServletTimeseriesProvider;
import org.smartrplace.util.frontend.servlet.UserServlet;
import org.smartrplace.util.frontend.servlet.UserServlet.ServletPageProvider;
import org.smartrplace.util.frontend.servlet.UserServlet.ServletValueProvider;
import org.smartrplace.util.frontend.servlet.UserServletUtil;

import de.iwes.timeseries.eval.garo.api.base.GaRoDataType;
import de.iwes.util.resource.ResourceHelper;
import de.iwes.util.resource.ValueResourceHelper;
import de.iwes.widgets.api.widgets.localisation.OgemaLocale;

/** Implementation of servlet on /org/sp/app/monappserv/userdata */
public class WriteableDatapointServlet implements ServletPageProvider<WriteDPData> {
	
	final MonitoringController controller;
	final DatapointService dpService;
	private final WriteableDatapoints apidata;
	
	public WriteableDatapointServlet(MonitoringController controller) {
		this.controller = controller;
		this.dpService = controller.dpService;
		apidata = ResourceHelper.getOrCreateTopLevelResource(WriteableDatapoints.class, controller.appMan);
		
		// !!! Init is now done by device handler !!!
		//List<WriteableDatapoint> ress = apidata.datapoints().getAllElements();
		//for(WriteableDatapoint res: ress) {
		//	setLogging(res);
		//}

		if(dpService == null)
			throw new IllegalStateException("Datapoint Service required for WriteableDatapoint Servlet!");
	}

	/** Create WriteableDatapoint. Note that usually such datapoints are created via API calls, but also OGEMA apps
	 * can do this via this method.
	 * 
	 * @param loc datapoint location to be created
	 * @param sres if not null this resource will be used to store values, otherwise a FloatResource is created
	 * @param dpService
	 * @param appMan
	 * @return
	 */
	public static Datapoint createWriteableDatapoint(String loc,
			SingleValueResource sres,
			DatapointService dpService, ApplicationManager appMan) {
		WriteableDatapoints apidataLoc = ResourceHelper.getOrCreateTopLevelResource(WriteableDatapoints.class, appMan);
		WriteableDatapoint writeDp = apidataLoc.datapoints().add();
		return createWriteableDatapoint(loc, writeDp, sres, apidataLoc, dpService);
	}
	public static Datapoint createWriteableDatapoint(String loc, WriteableDatapoint writeDp,
			SingleValueResource sres,
			WriteableDatapoints apidata, DatapointService dpService) {
		WriteDPData existing = getExistingWDPData(loc, apidata, dpService); //getObject(loc, null);
		if(existing != null)
			throw new IllegalStateException("Writeable datapoint with id "+loc+" already exists:"+existing.writeDp.getLocation());
		ValueResourceHelper.setCreate(writeDp.datapointLocation(), loc);

		if(sres == null)
			writeDp.addDecorator("resource", FloatResource.class); //.add.resource().create();
		else
			writeDp.resource().setAsReference(sres);
		
		//timeseries is done by DeviceHandler, but make sure logging is activated right away
		Datapoint result = dpService.getDataPointStandard(loc);
		setLogging(writeDp);
		return result;
	}
	
	@Override
	public Map<String, ServletValueProvider> getProviders(WriteDPData obj, String user, Map<String, String[]> parameters) {
		Map<String, ServletValueProvider> result = new HashMap<>();
		
		final boolean isNew;
		if(obj.dp == null) {
			if(obj.writeDp.resource().exists())
				throw new IllegalStateException("If dp null resource cannot exist:"+obj.writeDp.getLocation());
			String loc = UserServlet.getParameter("location", parameters);
			obj.dp = createWriteableDatapoint(loc, obj.writeDp, null, apidata, dpService);
			
			/*WriteDPData existing = getObject(loc, null);
			if(existing != null)
				throw new IllegalStateException("Writeable datapoint with id "+loc+" already exists:"+existing.writeDp.getLocation());
			ValueResourceHelper.setCreate(obj.writeDp.datapointLocation(), loc);
			//FloatResource sres = ResourceHelper.getResourceAlsoVirtual(loc, FloatResource.class, controller.appMan.getResourceAccess());
			//if(!sres.exists()) {
			//	sres.create();
			//}
			//obj.writeDp.resource().setAsReference(sres);
			//We have to create as FloatResource although model only specifies SingleValueResource
			obj.writeDp.addDecorator("resource", FloatResource.class); //.add.resource().create();
			
			//timeseries is done by DeviceHandler, but make sure logging is activated right away
			obj.dp = dpService.getDataPointStandard(loc);
			setLogging(obj.writeDp);
			//RecordedData recData = ValueResourceHelper.getRecordedData(obj.writeDp.resource());
			//if(recData != null)
			//	obj.dp.setTimeSeries(recData);*/
			isNew = true;
		} else
			isNew = false;
		DPRoom dpRoom = obj.dp.getRoom();
		int roomIdInt1 = -3;
		int roomIdInt = -3;
		if(dpRoom != null) {
			roomIdInt1 = dpRoom.getLocation().hashCode();
			roomIdInt = ServletPageProvider.getNumericalId(dpRoom.getLocation(), true);
		}

		GaRoDataType garo = obj.dp.getGaroDataType();
		String typeId = null;
		if(garo != null) {
			typeId = garo.id();
		}
		
		int deviceIdInt = -5;
		InstallAppDevice iad = null;
		if(obj.dp.getResource() != null) {
			iad = DpGroupUtil.getInstallAppDeviceForSubCashed(obj.dp.getResource(), controller.appMan);
			if(iad != null)
				deviceIdInt = iad.getLocation().hashCode();
		}
		
		final String locationStr;
		locationStr = obj.dp.getLocation();

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
				if((roomIdInt != roomFilterId) && (roomIdInt1 != roomFilterId))
					return null;
			}
			
			String dpTypeFilter = UserServlet.getParameter("type", parameters);
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

		ServletNumProvider id = new ServletNumProvider(ServletPageProvider.getNumericalId(locationStr));
		result.put("id", id);
		
		SingleValueResource sres = (SingleValueResource) obj.writeDp.resource();
		ServletValueProvider valueP;
		if(sres instanceof FloatResource) {
			valueP = new ServletFloatResourceProvider((FloatResource)sres);
		} else if(sres instanceof IntegerResource) {
			valueP = new ServletIntegerResourceProvider((IntegerResource)sres);
		} else if(sres instanceof TimeResource) {
			valueP = new ServletTimeResourceProvider((TimeResource)sres);
		} else if(sres instanceof BooleanResource) {
			valueP = new ServletBooleanResourceProvider((BooleanResource)sres);
		} else
			valueP = new ServletNumProvider(ValueResourceUtils.getFloatValue(sres));
		result.put("currentValue", valueP);

		ServletStringProvider location = new ServletStringProvider(locationStr);
		result.put("location", location);

		final String label;
		if(obj.writeDp.name().isActive() && (!obj.writeDp.name().getValue().isEmpty()))
			label = obj.writeDp.name().getValue();
		else
			label = obj.dp.label(null);
		ServletStringProvider labelStd = new ServletStringProvider(label) {
			@Override
			public void setValue(String user, String key, String value) {
				ValueResourceHelper.setCreate(obj.writeDp.name(), value);
			}
		};
		result.put("labelStd", labelStd);
		OgemaLocale locale = UserServlet.getLocale(parameters);
		if(locale != null) {
			ServletStringProvider labelLocale = new ServletStringProvider(obj.dp.label(locale));
			result.put("labelLocale", labelLocale);
		}
		String typeName = obj.dp.getTypeName(null);
		ServletStringProvider type = new ServletStringProvider(typeName);
		result.put("type", type);
		if(garo != null) {
			ServletStringProvider typeGaro = new ServletStringProvider(typeId);
			result.put("typeId", typeGaro);
		}
		ServletStringProvider roomName = new ServletStringProvider(obj.dp.getRoomName(locale));
		result.put("roomName", roomName);
		if(dpRoom != null) {
			ServletNumProvider roomId = new ServletNumProvider(roomIdInt);
			result.put("roomId", roomId);
		}
		String subloc = obj.dp.getSubRoomLocation(null, null);
		if(subloc != null) {
			ServletStringProvider subLocation = new ServletStringProvider(subloc);
			result.put("subLocation", subLocation);
		}
		if(iad != null) {
			ServletNumProvider deviceId = new ServletNumProvider(iad.getLocation().hashCode());
			result.put("deviceId", deviceId);
		}
		if(obj.dp.getResource() != null && (obj.dp.getResource() instanceof PhysicalUnitResource)) {
			PhysicalUnit unit = ((PhysicalUnitResource)obj.dp.getResource()).getUnit();
			ServletStringProvider unitProv = new ServletStringProvider(unit.toString());
			result.put("unit", unitProv);
		}
		
		ServletBooleanResourceProvider disableLogging = new ServletBooleanResourceProvider(obj.writeDp.disableLogging()) {
			@Override
			public void setValue(String user, String key, String value) {
				super.setValue(user, key, value);
				setLogging(obj.writeDp);
			}
		};
		result.put("disableLogging", disableLogging);
		
		if(iad != null) {
			AlarmConfiguration alarm = AlarmingUtiH.getAlarmingConfiguration(iad, obj.writeDp.resource());
			ServletFloatResourceProvider lowerLimit = new ServletFloatResourceProvider(alarm.lowerLimit());
			result.put("lowerLimit", lowerLimit);
			ServletFloatResourceProvider upperLimit = new ServletFloatResourceProvider(alarm.upperLimit());
			result.put("upperLimit", upperLimit);
			ServletFloatResourceProvider maxViolationTimeWithoutAlarm = new ServletFloatResourceProvider(alarm.maxViolationTimeWithoutAlarm());
			result.put("maxViolationTimeWithoutAlarm", maxViolationTimeWithoutAlarm);
			ServletFloatResourceProvider maxIntervalBetweenNewValues = new ServletFloatResourceProvider(alarm.maxIntervalBetweenNewValues());
			result.put("maxIntervalBetweenNewValues", maxIntervalBetweenNewValues);
			
			ServletBooleanResourceProvider sendAlarm = new ServletBooleanResourceProvider(alarm.sendAlarm());
			result.put("sendAlarm", sendAlarm);
		}

		if(isNew)
			obj.writeDp.activate(true);
		
		if(!UserServletUtil.isDepthTimeSeries(parameters))
			return result;
		ReadOnlyTimeSeries ts = obj.dp.getTimeSeries();
		if(ts != null) {
			//@SuppressWarnings("unchecked")
			//ServletPageProvider<TimeSeriesDataImpl> timeSeriesProvider =
			//		(ServletPageProvider<TimeSeriesDataImpl>) UserServlet.getProvider("org.smartrplace.app.monbase.servlet.TimeseriesBaseServlet", "timeseries");
			//TimeSeriesDataImpl tsi = UserServletUtil.getOrAddTimeSeriesDataPlus(ts, locationStr);
			//ServletSubDataProvider<TimeSeriesDataImpl> timeseries = new ServletSubDataProvider<TimeSeriesDataImpl>(timeSeriesProvider,
			//	tsi, true, ReturnStructure.TOPARRAY_DICTIONARY, parameters);
			ServletTimeseriesProvider timeseries = new ServletTimeseriesProvider(null, ts, controller.appMan, null, parameters);
			if(obj.dp.getAlternativeTimeSeries() != null)
				timeseries.replacementTimeseries = obj.dp.getAlternativeTimeSeries();
			timeseries.addUTCOffset = true;
			result.put("timeseries", timeseries);
		}
		
		return result;
	}

	private static void setLogging(WriteableDatapoint writeDp) {
		if(writeDp.disableLogging().getValue())
			LoggingUtils.deactivateLogging(writeDp.resource());
		else
			LoggingUtils.activateLogging(writeDp.resource(), -2);
	}
	
	@Override
	public Collection<WriteDPData> getAllObjects(String user) {
		List<WriteableDatapoint> ress = apidata.datapoints().getAllElements();
		List<WriteDPData> result = new ArrayList<>();
		for(WriteableDatapoint res: ress) {
			WriteDPData el = new WriteDPData();
			el.writeDp = res;
			el.dp = dpService.getDataPointStandard(res.datapointLocation().getValue());
			result.add(el);
		}
		return result ;
	}

	@Override
	public WriteDPData getObject(String objectId, String user) {
		if(objectId.equals("NEW_DATAPOINT")) {
			WriteDPData el = new WriteDPData();
			el.writeDp = apidata.datapoints().add();
			el.dp = null;
			return el;
		}
		return getExistingWDPData(objectId, apidata, dpService);
	}
	
	public static WriteDPData getExistingWDPData(String dpLocation,
			WriteableDatapoints apidata, DatapointService dpService) {
		List<WriteableDatapoint> ress = apidata.datapoints().getAllElements();
		for(WriteableDatapoint res: ress) {
			if(res.datapointLocation().getValue().equals(dpLocation)) {
				WriteDPData el = new WriteDPData();
				el.writeDp = res;
				el.dp = dpService.getDataPointStandard(res.datapointLocation().getValue());
				return el;
			}
		}
		return null;
	}
	
	@Override
	public String getObjectId(WriteDPData obj) {
		if(obj.dp.isLocal()) {
			return obj.dp.id();
		} else {
			throw new IllegalStateException("Only local datapoints supported for Writeable Datapoints!");
			//return DatapointGroup.getGroupIdForGw(obj.dp.id(), obj.dp.getGatewayId());
		}
	}
	
	@Override
	public String getObjectName() {
		return "datapoint";
	}
}
