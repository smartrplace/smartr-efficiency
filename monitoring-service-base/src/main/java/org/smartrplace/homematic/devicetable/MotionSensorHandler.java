package org.smartrplace.homematic.devicetable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.ogema.accessadmin.api.ApplicationManagerPlus;
import org.ogema.core.model.simple.SingleValueResource;
import org.ogema.core.resourcemanager.pattern.ResourcePattern;
import org.ogema.devicefinder.api.Datapoint;
import org.ogema.devicefinder.util.DeviceHandlerSimple;
import org.ogema.model.sensors.OccupancySensor;
import org.smartrplace.apps.hw.install.config.InstallAppDevice;

public class MotionSensorHandler extends DeviceHandlerSimple<OccupancySensor> {

	public MotionSensorHandler(ApplicationManagerPlus appMan) {
		super(appMan, true);
	}

	@Override
	public Class<OccupancySensor> getResourceType() {
		return OccupancySensor.class;
	}

	@Override
	public SingleValueResource getMainSensorValue(OccupancySensor device, InstallAppDevice deviceConfiguration) {
		return device.reading();
	}

	@Override
	public String getTableTitle() {
		return "Motion Sensors";
	}

	@Override
	protected Collection<Datapoint> getDatapoints(OccupancySensor device, InstallAppDevice deviceConfiguration) {
		List<Datapoint> result = new ArrayList<>();
		addDatapoint(getMainSensorValue(deviceConfiguration), result);
		
		addtStatusDatapointsHomematic(device, dpService, result);
		return result;
	}

	@Override
	protected Class<? extends ResourcePattern<OccupancySensor>> getPatternClass() {
		return MotionSensorPattern.class;
	}

}
