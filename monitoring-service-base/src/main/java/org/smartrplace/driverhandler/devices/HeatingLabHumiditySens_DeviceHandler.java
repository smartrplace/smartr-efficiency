package org.smartrplace.driverhandler.devices;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.ogema.accessadmin.api.ApplicationManagerPlus;
import org.ogema.core.model.simple.SingleValueResource;
import org.ogema.core.resourcemanager.pattern.ResourcePattern;
import org.ogema.devicefinder.api.Datapoint;
import org.ogema.devicefinder.util.DeviceHandlerSimple;
import org.ogema.model.sensors.HumiditySensor;
import org.smartrplace.apps.hw.install.config.InstallAppDevice;

public class HeatingLabHumiditySens_DeviceHandler extends DeviceHandlerSimple<HumiditySensor> {

	public HeatingLabHumiditySens_DeviceHandler(ApplicationManagerPlus appMan) {
		super(appMan, true);
	}

	@Override
	public Class<HumiditySensor> getResourceType() {
		return HumiditySensor.class;
	}

	@Override
	public SingleValueResource getMainSensorValue(HumiditySensor device, InstallAppDevice deviceConfiguration) {
		return device.reading();
	}

	@Override
	protected Collection<Datapoint> getDatapoints(HumiditySensor device, InstallAppDevice deviceConfiguration) {
		List<Datapoint> result = new ArrayList<>();
		addDatapoint(getMainSensorValue(device, deviceConfiguration), result);
		return result;
	}

	@Override
	public String getTableTitle() {
		return "Room Humidity Sensors LAB/KNX";
	}

	@Override
	protected Class<? extends ResourcePattern<HumiditySensor>> getPatternClass() {
		return HeatingLabHumiditySensPattern.class;
	}

}
