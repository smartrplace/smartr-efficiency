package org.smartrplace.driverhandler.devices;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.ogema.accessadmin.api.ApplicationManagerPlus;
import org.ogema.core.model.simple.SingleValueResource;
import org.ogema.core.resourcemanager.pattern.ResourcePattern;
import org.ogema.devicefinder.api.Datapoint;
import org.ogema.devicefinder.util.DeviceHandlerSimple;
import org.ogema.model.sensors.FlowSensor;
import org.smartrplace.apps.hw.install.config.InstallAppDevice;

public class HeatingLabFlowSens_DeviceHandler extends DeviceHandlerSimple<FlowSensor> {

	public HeatingLabFlowSens_DeviceHandler(ApplicationManagerPlus appMan) {
		super(appMan, true);
	}

	@Override
	public Class<FlowSensor> getResourceType() {
		return FlowSensor.class;
	}

	@Override
	public SingleValueResource getMainSensorValue(FlowSensor device, InstallAppDevice deviceConfiguration) {
		return device.reading();
	}

	@Override
	protected Collection<Datapoint> getDatapoints(FlowSensor device, InstallAppDevice deviceConfiguration) {
		List<Datapoint> result = new ArrayList<>();
		addDatapoint(getMainSensorValue(device, deviceConfiguration), result);
		return result;
	}

	@Override
	public String getTableTitle() {
		return "Flow Sensors LAB";
	}

	@Override
	protected Class<? extends ResourcePattern<FlowSensor>> getPatternClass() {
		return HeatingLabTempsFlowSensorPattern.class;
	}

}
