package org.smartrplace.driverhandler.devices;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.ogema.accessadmin.api.ApplicationManagerPlus;
import org.ogema.core.model.simple.SingleValueResource;
import org.ogema.core.resourcemanager.pattern.ResourcePattern;
import org.ogema.devicefinder.api.Datapoint;
import org.ogema.devicefinder.util.DeviceHandlerSimple;
import org.ogema.model.actors.MultiSwitch;
import org.ogema.model.devices.sensoractordevices.SensorDevice;
import org.ogema.model.sensors.GenericBinarySensor;
import org.ogema.model.sensors.GenericFloatSensor;
import org.ogema.model.sensors.HumiditySensor;
import org.ogema.model.sensors.SmokeDetector;
import org.ogema.model.sensors.TemperatureSensor;
import org.smartrplace.apps.hw.install.config.InstallAppDevice;


//@Component(specVersion = "1.2", immediate = true)
//@Service(DeviceHandlerProvider.class)
public class SmartProtect_DeviceHandler extends DeviceHandlerSimple<SensorDevice> {

	public SmartProtect_DeviceHandler(ApplicationManagerPlus appMan) {
		super(appMan, true);
	}

	@Override
	public Class<SensorDevice> getResourceType() {
		return SensorDevice.class;
	}

	@Override
	protected SingleValueResource getMainSensorValue(SensorDevice device, InstallAppDevice deviceConfiguration) {
		return device.getSubResource("state", MultiSwitch.class).stateFeedback();
	}

	@Override
	protected Collection<Datapoint> getDatapoints(SensorDevice device, InstallAppDevice deviceConfiguration) {
		List<Datapoint> result = new ArrayList<>();
		addDatapoint(getMainSensorValue(device, deviceConfiguration), result);
		addDatapoint(device.sensors().getSubResource("battery_low", GenericBinarySensor.class).reading(), result);
		addDatapoint(device.sensors().getSubResource("co_alert", GenericBinarySensor.class).reading(), result);
		addDatapoint(device.sensors().getSubResource("air", GenericFloatSensor.class).reading(), result);
		addDatapoint(device.sensors().getSubResource("humidity", HumiditySensor.class).reading(), result);
		addDatapoint(device.sensors().getSubResource("siren", GenericBinarySensor.class).reading(), result);
		addDatapoint(device.sensors().getSubResource("smoke", SmokeDetector.class).reading(), result);
		addDatapoint(device.sensors().getSubResource("motion", GenericBinarySensor.class).reading(), result);
		addDatapoint(device.getSubResource("state", MultiSwitch.class).stateControl(), result);
		addDatapoint(device.sensors().getSubResource("temperature", TemperatureSensor.class).reading(), result);
	return result;
	}

	@Override
	protected String getTableTitle() {
		return "Smart Protect Devices";
	}

	@Override
	protected Class<? extends ResourcePattern<SensorDevice>> getPatternClass() {
		return SmartProtect_SensorDevicePattern.class;
	}

}
