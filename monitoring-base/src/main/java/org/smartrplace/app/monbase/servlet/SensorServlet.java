package org.smartrplace.app.monbase.servlet;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONObject;
import org.ogema.core.model.ValueResource;
import org.ogema.model.locations.Room;
import org.ogema.model.sensors.Sensor;
import org.ogema.tools.resource.util.ResourceUtils;
import org.smartrplace.app.monbase.MonitoringController;
import org.smartrplace.util.frontend.servlet.ServletResourceDataProvider;
import org.smartrplace.util.frontend.servlet.UserServlet;
import org.smartrplace.util.frontend.servlet.UserServlet.ServletPageProvider;
import org.smartrplace.util.frontend.servlet.UserServlet.ServletValueProvider;

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
		List<Sensor> roomSensors = knownSensors.get(object.getLocation());
		//TODO: Add caching mechanism, now new sensors are only found when the bundle is restarted
		if(roomSensors == null ) {
			roomSensors = ResourceUtils.getDevicesFromRoom(controller.appMan.getResourceAccess(), Sensor.class, object);
			knownSensors.put(object.getLocation(), roomSensors);
		}
		int index = 0;
		for(Sensor sens: roomSensors) {
			ServletResourceDataProvider resProv = new ServletResourceDataProvider(sens) {
				@Override
				protected void addAdditionalInformation(JSONObject result) {
					ValueResource reading = sens.reading();
					UserServlet.addValueEntry(reading, result);
					String roomName = controller.getRoomLabel(sens.reading().getLocation(), null);
					if(roomName != null)
						result.put("monitoringRoomName", roomName);
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