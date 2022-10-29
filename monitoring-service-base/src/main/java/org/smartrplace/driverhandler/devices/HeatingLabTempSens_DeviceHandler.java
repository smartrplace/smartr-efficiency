package org.smartrplace.driverhandler.devices;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.ogema.accessadmin.api.ApplicationManagerPlus;
import org.ogema.core.model.simple.SingleValueResource;
import org.ogema.core.resourcemanager.pattern.ResourcePattern;
import org.ogema.devicefinder.api.Datapoint;
import org.ogema.devicefinder.util.DeviceHandlerSimple;
import org.ogema.model.sensors.TemperatureSensor;
import org.smartrplace.apps.hw.install.config.InstallAppDevice;

public class HeatingLabTempSens_DeviceHandler extends DeviceHandlerSimple<TemperatureSensor> {

	public HeatingLabTempSens_DeviceHandler(ApplicationManagerPlus appMan) {
		super(appMan, true);
	}

	@Override
	public Class<TemperatureSensor> getResourceType() {
		return TemperatureSensor.class;
	}

	@Override
	public SingleValueResource getMainSensorValue(TemperatureSensor device, InstallAppDevice deviceConfiguration) {
		return device.reading();
	}

	@Override
	protected Collection<Datapoint> getDatapoints(TemperatureSensor device, InstallAppDevice deviceConfiguration) {
		List<Datapoint> result = new ArrayList<>();
		addDatapoint(getMainSensorValue(device, deviceConfiguration), result);
		return result;
	}

	@Override
	public String getTableTitle() {
		return "Room Temperature Sensors LAB/KNX";
	}

	@Override
	protected Class<? extends ResourcePattern<TemperatureSensor>> getPatternClass() {
		return HeatingLabTempsSensPattern.class;
	}

}
