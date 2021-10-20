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
import org.smartrplace.apps.hw.install.config.InstallAppDevice;

public class MultiSwitchHandler extends DeviceHandlerSimple<MultiSwitch> {

	public MultiSwitchHandler(ApplicationManagerPlus appMan) {
		super(appMan, true);
	}

	@Override
	public Class<MultiSwitch> getResourceType() {
		return MultiSwitch.class;
	}

	@Override
	public SingleValueResource getMainSensorValue(MultiSwitch device, InstallAppDevice deviceConfiguration) {
		return device.stateControl();
	}

	@Override
	public String getTableTitle() {
		return "KNX Multiswitch";
	}

	@Override
	protected Collection<Datapoint> getDatapoints(MultiSwitch device, InstallAppDevice deviceConfiguration) {
		List<Datapoint> result = new ArrayList<>();
		addDatapoint(getMainSensorValue(device, deviceConfiguration), result);
		return result;
	}

	@Override
	protected Class<? extends ResourcePattern<MultiSwitch>> getPatternClass() {
		return MultiSwitchPattern.class;
	}

}
