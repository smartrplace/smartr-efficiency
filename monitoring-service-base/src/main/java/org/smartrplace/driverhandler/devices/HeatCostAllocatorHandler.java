package org.smartrplace.driverhandler.devices;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.ogema.accessadmin.api.ApplicationManagerPlus;
import org.ogema.core.model.ValueResource;
import org.ogema.core.model.simple.SingleValueResource;
import org.ogema.core.resourcemanager.pattern.ResourcePattern;
import org.ogema.devicefinder.api.Datapoint;
import org.ogema.devicefinder.util.DeviceHandlerSimple;
import org.ogema.model.devices.sensoractordevices.SensorDevice;
import org.ogema.model.sensors.EnergyAccumulatedSensor;
import org.ogema.model.sensors.Sensor;
import org.smartrplace.apps.hw.install.config.InstallAppDevice;

public class HeatCostAllocatorHandler extends DeviceHandlerSimple<SensorDevice> {

	public HeatCostAllocatorHandler(ApplicationManagerPlus appMan) {
		super(appMan, true);
	}

	@Override
	public Class<SensorDevice> getResourceType() {
		return SensorDevice.class;
	}

	@Override
	public SingleValueResource getMainSensorValue(SensorDevice device, InstallAppDevice deviceConfiguration) {
		return device.getSubResource("hcaEnergy", EnergyAccumulatedSensor.class).reading();
	}

	@Override
	public String getTableTitle() {
		return "Heat Cost Allocators";
	}

	@Override
	protected Collection<Datapoint> getDatapoints(SensorDevice device, InstallAppDevice deviceConfiguration) {
		List<Datapoint> result = new ArrayList<>();
		addDatapoint(getMainSensorValue(device, deviceConfiguration), result);
		//addDatapoint(device.getSubResource("hcaEnergy", EnergyAccumulatedSensor.class).reading(), result, dpService);
		return result;
	}

	@Override
	protected Class<? extends ResourcePattern<SensorDevice>> getPatternClass() {
		return HeatCostAllocatorPattern.class;
	}

}
