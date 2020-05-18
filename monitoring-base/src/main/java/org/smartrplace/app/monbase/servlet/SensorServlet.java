package org.smartrplace.app.monbase.servlet;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONObject;
import org.ogema.core.model.ValueResource;
import org.ogema.core.model.simple.IntegerResource;
import org.ogema.core.model.units.PhysicalUnitResource;
import org.ogema.devicefinder.api.Datapoint;
import org.ogema.model.locations.Room;
import org.ogema.model.sensors.Sensor;
import org.ogema.tools.resource.util.ResourceUtils;
import org.smartrplace.app.monbase.MonitoringController;
import org.smartrplace.util.frontend.servlet.ServletResourceDataProvider;
import org.smartrplace.util.frontend.servlet.UserServletParamData;
import org.smartrplace.util.frontend.servlet.UserServlet.ServletPageProvider;
import org.smartrplace.util.frontend.servlet.UserServlet.ServletValueProvider;
import org.smartrplace.util.frontend.servlet.UserServletUtil;

/** Implementation of servlet on /org/sp/app/monappserv/userdata */
public class SensorServlet implements ServletPageProvider<Room> {
	List<Room> knownRooms = null;
	/** Room location -> Sensors*/
	Map<String, List<Sensor>> knownSensors = new HashMap<String, List<Sensor>>();
	final MonitoringController controller;
	
	public SensorServlet(MonitoringController controller) {
		this.controller = controller;
	}

	@Override
	public Map<String, ServletValueProvider> getProviders(Room object, String user, Map<String, String[]> parameters) {
		Map<String, ServletValueProvider> result = new HashMap<>();
		boolean suppressNan = UserServletUtil.suppressNan(parameters);
		
		List<Sensor> roomSensors = knownSensors.get(object.getLocation());
		//TODO: Add caching mechanism, now new sensors are only found when the bundle is restarted
		if(roomSensors == null ) {
			roomSensors = ResourceUtils.getDevicesFromRoom(controller.appMan.getResourceAccess(), Sensor.class, object);
			knownSensors.put(object.getLocation(), roomSensors);
		}
		int index = 0;
		for(Sensor sens: roomSensors) {
			ServletResourceDataProvider resProv = new ServletResourceDataProvider(sens, suppressNan) {
				@Override
				protected void addAdditionalInformation(JSONObject result) {
					ValueResource reading = sens.reading();
					UserServletUtil.addValueEntry(reading, suppressNan, result);
					String roomName = controller.getRoomLabel(sens.reading().getLocation(), null);
					if(roomName != null)
						result.put("monitoringRoomName", roomName);
					IntegerResource alarmStatus = sens.getSubResource("alarmStatus");
					if (alarmStatus != null)
						result.put("alarmStatus", alarmStatus.getValue());
					if (reading instanceof PhysicalUnitResource) {
						String unit = ((PhysicalUnitResource) reading).getUnit().toString();
						result.put("unit", unit);
					}
					
					UserServletParamData pdata = new UserServletParamData(parameters);
					if(pdata.provideExtended && controller.dpService != null) {
						Datapoint dp = controller.dpService.getDataPointStandard(reading);
						if(dp.getConsumptionInfo() != null)
							result.put("consumptionInfo", dp.getConsumptionInfo());
						if(dp.getGaroDataType() != null)
							result.put("garoType", dp.getGaroDataType().label(null));
						String subRoomLoc = dp.getSubRoomLocation(pdata.locale, null);
						if(subRoomLoc != null)
							result.put("subRoomLocation", subRoomLoc);
						if(dp.getDeviceResource() != null)
							result.put("deviceResourceLocation", dp.getDeviceResource().getLocation());
						if(dp.getDeviceResource() != null)
							result.put("deviceResourceType", dp.getDeviceResource().getResourceType().getName());
						if(dp.getTimeSeriesID() != null)
							result.put("timeSeries", dp.getTimeSeriesID());
					}
				}
			};
			result.put(""+index, resProv);
			index++;
		}
		return result;
	}

	@Override
	public Collection<Room> getAllObjects(String user) {
		if(knownRooms == null) {
			knownRooms = controller.appMan.getResourceAccess().getToplevelResources(Room.class);
		}
		return knownRooms;
	}

	@Override
	public Room getObject(String objectId) {
		for(Room room: knownRooms) {
			if(room.getLocation().equals(objectId) || ResourceUtils.getHumanReadableShortName(room).equals(objectId))
				return room;
		}
		return null;
	}
}
