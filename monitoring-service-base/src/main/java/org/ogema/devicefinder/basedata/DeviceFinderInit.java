package org.ogema.devicefinder.basedata;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.ogema.core.application.ApplicationManager;
import org.ogema.core.model.Resource;
import org.ogema.core.model.ValueResource;
import org.ogema.devicefinder.api.DPRoom;
import org.ogema.devicefinder.api.Datapoint;
import org.ogema.devicefinder.api.DatapointService;
import org.ogema.devicefinder.service.DatapointServiceImpl;
import org.ogema.devicefinder.util.DPRoomImpl;
import org.ogema.devicefinder.util.DatapointImpl;
import org.ogema.model.actors.OnOffSwitch;
import org.ogema.model.devices.connectiondevices.ElectricityConnectionBox;
import org.ogema.model.locations.Room;
import org.ogema.model.sensors.Sensor;
import org.ogema.model.sensors.TemperatureSensor;
import org.ogema.tools.resource.util.ResourceUtils;
import org.smartrplace.app.monservice.MonitoringServiceBaseController;

import de.iwes.timeseries.eval.garo.api.base.GaRoDataType;
import de.iwes.timeseries.eval.garo.api.base.GaRoMultiEvalDataProvider;
import de.iwes.timeseries.eval.garo.api.helper.base.GaRoEvalHelper;

public class DeviceFinderInit {
	/** Re-implementation of finding sensors accoring to the SensorServlet*/
	public static Map<Room, List<Sensor>> getAllSensors(MonitoringServiceBaseController controller) {
		Map<Room, List<Sensor>> result = new HashMap<>();
		List<Room> rooms = controller.appMan.getResourceAccess().getToplevelResources(Room.class);
		for(Room room: rooms) {
			List<Sensor> slist = ResourceUtils.getDevicesFromRoom(controller.appMan.getResourceAccess(), Sensor.class, room);
			result.put(room, slist);
		}
		return result ;
	}
	
	/** See {@link #getAllDatapointsForRoom(MonitoringServiceBaseController, Room)}
	 * @param dpService 
	 * 
	 * @param controller
	 * @return
	 */
	public static Map<DPRoom, List<Datapoint>> getAllDatapoints(ApplicationManager appMan,
			DatapointService dpService) {
		Map<DPRoom, List<Datapoint>> result = new HashMap<>();
		//Map<Room, List<Sensor>> sensors = getAllSensors(controller);
		List<Room> rooms = appMan.getResourceAccess().getToplevelResources(Room.class);
		for(Room room: rooms) {
			DPRoomImpl dproom = new DPRoomImpl(room);
			List<Datapoint> valress = getAllDatapointsForRoom(appMan, dpService, dproom);
			result.put(dproom, valress);
		}
		
		List<ElectricityConnectionBox> meters = appMan.getResourceAccess().getToplevelResources(ElectricityConnectionBox.class);
		DPRoomImpl dproom = new DPRoomImpl(GaRoMultiEvalDataProvider.BUILDING_OVERALL_ROOM_ID, "Building");
		for(ElectricityConnectionBox meter: meters) {
			Room room = ResourceUtils.getDeviceRoom(meter);
			if(room != null)
				continue; // we have already processed this
			List<Sensor> sensors = meter.getSubResources(Sensor.class, true);
			List<Datapoint> valress = new ArrayList<>();
			for(Sensor sens: sensors) {
				valress.add(getDataPoint(sens.reading(), dpService, sens, dproom));
			}
			result.put(dproom, valress);
		}
		return result;
	}
	
	/** Does contain all plot information, e.g. RSSI information and BooleanResource USV state are not in
	 * the result yet 
	 * @param controller 
	 * @param room
	 * @return data points registered. If controller.dpService is null then own instances not registered
	 * 		with a service are returned as legacy option
	 */
	public static List<Datapoint> getAllDatapointsForRoom(ApplicationManager appMan,
			DatapointService dpService, DPRoom room) {
		List<Datapoint> valress = new ArrayList<>();
		List<Sensor> slist = ResourceUtils.getDevicesFromRoom(appMan.getResourceAccess(), Sensor.class, room.getResource());
		for(Sensor sens: slist) {
			ValueResource valRes = sens.reading();
			
			valress.add(getDataPoint(valRes, dpService, sens, room));
			if(sens instanceof TemperatureSensor)  {
				TemperatureSensor tsens = (TemperatureSensor) sens;
				if(tsens.settings().setpoint().exists())
					valress.add(getDataPoint(tsens.settings().setpoint(), dpService, sens, room));
				if(tsens.deviceFeedback().setpoint().exists())
					valress.add(getDataPoint(tsens.deviceFeedback().setpoint(), dpService, sens, room));
			}
		}
		List<OnOffSwitch> switches = ResourceUtils.getDevicesFromRoom(appMan.getResourceAccess(),
				OnOffSwitch.class, room.getResource());
		for(OnOffSwitch sw: switches) {
			valress.add(getDataPoint(sw.stateControl(), dpService, sw, room));
			valress.add(getDataPoint(sw.stateFeedback(), dpService, sw, room));
		}
		return valress;
	}

	protected static Datapoint getDataPoint(ValueResource valRes, DatapointService dpService,
			Resource sensorActorResource, DPRoom room) {
		final Datapoint dp;
		if(dpService != null) {
			dp = dpService.getDataPointStandard(valRes);
			GaRoDataType type = GaRoEvalHelper.getDataType(valRes.getLocation());
			if(type != null)
				dp.setGaroDataType(type);
		} else {
			dp = new DatapointImpl(valRes.getLocation(), null, valRes, null);
			DatapointServiceImpl.addStandardData(dp);
		}
		dp.setSensorActorResource(sensorActorResource);
		if(room != null)
			dp.setRoom(room);
		return dp;
	}
}
