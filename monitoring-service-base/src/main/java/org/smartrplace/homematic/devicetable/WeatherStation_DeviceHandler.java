package org.smartrplace.homematic.devicetable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.ogema.accessadmin.api.ApplicationManagerPlus;
import org.ogema.core.model.simple.FloatResource;
import org.ogema.core.model.simple.SingleValueResource;
import org.ogema.core.resourcemanager.pattern.ResourcePattern;
import org.ogema.devicefinder.api.Datapoint;
import org.ogema.devicefinder.api.DatapointService;
import org.ogema.devicefinder.util.DeviceHandlerSimple;
import org.ogema.model.devices.sensoractordevices.SensorDevice;
import org.ogema.model.sensors.AngleSensor;
import org.ogema.model.sensors.GenericBinarySensor;
import org.ogema.model.sensors.GenericFloatSensor;
import org.ogema.model.sensors.HumiditySensor;
import org.ogema.model.sensors.LightSensor;
import org.ogema.model.sensors.TemperatureSensor;
import org.ogema.model.sensors.VelocitySensor;
import org.smartrplace.apps.hw.install.config.InstallAppDevice;


//@Component(specVersion = "1.2", immediate = true)
//@Service(DeviceHandlerProvider.class)
public class WeatherStation_DeviceHandler extends DeviceHandlerSimple<SensorDevice> {

	public WeatherStation_DeviceHandler(ApplicationManagerPlus appMan) {
		super(appMan, true);
	}

	@Override
	public Class<SensorDevice> getResourceType() {
		return SensorDevice.class;
	}

	@Override
	public SingleValueResource getMainSensorValue(SensorDevice device, InstallAppDevice deviceConfiguration) {
		return device.sensors().getSubResource("BRIGHTNESS", LightSensor.class).reading();
	}

	@Override
	protected Collection<Datapoint> getDatapoints(SensorDevice device, InstallAppDevice deviceConfiguration) {
		List<Datapoint> result = new ArrayList<>();
		addDatapoint(getMainSensorValue(device, deviceConfiguration), result);
		addDatapoint(device.sensors().getSubResource("BRIGHTNESS", LightSensor.class).getSubResource("rawValue", FloatResource.class), result);
		addDatapoint(device.sensors().getSubResource("HUMIDITY", HumiditySensor.class).reading(), result);
		addDatapoint(device.sensors().getSubResource("RAIN_COUNTER", GenericFloatSensor.class).reading(), result);
		addDatapoint(device.sensors().getSubResource("RAINING", GenericBinarySensor.class).reading(), result);
		addDatapoint(device.sensors().getSubResource("TEMPERATURE", TemperatureSensor.class).reading(), result);
		addDatapoint(device.sensors().getSubResource("WIND_DIRECTION", AngleSensor.class).reading(), result);
		addDatapoint(device.sensors().getSubResource("WIND_SPEED", VelocitySensor.class).reading(), result);

		addtStatusDatapointsHomematic(device, dpService, result);
		return result;
	}

	@Override
	public String getTableTitle() {
		return "Weather Stations";
	}

	@Override
	protected Class<? extends ResourcePattern<SensorDevice>> getPatternClass() {
		return WeatherStation_SensorDevicePattern.class;
	}

	@Override
	public String getDeviceTypeShortId(DatapointService dpService) {
		return "WST";
	}
}
