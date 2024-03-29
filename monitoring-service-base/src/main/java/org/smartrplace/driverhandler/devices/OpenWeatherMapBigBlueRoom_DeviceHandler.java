package org.smartrplace.driverhandler.devices;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.ogema.accessadmin.api.ApplicationManagerPlus;
import org.ogema.core.model.simple.SingleValueResource;
import org.ogema.core.model.units.TemperatureResource;
import org.ogema.core.resourcemanager.pattern.ResourcePattern;
import org.ogema.devicefinder.api.Datapoint;
import org.ogema.devicefinder.api.DatapointService;
import org.ogema.devicefinder.util.DeviceHandlerSimple;
import org.ogema.devicefinder.util.DeviceTableBase;
import org.ogema.eval.timeseries.simple.smarteff.AlarmingUtiH;
import org.ogema.model.devices.sensoractordevices.SensorDevice;
import org.ogema.model.devices.sensoractordevices.WindSensor;
import org.ogema.model.locations.Room;
import org.ogema.model.sensors.GenericFloatSensor;
import org.ogema.model.sensors.HumiditySensor;
import org.ogema.model.sensors.SolarIrradiationSensor;
import org.ogema.model.sensors.TemperatureSensor;
import org.ogema.timeseries.eval.simple.api.KPIResourceAccess;
import org.ogema.timeseries.eval.simple.mon3.std.StandardEvalAccess;
import org.ogema.timeseries.eval.simple.mon3.std.StandardEvalAccess.StandardDeviceEval;
import org.smartrplace.apps.hw.install.config.HardwareInstallConfig;
import org.smartrplace.apps.hw.install.config.InstallAppDevice;

public class OpenWeatherMapBigBlueRoom_DeviceHandler extends DeviceHandlerSimple<SensorDevice> {


	public OpenWeatherMapBigBlueRoom_DeviceHandler(ApplicationManagerPlus appMan) {
		super(appMan, false);
	}

	@Override
	public Class<SensorDevice> getResourceType() {
		return SensorDevice.class;
	}

	@Override
	public SingleValueResource getMainSensorValue(SensorDevice device, InstallAppDevice deviceConfiguration) {
		TemperatureResource ownTemp = device.sensors().getSubResource("temperature", TemperatureSensor.class).reading();
		if(!ownTemp.exists()) {
			TemperatureResource openWroom = KPIResourceAccess.getOpenWeatherMapTemperature(appMan.getResourceAccess());
			return openWroom;
		}
		return ownTemp;
	}

	@Override
	protected Collection<Datapoint> getDatapoints(SensorDevice device, InstallAppDevice deviceConfiguration) {
		List<Datapoint> result = new ArrayList<>();
		Datapoint dp = addDatapoint(getMainSensorValue(device, deviceConfiguration), result);
		if(dp != null && DeviceTableBase.makeDeviceToplevel(dp.getLocation()).startsWith("OpenWeatherMapData")) {
			Room openWroom = KPIResourceAccess.getOpenWeatherMapRoom(appMan.getResourceAccess());
			if(openWroom != null) {
				addDatapoint(openWroom.temperatureSensor().reading().forecast(), result);
				addDatapoint(openWroom.humiditySensor().reading(), result);
				addDatapoint(openWroom.humiditySensor().reading().forecast(), result);
				SolarIrradiationSensor solIrrSens = openWroom .getSubResource("solarIrradiationSensor", SolarIrradiationSensor.class);
				Datapoint realSolIrrDp = addDatapoint(solIrrSens.reading(), result);
				addDatapoint(openWroom .getSubResource("solarIrradiationSensor", SolarIrradiationSensor.class).reading().forecast(), result);
				addDatapoint(openWroom .getSubResource("windSensor", WindSensor.class).direction().reading(), result);
				addDatapoint(openWroom .getSubResource("windSensor", WindSensor.class).direction().reading().forecast(), result);
				addDatapoint(openWroom .getSubResource("windSensor", WindSensor.class).speed().reading(), result);
				addDatapoint(openWroom .getSubResource("windSensor", WindSensor.class).speed().reading().forecast(), result);
				addDatapoint(openWroom .getSubResource("cloudCoverage", GenericFloatSensor.class).reading(), result);
				addDatapoint(openWroom .getSubResource("cloudCoverage", GenericFloatSensor.class).reading().forecast(), result);
				
				if(Boolean.getBoolean("org.smartrplace.driverhandler.devices.weather_average")) {
					//Datapoint daily = null;
					if(realSolIrrDp != null) {
						Datapoint evalDp = StandardEvalAccess.getDatapointBaseEvalMetering(realSolIrrDp,
								StandardDeviceEval.AVERAGE_DAILY, dpService);
						result.add(evalDp);
						Datapoint evalDpMonthly = StandardEvalAccess.getDatapointBaseEvalMetering(realSolIrrDp,
								StandardDeviceEval.AVERAGE_MONTHLY, dpService);
						result.add(evalDpMonthly);
						Datapoint evalDpYearly = StandardEvalAccess.getDatapointBaseEvalMetering(realSolIrrDp,
								StandardDeviceEval.AVERAGE_YEARLY, dpService);
						result.add(evalDpYearly);
					}
					/*if(dp != null) {
						Datapoint evalDp = StandardEvalAccess.getDatapointBaseEvalMetering(dp,
								StandardDeviceEval.AVERAGE_DAILY, dpService);
						result.add(evalDp);
						Datapoint evalDpMonthly = StandardEvalAccess.getDatapointBaseEvalMetering(dp,
								StandardDeviceEval.AVERAGE_MONTHLY, dpService);
						result.add(evalDpMonthly);
						Datapoint evalDpYearly = StandardEvalAccess.getDatapointBaseEvalMetering(dp,
								StandardDeviceEval.AVERAGE_YEARLY, dpService);
						result.add(evalDpYearly);
						//evalDp.addToSubRoomLocationAtomic(null, null, ifac.getName()+"-hourly", false);

						//daily = provideIntervalFromMeterDatapoint("avIrradiationDaily", realSolIrrDp, result, solIrrSens, dpService,
						//		utilAggDaily, false);
					}*/
				}

				
				return result;
			}
		}
		addDatapoint(device.sensors().getSubResource("humidity", HumiditySensor.class).reading(), result);
		addDatapoint(device.sensors().getSubResource("solarIrradiation", SolarIrradiationSensor.class).reading(), result);
		addDatapoint(device.sensors().getSubResource("wind", WindSensor.class).direction().reading(), result);
		addDatapoint(device.sensors().getSubResource("wind", WindSensor.class).speed().reading(), result);
		return result;
	}

	@Override
	public String getTableTitle() {
		return "OpenWeatherMap Sensor Devices";
	}

	@Override
	protected Class<? extends ResourcePattern<SensorDevice>> getPatternClass() {
		return OpenWeatherMapBigBlueRoomPattern.class;
	}

	@Override
	public String getDeviceTypeShortId(DatapointService dpService) {
		return "WSRV";
	}
	
	@Override
	public void initAlarmingForDevice(InstallAppDevice appDevice, HardwareInstallConfig appConfigData) {
		appDevice.alarms().create();
		SensorDevice device = (SensorDevice) appDevice.device();
		TemperatureResource ownTemp = device.sensors().getSubResource("temperature", TemperatureSensor.class).reading();
		if(!ownTemp.exists()) {
			TemperatureResource ownTemp2 = KPIResourceAccess.getOpenWeatherMapTemperature(appMan.getResourceAccess());
			if(ownTemp2 != null)
				ownTemp = ownTemp2;
		}
	
		AlarmingUtiH.setTemplateValues(appDevice, ownTemp,
				-50, +80, 10, 6*60);
	}
	
	@Override
	public String getInitVersion() {
		return "A";
	}
	
	@Override
	public ComType getComType() {
		return ComType.IP;
	}
	
	@Override
	public boolean addDeviceOrResourceListToSync() {
		return false;
	}
}
