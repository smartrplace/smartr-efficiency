package org.smartrplace.driverhandler.devices;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.ogema.accessadmin.api.ApplicationManagerPlus;
import org.ogema.core.model.simple.SingleValueResource;
import org.ogema.core.resourcemanager.pattern.ResourcePattern;
import org.ogema.devicefinder.api.Datapoint;
import org.ogema.devicefinder.util.DeviceHandlerSimple;
import org.ogema.model.devices.connectiondevices.ThermalValve;
import org.ogema.model.sensors.EnergyAccumulatedSensor;
import org.ogema.model.sensors.FlowSensor;
import org.ogema.model.sensors.TemperatureSensor;
import org.ogema.model.sensors.VolumeAccumulatedSensor;
import org.smartrplace.apps.hw.install.config.InstallAppDevice;

import de.fhg.iee.ogema.labdata.HeatingLabData;

public class HeatingLabThermalValve_DeviceHandler extends DeviceHandlerSimple<ThermalValve> {

	public HeatingLabThermalValve_DeviceHandler(ApplicationManagerPlus appMan) {
		super(appMan, true);
	}

	@Override
	public Class<ThermalValve> getResourceType() {
		return ThermalValve.class;
	}

	@Override
	protected SingleValueResource getMainSensorValue(ThermalValve device, InstallAppDevice deviceConfiguration) {
		return device.connection().inputTemperature().reading();
	}

	@Override
	protected Collection<Datapoint> getDatapoints(ThermalValve device, InstallAppDevice deviceConfiguration) {
		List<Datapoint> result = new ArrayList<>();
		addDatapoint(getMainSensorValue(device, deviceConfiguration), result);
		addDatapoint(device.connection().outputTemperature().reading(), result);
		return result;
	}

	@Override
	public String getTableTitle() {
		return "Heating Lab Radiators Extended Data";
	}

	@Override
	protected Class<? extends ResourcePattern<ThermalValve>> getPatternClass() {
		return HeatingLabThermalValvePattern.class;
	}

}
