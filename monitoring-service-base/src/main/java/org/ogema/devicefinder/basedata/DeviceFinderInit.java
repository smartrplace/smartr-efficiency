package org.ogema.devicefinder.basedata;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.ogema.core.application.ApplicationManager;
import org.ogema.core.application.Timer;
import org.ogema.core.model.Resource;
import org.ogema.core.model.ResourceList;
import org.ogema.core.model.ValueResource;
import org.ogema.core.model.simple.FloatResource;
import org.ogema.core.model.simple.StringResource;
import org.ogema.core.model.units.TemperatureResource;
import org.ogema.devicefinder.api.DPRoom;
import org.ogema.devicefinder.api.Datapoint;
import org.ogema.devicefinder.api.DatapointInfo.UtilityType;
import org.ogema.devicefinder.api.DatapointService;
import org.ogema.devicefinder.api.DpConnection.DpElectricityConnection;
import org.ogema.devicefinder.api.SumUpLevel;
import org.ogema.devicefinder.service.DatapointServiceImpl;
import org.ogema.devicefinder.util.DPRoomImpl;
import org.ogema.devicefinder.util.DatapointImpl;
import org.ogema.devicefinder.util.FactorScale;
import org.ogema.model.actors.OnOffSwitch;
import org.ogema.model.connections.ElectricityConnection;
import org.ogema.model.devices.connectiondevices.ElectricityConnectionBox;
import org.ogema.model.devices.generators.PVPlant;
import org.ogema.model.locations.Room;
import org.ogema.model.sensors.ElectricCurrentSensor;
import org.ogema.model.sensors.ElectricEnergySensor;
import org.ogema.model.sensors.ElectricFrequencySensor;
import org.ogema.model.sensors.ElectricVoltageSensor;
import org.ogema.model.sensors.PowerSensor;
import org.ogema.model.sensors.Sensor;
import org.ogema.model.sensors.TemperatureSensor;
import org.ogema.timeseries.eval.simple.api.KPIResourceAccess;
import org.ogema.timeseries.eval.simple.api.TimeProcUtil;
import org.ogema.tools.resource.util.ResourceUtils;
import org.smartrplace.app.monservice.MonitoringServiceBaseController;

import de.iwes.timeseries.eval.garo.api.base.GaRoDataType;
import de.iwes.timeseries.eval.garo.api.helper.base.GaRoEvalHelper;
import de.iwes.util.resource.ResourceHelper;
import de.iwes.util.resource.ValueResourceHelper;
import extensionmodel.smarteff.api.base.SmartEffUserData;
import extensionmodel.smarteff.api.common.BuildingData;
import extensionmodel.smarteff.api.common.BuildingUnit;

public class DeviceFinderInit {
	protected static Timer openWeatherTempTimer = null;
	
	protected static final Map<UtilityType, Float> defaultPrices = new HashMap<>();
	protected static final Map<UtilityType, Float> defaultEffs = new HashMap<>();
	static {
		defaultPrices.put(UtilityType.ELECTRICITY, 0.28f);
		defaultPrices.put(UtilityType.HEAT_ENERGY, 0.06f);
		defaultEffs.put(UtilityType.HEAT_ENERGY, 0.95f);
		defaultPrices.put(UtilityType.WATER, 1.8f);
	}
	
	/** Intended to be called on startup by one initializing application*/
	public static void initAllDatapoints(ApplicationManager appMan,
			DatapointService dpService) {
		getAllDatapoints(appMan, dpService);
		
		Long referenceTime = Long.getLong("org.ogema.timeseries.eval.simple.api.meteringreferencetime");
		if(referenceTime != null)
			TimeProcUtil.initDefaultMeteringReferenceResource(referenceTime, true, appMan.getResourceAccess());
		
		TemperatureResource openWeatherMapTemp = KPIResourceAccess.getOpenWeatherMapTemperature(appMan.getResourceAccess());
		if(openWeatherMapTemp != null) {
			if(openWeatherTempTimer == null) {
				openWeatherTempTimer = appMan.createTimer(TimeProcUtil.DAY_MILLIS, new TimerListenerInitialElapsed() {
					@Override
					public void timerElapsed(Timer arg0) {
						long now = appMan.getFrameworkTime();
						float val = TimeProcUtil.getInterpolatedOrAvailableValue(openWeatherMapTemp.forecast(), now);
						if(!Float.isNaN(val))
							openWeatherMapTemp.setValue(val);
					}
				});
			}
		}
		
		SmartEffUserData smUser = appMan.getResourceAccess().getResource("master/editableData");
		if(smUser != null) {
			ResourceList<BuildingData> buildingData = smUser.buildingData();
			buildingData.create();
			BuildingData building = buildingData.addDecorator("E_0", BuildingData.class);
			ResourceList<BuildingUnit> roomList = building.buildingUnit();
			roomList.create();
			BuildingUnit overall = roomList.addDecorator(DPRoom.BUILDING_OVERALL_ROOM_LABEL, BuildingUnit.class);
			overall.name().<StringResource>create().setValue(DPRoom.BUILDING_OVERALL_ROOM_LABEL);
			for(DPRoom room: dpService.getAllRooms()) {
				BuildingUnit bu = roomList.addDecorator(ResourceUtils.getValidResourceName(room.label(null)), BuildingUnit.class);
				bu.name().<StringResource>create().setValue(room.label(null));				
			}
			smUser.activate(true);
		}
		for(UtilityType type: UtilityType.values()) {
			FloatResource priceRes = KPIResourceAccess.getDefaultPriceResource(type, appMan);
			if(priceRes != null && (!priceRes.exists())) {
				Float price = defaultPrices.get(type);
				if(price != null)
					ValueResourceHelper.setCreate(priceRes, price);
				FloatResource effRes = KPIResourceAccess.getDefaultEfficiencyResource(type, appMan);
				Float eff = defaultEffs.get(type);
				if(eff != null)
					ValueResourceHelper.setCreate(effRes, eff);
			}
		}
	}
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
		DPRoom dproom = dpService.getRoom(DPRoom.BUILDING_OVERALL_ROOM_LABEL);
		for(ElectricityConnectionBox meter: meters) {
			Room room = ResourceUtils.getDeviceRoom(meter);
			if(room != null)
				continue; // we have already processed this
			List<Sensor> sensors = meter.getSubResources(Sensor.class, true);
			List<Datapoint> valress = new ArrayList<>();
			for(Sensor sens: sensors) {
				Datapoint dp = getDataPoint(sens.reading(), dpService, sens, dproom);
				valress.add(dp);
				checkForElectricityMeter(sens, dp);
			}
			result.put(dproom, valress);
		}
		
		List<PVPlant> pvs = appMan.getResourceAccess().getToplevelResources(PVPlant.class);
		for(PVPlant pv: pvs) {
			Room room = ResourceUtils.getDeviceRoom(pv);
			if(room != null)
				continue; // we have already processed this
			List<Sensor> sensors = pv.getSubResources(Sensor.class, true);
			List<Datapoint> valress = new ArrayList<>();
			for(Sensor sens: sensors) {
				Datapoint dp = getDataPoint(sens.reading(), dpService, sens, dproom);
				valress.add(dp);
				checkForElectricityMeter(sens, dp);
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
			if(omitSensor(sens))
				continue;
			ValueResource valRes = sens.reading();
			
			Datapoint dp = getDataPoint(valRes, dpService, sens, room);
			valress.add(dp);
			if(sens instanceof TemperatureSensor)  {
				TemperatureSensor tsens = (TemperatureSensor) sens;
				if(tsens.settings().setpoint().exists())
					valress.add(getDataPoint(tsens.settings().setpoint(), dpService, sens, room));
				if(tsens.deviceFeedback().setpoint().exists())
					valress.add(getDataPoint(tsens.deviceFeedback().setpoint(), dpService, sens, room));
			}
			else {
				checkForElectricityMeter(sens, dp);
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

	protected static boolean checkForElectricityMeter(Sensor sens, Datapoint dp) {
		Resource parent = sens.getParent();
		//TODO: Check handling of thermal connections / heat meters
		if(parent != null && (parent instanceof ElectricityConnection || parent instanceof PVPlant)) {
			if(sens instanceof PowerSensor) {
				dp.info().getConnection(parent.getLocation(), UtilityType.ELECTRICITY).setPowerSensorDp(dp);
			} else if(sens instanceof ElectricEnergySensor) {
				dp.info().getConnection(parent.getLocation(), UtilityType.ELECTRICITY).setEnergySensorDp(dp);
				//TODO: This setting will not work for all electricity meters. We will have to develop a differentiation in the future
				dp.setScale(new FactorScale(1f/3600000f));
			} else if(sens instanceof ElectricCurrentSensor) {
				((DpElectricityConnection)dp.info().getConnection(parent.getLocation(), UtilityType.ELECTRICITY)).setCurrentSensorDp(dp);
			} else if(sens instanceof ElectricVoltageSensor) {
				((DpElectricityConnection)dp.info().getConnection(parent.getLocation(), UtilityType.ELECTRICITY)).setVoltageSensorDp(dp);
			} else if(sens instanceof ElectricFrequencySensor) {
				((DpElectricityConnection)dp.info().getConnection(parent.getLocation(), UtilityType.ELECTRICITY)).setFrequencySensorDp(dp);
			}
			int majorLevel;
			boolean isOutlet = dp.getGaroDataType().label(null).contains("Outlet");
			if(ResourceHelper.hasParentAboveType(parent, ElectricityConnectionBox.class) >= 0 && (!isOutlet)) {
				majorLevel = SumUpLevel.BUILDING_LEVEL;
			} else if(dp.getLocation().contains("Vekin"))
				majorLevel = SumUpLevel.ROOM_LEVEL;
			else
				majorLevel = SumUpLevel.DEVICE_LEVEL;
			
			Resource parentList = parent.getParent();
			if(parentList != null && parentList.getName().equals("subPhaseConnections")) {
				dp.setSubRoomLocation(null, null, parent.getName());
				Resource nodeConnection = parentList.getParent();
				boolean sumOfAllPhasesisMeasured = false;
				if(nodeConnection != null && nodeConnection instanceof ElectricityConnection) {
					ElectricityConnection nodeConn = (ElectricityConnection) nodeConnection;
					//TODO: Check could be more precise
					if(nodeConn.powerSensor().isActive() || nodeConn.energySensor().isActive()) {
						sumOfAllPhasesisMeasured = true;
					}
				}
				dp.info().setSumUpLevel(majorLevel+(sumOfAllPhasesisMeasured?10:0));								
			} else {
				dp.info().setSumUpLevel(majorLevel);
			}
			return true;
		}
		return false;
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
	
	/** Filter out sensors that we do no want to use at all as they are faulty resources or similar*/
	protected static boolean omitSensor(Sensor sens) {
		if((sens instanceof PowerSensor) && (sens.getLocation().contains("THERMOSTAT")))
			return true;
		return false;
	}
}
