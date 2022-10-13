package org.smartrplace.driverhandler.devices;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.ogema.accessadmin.api.ApplicationManagerPlus;
import org.ogema.core.model.simple.SingleValueResource;
import org.ogema.core.model.simple.TimeResource;
import org.ogema.core.resourcemanager.pattern.ResourcePattern;
import org.ogema.devicefinder.api.Datapoint;
import org.ogema.devicefinder.util.DeviceHandlerSimple;
import org.ogema.model.devices.sensoractordevices.SensorDevice;
import org.smartrplace.apps.hw.install.config.InstallAppDevice;

public class SmokeDetectorMBusHandler extends DeviceHandlerSimple<SensorDevice> {

	public SmokeDetectorMBusHandler(ApplicationManagerPlus appMan) {
		super(appMan, true);
	}

	@Override
	public Class<SensorDevice> getResourceType() {
		return SensorDevice.class;
	}

	@Override
	public SingleValueResource getMainSensorValue(SensorDevice device, InstallAppDevice deviceConfiguration) {
		return device.getSubResource("unsupported_02__fd_17_ERROR_FLAGS", TimeResource.class);
	}

	@Override
	public String getTableTitle() {
		return "Smoke Detectors (MBus)";
	}

	@Override
	protected Collection<Datapoint> getDatapoints(SensorDevice device, InstallAppDevice deviceConfiguration) {
		List<Datapoint> result = new ArrayList<>();
		addDatapoint(getMainSensorValue(device, deviceConfiguration), "errorFlags", result);
		addDatapoint(device.getSubResource("unsupported_02__fd_3c_NOT_SUPPORTED", TimeResource.class), "value450", result);
		return result;
	}

	@Override
	protected Class<? extends ResourcePattern<SensorDevice>> getPatternClass() {
		return SmokeDetectorMBusPattern.class;
	}

}
