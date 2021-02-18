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
import org.ogema.model.metering.special.FlowProbe;
import org.ogema.model.sensors.Sensor;
import org.smartrplace.apps.hw.install.config.InstallAppDevice;

public class FlowScopeDevHandler extends DeviceHandlerSimple<FlowProbe> {

	public FlowScopeDevHandler(ApplicationManagerPlus appMan) {
		super(appMan, true);
	}

	@Override
	public Class<FlowProbe> getResourceType() {
		return FlowProbe.class;
	}

	@Override
	protected SingleValueResource getMainSensorValue(FlowProbe device, InstallAppDevice deviceConfiguration) {
		return device.flow().reading();
	}

	@Override
	protected Collection<Datapoint> getDatapoints(FlowProbe device, InstallAppDevice deviceConfiguration) {
		List<Datapoint> result = new ArrayList<>();
		//addDatapoint(getMainSensorValue(device, deviceConfiguration), result);
		List<Sensor> allSens = device.getSubResources(Sensor.class, false);
		for(Sensor ec: allSens) {
			ValueResource reading = ec.reading();
			if(!(reading instanceof SingleValueResource))
				continue;
			String ch = ec.getName();
			addDatapoint((SingleValueResource) reading, result, ch, dpService);
		}
		return result;
	}

	@Override
	protected String getTableTitle() {
		return "FlowProbe Devices";
	}

	@Override
	protected Class<? extends ResourcePattern<FlowProbe>> getPatternClass() {
		return FlowScopePattern.class;
	}

}
