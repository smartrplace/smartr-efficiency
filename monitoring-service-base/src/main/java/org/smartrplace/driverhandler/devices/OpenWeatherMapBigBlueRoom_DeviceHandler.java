package org.smartrplace.driverhandler.devices;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.ogema.accessadmin.api.ApplicationManagerPlus;
import org.ogema.core.model.simple.SingleValueResource;
import org.ogema.core.resourcemanager.pattern.ResourcePattern;
import org.ogema.devicefinder.api.Datapoint;
import org.ogema.devicefinder.util.DeviceHandlerSimple;
import org.ogema.model.devices.sensoractordevices.SensorDevice;
import org.ogema.model.devices.sensoractordevices.WindSensor;
import org.ogema.model.sensors.HumiditySensor;
import org.ogema.model.sensors.SolarIrradiationSensor;
import org.ogema.model.sensors.TemperatureSensor;
import org.smartrplace.apps.hw.install.config.InstallAppDevice;

public class OpenWeatherMapBigBlueRoom_DeviceHandler extends DeviceHandlerSimple<SensorDevice> {

	public OpenWeatherMapBigBlueRoom_DeviceHandler(ApplicationManagerPlus appMan) {
		super(appMan, true);
	}

	@Override
	public Class<SensorDevice> getResourceType() {
		return SensorDevice.class;
	}

	@Override
	protected SingleValueResource getMainSensorValue(SensorDevice device, InstallAppDevice deviceConfiguration) {
		return device.sensors().getSubResource("temperature", TemperatureSensor.class).reading();
	}

	@Override
	protected Collection<Datapoint> getDatapoints(SensorDevice device, InstallAppDevice deviceConfiguration) {
		List<Datapoint> result = new ArrayList<>();
		addDatapoint(getMainSensorValue(device, deviceConfiguration), result);
		addDatapoint(device.sensors().getSubResource("humidity", HumiditySensor.class).reading(), result);
		addDatapoint(device.sensors().getSubResource("solarIrradiation", SolarIrradiationSensor.class).reading(), result);
		addDatapoint(device.sensors().getSubResource("wind", WindSensor.class).direction().reading(), result);
		addDatapoint(device.sensors().getSubResource("wind", WindSensor.class).speed().reading(), result);
		return result;
	}

	@Override
	protected String getTableTitle() {
		return "OpenWeatherMap Sensor Devices";
	}

	@Override
	protected Class<? extends ResourcePattern<SensorDevice>> getPatternClass() {
		return OpenWeatherMapBigBlueRoomPattern.class;
	}

}
