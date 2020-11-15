package org.smartrplace.homematic.devicetable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.ogema.accessadmin.api.ApplicationManagerPlus;
import org.ogema.core.model.simple.BooleanResource;
import org.ogema.core.model.simple.SingleValueResource;
import org.ogema.core.resourcemanager.pattern.ResourcePattern;
import org.ogema.devicefinder.api.Datapoint;
import org.ogema.devicefinder.util.DeviceHandlerSimple;
import org.ogema.model.sensors.SmokeDetector;
import org.smartrplace.apps.hw.install.config.InstallAppDevice;

public class SmokeDetector_DeviceHandler extends DeviceHandlerSimple<SmokeDetector> {

	public SmokeDetector_DeviceHandler(ApplicationManagerPlus appMan) {
		super(appMan, true);
	}

	@Override
	public Class<SmokeDetector> getResourceType() {
		return SmokeDetector.class;
	}

	@Override
	protected Collection<Datapoint> getDatapoints(SmokeDetector device, InstallAppDevice deviceConfiguration) {
		List<Datapoint> result = new ArrayList<>();
		addDatapoint(device.reading(), result);
		addDatapoint(device.getSubResource("error", BooleanResource.class), result);
		
		addtStatusDatapointsHomematic(device, dpService, result);
		return result;
	}

	@Override
	protected SingleValueResource getMainSensorValue(SmokeDetector device, InstallAppDevice deviceConfiguration) {
		return device.reading();
	}

	@Override
	protected String getTableTitle() {
		return "Smoke Detectors";
	}

	@Override
	protected Class<? extends ResourcePattern<SmokeDetector>> getPatternClass() {
		return SmokeDetectorPattern.class;
	}
}
